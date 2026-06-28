package com.linkyun.her;

import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

final class GatewayTtsPlayer {
    interface Listener {
        void onStarted(String id, String text);
        void onCompleted(String id);
        void onError(String id, String message);
    }

    static final class PlaybackOptions {
        final boolean voiceCommunication;
        final AudioDeviceInfo preferredOutput;
        final float volumeGain;

        private PlaybackOptions(boolean voiceCommunication, AudioDeviceInfo preferredOutput,
                float volumeGain) {
            this.voiceCommunication = voiceCommunication;
            this.preferredOutput = preferredOutput;
            this.volumeGain = clampGain(volumeGain);
        }

        static PlaybackOptions media(float volumeGain) {
            return new PlaybackOptions(false, null, volumeGain);
        }

        static PlaybackOptions voiceCommunication(AudioDeviceInfo preferredOutput, float volumeGain) {
            return new PlaybackOptions(true, preferredOutput, volumeGain);
        }
    }

    private static final class WsPlayback {
        final int run;
        final String id;
        final String text;
        final PlaybackOptions playback;
        final Listener listener;
        final int voiceIndex;
        final long startedAtMs;
        final PcmSampleBuffer sampleBuffer = new PcmSampleBuffer();

        Runnable timeout;
        Runnable idleCompletion;
        AudioTrack track;
        boolean taskSent;
        boolean audioStarted;
        long firstAudioAtMs;
        long totalBytes;

        WsPlayback(int run, String id, String text, PlaybackOptions playback,
                Listener listener, int voiceIndex, long startedAtMs) {
            this.run = run;
            this.id = id;
            this.text = text;
            this.playback = playback;
            this.listener = listener;
            this.voiceIndex = voiceIndex;
            this.startedAtMs = startedAtMs;
        }
    }

    private static final class WsFrame {
        final int event;
        final byte[] audioBytes;
        final String payloadText;
        final String error;

        WsFrame(int event, byte[] audioBytes, String payloadText, String error) {
            this.event = event;
            this.audioBytes = audioBytes;
            this.payloadText = payloadText;
            this.error = error;
        }
    }

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String OPUS_FORMAT = "opus";
    private static final String PCM_FORMAT = "pcm";
    private static final String MP3_FORMAT = "mp3";
    private static final int STREAM_SAMPLE_RATE = 24000;
    private static final int STREAM_CHUNK_BYTES = 4096;
    private static final int WS_EVENT_START_CONNECTION = 1;
    private static final int WS_EVENT_CONNECTION_STARTED = 50;
    private static final int WS_EVENT_START_SESSION = 100;
    private static final int WS_EVENT_SESSION_STARTED = 150;
    private static final int WS_EVENT_TASK_REQUEST = 200;
    private static final int WS_EVENT_SENTENCE_START = 350;
    private static final int WS_EVENT_SENTENCE_END = 351;
    private static final int WS_READY_TIMEOUT_MS = 9_000;
    private static final int WS_FIRST_AUDIO_TIMEOUT_MS = 2_500;
    private static final int WS_AUDIO_IDLE_COMPLETE_MS = 1_200;

    private final String tag;
    private final OkHttpClient http;
    private final Handler main;
    private final File cacheDir;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private volatile String[] voices;

    private final GatewayTtsCallbackGate callbackGate = new GatewayTtsCallbackGate();

    private volatile Call call;
    private volatile MediaPlayer player;
    private volatile AudioTrack streamTrack;
    private volatile boolean playing = false;
    private volatile int preferredVoiceIndex = 0;

    private final Object wsLock = new Object();
    private WebSocket ttsWebSocket;
    private boolean wsConnecting;
    private boolean wsReady;
    private int wsVoiceIndex = -1;
    private String wsSessionId;
    private long wsSessionStartedAtMs;
    private WsPlayback currentWsPlayback;

    GatewayTtsPlayer(String tag, OkHttpClient http, Handler main, File cacheDir,
            String baseUrl, String apiKey, String model, String voice) {
        this(tag, http, main, cacheDir, baseUrl, apiKey, model, new String[] { voice });
    }

    GatewayTtsPlayer(String tag, OkHttpClient http, Handler main, File cacheDir,
            String baseUrl, String apiKey, String model, String[] voices) {
        this.tag = tag;
        this.http = http;
        this.main = main;
        this.cacheDir = cacheDir;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.voices = sanitizeVoices(voices);
    }

    boolean isPlaying() {
        return playing;
    }

    void setVoicePreference(String primaryVoice, String[] fallbackVoices) {
        String[] updated = sanitizeVoices(mergeVoices(primaryVoice, fallbackVoices));
        String oldVoice = voiceForIndex(0);
        voices = updated;
        preferredVoiceIndex = 0;
        if (!oldVoice.equals(voiceForIndex(0))) {
            synchronized (wsLock) {
                closeWebSocketLocked("voice_changed");
            }
            warmup();
        }
    }

    void warmup() {
        if (apiKey == null || apiKey.isEmpty()) return;
        int voiceIndex = Math.max(0, Math.min(preferredVoiceIndex, voices.length - 1));
        synchronized (wsLock) {
            ensureWebSocketLocked(voiceIndex, System.currentTimeMillis(), true);
        }
    }

    void play(String id, String text, Listener listener) {
        play(id, text, PlaybackOptions.media(1.0f), listener);
    }

    void play(String id, String text, float volumeGain, Listener listener) {
        play(id, text, PlaybackOptions.media(volumeGain), listener);
    }

    void play(String id, String text, PlaybackOptions options, Listener listener) {
        stop();
        String rawText = text == null ? "" : text.trim();
        if (rawText.isEmpty()) return;
        String speechText = sanitizeForSpeech(rawText);
        int run = callbackGate.nextRun();
        long startedAtMs = System.currentTimeMillis();
        PlaybackOptions playback = options == null ? PlaybackOptions.media(1.0f) : options;
        int startVoiceIndex = Math.max(0, Math.min(preferredVoiceIndex, voices.length - 1));
        Log.i(tag, "gateway tts request id=" + id
                + " len=" + rawText.length()
                + " speechLen=" + speechText.length()
                + " startVoice=" + voiceForIndex(startVoiceIndex)
                + " voiceCommunication=" + playback.voiceCommunication
                + " preferredOutput=" + describeDevice(playback.preferredOutput)
                + " gain=" + playback.volumeGain);
        if (speechText.isEmpty()) {
            Log.i(tag, "gateway tts skipped empty speech text id=" + id);
            postCompleted(run, listener, id);
            return;
        }
        if (apiKey == null || apiKey.isEmpty()) {
            postError(run, listener, id, "Missing AGENTLLM_API_KEY");
            return;
        }
        if (tryPlayWebSocket(run, id, speechText, playback, listener, startVoiceIndex, startedAtMs)) {
            return;
        }
        requestSpeech(run, id, speechText, PCM_FORMAT, playback, listener, startVoiceIndex, startedAtMs);
    }

    static String sanitizeForSpeech(String text) {
        if (text == null) return "";
        String clean = text.replace('\u00A0', ' ');
        clean = clean.replaceAll("(?s)```.*?```", " ");
        clean = clean.replaceAll("(?s)`[^`]*`", " ");
        clean = clean.replaceAll("(?s)\\*\\*.*?\\*\\*", " ");
        clean = clean.replaceAll("(?s)__.*?__", " ");
        clean = clean.replaceAll("(?s)~~.*?~~", " ");
        clean = clean.replaceAll("(?s)\\*[^*\\n]+\\*", " ");
        clean = clean.replaceAll("(?s)_[^_\\n]+_", " ");
        clean = removeBracketedContent(clean);
        clean = clean.replaceAll("https?://\\S+", " ");
        clean = clean.replaceAll("(?m)^\\s{0,3}(#{1,6}|>|[-*+]|\\d+[.)])\\s+", " ");
        clean = clean.replaceAll("(?m)^\\s*[-=]{3,}\\s*$", " ");
        clean = clean.replaceAll("[*_~`#>]+", " ");
        clean = clean.replaceAll("[\\r\\n\\t]+", " ");
        clean = clean.replaceAll("\\s+", " ");
        clean = clean.replaceAll("\\s+([，。！？、；：,.!?;:])", "$1");
        clean = clean.replaceAll("([，。！？、；：])\\s+", "$1");
        return clean.trim();
    }

    private static String removeBracketedContent(String text) {
        StringBuilder out = new StringBuilder(text.length());
        char[] stack = new char[text.length()];
        int depth = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (isOpeningBracket(ch)) {
                if (depth < stack.length) stack[depth] = matchingClosingBracket(ch);
                depth++;
                if (depth == 1) out.append(' ');
                continue;
            }
            if (depth > 0) {
                if (depth <= stack.length && ch == stack[depth - 1]) depth--;
                continue;
            }
            if (isClosingBracket(ch)) {
                out.append(' ');
                continue;
            }
            out.append(ch);
        }
        return out.toString();
    }

    private static boolean isOpeningBracket(char ch) {
        return ch == '(' || ch == '（' || ch == '[' || ch == '【'
                || ch == '{' || ch == '｛' || ch == '〔' || ch == '「'
                || ch == '『';
    }

    private static boolean isClosingBracket(char ch) {
        return ch == ')' || ch == '）' || ch == ']' || ch == '】'
                || ch == '}' || ch == '｝' || ch == '〕' || ch == '」'
                || ch == '』';
    }

    private static char matchingClosingBracket(char ch) {
        switch (ch) {
            case '(':
                return ')';
            case '（':
                return '）';
            case '[':
                return ']';
            case '【':
                return '】';
            case '{':
                return '}';
            case '｛':
                return '｝';
            case '〔':
                return '〕';
            case '「':
                return '」';
            case '『':
                return '』';
            default:
                return ch;
        }
    }

    private boolean tryPlayWebSocket(int run, String id, String text, PlaybackOptions playback,
            Listener listener, int voiceIndex, long startedAtMs) {
        WsPlayback request = new WsPlayback(run, id, text, playback, listener, voiceIndex, startedAtMs);
        synchronized (wsLock) {
            if (isWebSocketReadyLocked(voiceIndex)) {
                currentWsPlayback = request;
                scheduleWsTimeoutLocked(request);
                return sendWsTaskLocked(request);
            }
            Log.i(tag, "gateway tts ws not ready, use http stream id=" + id
                    + " voice=" + voiceForIndex(voiceIndex)
                    + " elapsedMs=" + elapsedSince(startedAtMs));
            return false;
        }
    }

    private boolean isWebSocketReadyLocked(int voiceIndex) {
        return ttsWebSocket != null && wsReady && wsVoiceIndex == voiceIndex;
    }

    private void ensureWebSocketLocked(int voiceIndex, long startedAtMs, boolean warmupOnly) {
        if (ttsWebSocket != null && wsVoiceIndex == voiceIndex && (wsReady || wsConnecting)) return;
        closeWebSocketLocked("replace");
        String voice = voiceForIndex(voiceIndex);
        String resource = resourceForVoice(voice);
        String url = webSocketUrl(resource);
        if (url == null) {
            Log.d(tag, "gateway tts ws skipped missing base url");
            return;
        }
        wsVoiceIndex = voiceIndex;
        wsReady = false;
        wsConnecting = true;
        wsSessionStartedAtMs = startedAtMs;
        wsSessionId = "tts-" + UUID.randomUUID();
        Request request = new Request.Builder().url(url).build();
        Log.i(tag, "gateway tts ws connect voice=" + voice
                + " resource=" + resource
                + " warmup=" + warmupOnly);
        ttsWebSocket = http.newWebSocket(request, new WebSocketListener() {
            @Override public void onOpen(WebSocket webSocket, Response response) {
                synchronized (wsLock) {
                    if (ttsWebSocket != webSocket) return;
                    sendWsFrameLocked(WS_EVENT_START_CONNECTION, null, new JSONObject());
                }
            }

            @Override public void onMessage(WebSocket webSocket, ByteString bytes) {
                handleWsMessage(webSocket, bytes.toByteArray());
            }

            @Override public void onFailure(WebSocket webSocket, Throwable error, Response response) {
                handleWsFailure(webSocket, error == null ? "unknown" : error.getMessage());
            }

            @Override public void onClosing(WebSocket webSocket, int code, String reason) {
                webSocket.close(code, reason);
            }

            @Override public void onClosed(WebSocket webSocket, int code, String reason) {
                handleWsClosed(webSocket, code, reason);
            }
        });
    }

    private String webSocketUrl(String resource) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) return null;
        String root = baseUrl.trim();
        while (root.endsWith("/")) root = root.substring(0, root.length() - 1);
        if (root.startsWith("https://")) {
            root = "wss://" + root.substring("https://".length());
        } else if (root.startsWith("http://")) {
            root = "ws://" + root.substring("http://".length());
        }
        return root + "/v1beta/volc/tts/ws?api_key=" + encode(apiKey)
                + "&resource=" + encode(resource);
    }

    private static String resourceForVoice(String voice) {
        if (voice == null) return "seed2";
        if (voice.startsWith("S_") || voice.startsWith("ICL_")) return "v1";
        if (voice.contains("_mars_") || voice.contains("_moon_")) return "bigtts";
        return "seed2";
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8");
        } catch (Exception ignored) {
            return "";
        }
    }

    private void handleWsMessage(WebSocket webSocket, byte[] data) {
        WsFrame frame;
        try {
            frame = parseWsFrame(data);
        } catch (Exception error) {
            handleWsFailure(webSocket, "parse " + error.getMessage());
            return;
        }
        WsPlayback completePlayback = null;
        boolean completeAfterAudio = false;
        synchronized (wsLock) {
            if (ttsWebSocket != webSocket) return;
            if (frame.error != null) {
                Log.i(tag, "gateway tts ws server error " + frame.error);
                fallbackCurrentWsLocked("server " + frame.error);
                return;
            }
            if (frame.event == WS_EVENT_CONNECTION_STARTED) {
                Log.i(tag, "gateway tts ws connection started elapsedMs="
                        + elapsedSince(wsSessionStartedAtMs));
                sendWsStartSessionLocked();
                return;
            }
            if (frame.event == WS_EVENT_SESSION_STARTED) {
                wsConnecting = false;
                wsReady = true;
                Log.i(tag, "gateway tts ws session ready voice=" + voiceForIndex(wsVoiceIndex)
                        + " elapsedMs=" + elapsedSince(wsSessionStartedAtMs));
                if (currentWsPlayback != null) sendWsTaskLocked(currentWsPlayback);
                return;
            }
            if (frame.event == WS_EVENT_SENTENCE_START) {
                WsPlayback active = currentWsPlayback;
                if (active != null) {
                    Log.d(tag, "gateway tts ws sentence start id=" + active.id
                            + " elapsedMs=" + elapsedSince(active.startedAtMs));
                }
                return;
            }
            if (frame.event == WS_EVENT_SENTENCE_END) {
                completePlayback = currentWsPlayback;
                if (completePlayback != null && completePlayback.audioStarted) {
                    cancelWsIdleLocked(completePlayback);
                    currentWsPlayback = null;
                    completeAfterAudio = true;
                }
            } else if (frame.audioBytes != null && frame.audioBytes.length > 0) {
                handleWsAudioLocked(frame.audioBytes);
                return;
            } else if (frame.payloadText != null && !frame.payloadText.isEmpty()) {
                Log.d(tag, "gateway tts ws event=" + frame.event + " payload=" + frame.payloadText);
                return;
            }
        }
        if (completePlayback != null) {
            if (completeAfterAudio) {
                completeWsPlayback(completePlayback);
            } else {
                fallbackWsPlayback(completePlayback, "empty ws audio");
            }
        }
    }

    private void sendWsStartSessionLocked() {
        JSONObject payload = wsPayload(WS_EVENT_START_SESSION, voiceForIndex(wsVoiceIndex), null);
        sendWsFrameLocked(WS_EVENT_START_SESSION, wsSessionId, payload);
    }

    private boolean sendWsTaskLocked(WsPlayback playback) {
        if (ttsWebSocket == null || !wsReady || wsVoiceIndex != playback.voiceIndex) return false;
        if (playback.taskSent) return true;
        playback.taskSent = true;
        JSONObject payload = wsPayload(WS_EVENT_TASK_REQUEST, voiceForIndex(playback.voiceIndex), playback.text);
        boolean sent = sendWsFrameLocked(WS_EVENT_TASK_REQUEST, wsSessionId, payload);
        if (sent) {
            scheduleWsFirstAudioTimeoutLocked(playback);
            Log.i(tag, "gateway tts ws task sent id=" + playback.id
                    + " voice=" + voiceForIndex(playback.voiceIndex)
                    + " elapsedMs=" + elapsedSince(playback.startedAtMs));
        } else {
            fallbackCurrentWsLocked("send failed");
        }
        return sent;
    }

    private JSONObject wsPayload(int event, String voice, String text) {
        JSONObject payload = new JSONObject();
        JSONObject req = new JSONObject();
        JSONObject audio = new JSONObject();
        try {
            audio.put("format", PCM_FORMAT);
            audio.put("sample_rate", STREAM_SAMPLE_RATE);
            req.put("speaker", voice);
            req.put("audio_params", audio);
            if (text != null) req.put("text", text);
            payload.put("namespace", "BidirectionalTTS");
            payload.put("event", event);
            payload.put("req_params", req);
        } catch (Exception ignored) {
        }
        return payload;
    }

    private void handleWsAudioLocked(byte[] audioBytes) {
        WsPlayback playback = currentWsPlayback;
        if (playback == null || !isCurrent(playback.run)) return;
        try {
            if (playback.track == null) {
                playback.track = createStreamTrack(playback.playback);
                playback.track.setVolume(playback.playback.volumeGain);
                streamTrack = playback.track;
                playing = true;
            }
            byte[] aligned = new byte[audioBytes.length + 1];
            int alignedBytes = playback.sampleBuffer.copyAligned(audioBytes, audioBytes.length, aligned);
            if (alignedBytes <= 0) return;
            if (!playback.audioStarted) {
                playback.audioStarted = true;
                playback.firstAudioAtMs = System.currentTimeMillis();
                cancelWsTimeoutLocked(playback);
                Log.i(tag, "gateway tts ws stream started id=" + playback.id
                        + " elapsedMs=" + elapsedSince(playback.startedAtMs));
                postStarted(playback.run, playback.listener, playback.id, playback.text);
            }
            int written = playback.track.write(aligned, 0, alignedBytes);
            if (written > 0) playback.totalBytes += written;
            scheduleWsIdleCompleteLocked(playback);
        } catch (RuntimeException error) {
            fallbackCurrentWsLocked("audio " + error.getMessage());
        }
    }

    private void completeWsPlayback(WsPlayback playback) {
        new Thread(() -> {
            if (playback.sampleBuffer.hasPendingByte()) {
                Log.d(tag, "gateway tts ws discarded dangling pcm byte id=" + playback.id);
            }
            waitForStreamDrain(playback.run, playback.firstAudioAtMs, playback.totalBytes);
            stopStreamTrackIfOwned(playback.track);
            if (!isCurrent(playback.run)) return;
            Log.i(tag, "gateway tts ws stream completed id=" + playback.id
                    + " elapsedMs=" + elapsedSince(playback.startedAtMs)
                    + " bytes=" + playback.totalBytes);
            postCompleted(playback.run, playback.listener, playback.id);
        }, "gateway-tts-ws-drain").start();
    }

    private void fallbackCurrentWsLocked(String reason) {
        WsPlayback playback = currentWsPlayback;
        if (playback == null) return;
        currentWsPlayback = null;
        cancelWsTimeoutLocked(playback);
        cancelWsIdleLocked(playback);
        if (playback.audioStarted) {
            Log.i(tag, "gateway tts ws failed after audio id=" + playback.id + " reason=" + reason);
            postError(playback.run, playback.listener, playback.id, reason);
            return;
        }
        Log.i(tag, "gateway tts ws fallback id=" + playback.id
                + " reason=" + reason
                + " elapsedMs=" + elapsedSince(playback.startedAtMs));
        main.post(() -> fallbackWsPlayback(playback, reason));
    }

    private void fallbackWsPlayback(WsPlayback playback, String reason) {
        if (!isCurrent(playback.run)) return;
        stopStreamTrackIfOwned(playback.track);
        requestSpeech(playback.run, playback.id, playback.text, PCM_FORMAT, playback.playback,
                playback.listener, playback.voiceIndex, playback.startedAtMs);
        warmup();
    }

    private void handleWsFailure(WebSocket webSocket, String message) {
        synchronized (wsLock) {
            if (ttsWebSocket != webSocket) return;
            Log.i(tag, "gateway tts ws failure message=" + message);
            ttsWebSocket = null;
            wsConnecting = false;
            wsReady = false;
            fallbackCurrentWsLocked(message);
        }
    }

    private void handleWsClosed(WebSocket webSocket, int code, String reason) {
        synchronized (wsLock) {
            if (ttsWebSocket != webSocket) return;
            Log.i(tag, "gateway tts ws closed code=" + code + " reason=" + reason);
            ttsWebSocket = null;
            wsConnecting = false;
            wsReady = false;
            fallbackCurrentWsLocked("closed " + code);
        }
    }

    private void closeWebSocketLocked(String reason) {
        WebSocket webSocket = ttsWebSocket;
        ttsWebSocket = null;
        wsConnecting = false;
        wsReady = false;
        if (webSocket != null) {
            try {
                webSocket.close(1000, reason);
            } catch (Exception ignored) {
            }
        }
    }

    private void scheduleWsTimeoutLocked(WsPlayback playback) {
        Runnable timeout = () -> {
            synchronized (wsLock) {
                if (currentWsPlayback != playback || playback.audioStarted) return;
                Log.i(tag, "gateway tts ws timeout id=" + playback.id
                        + " phase=" + (playback.taskSent ? "first_audio" : "ready")
                        + " elapsedMs=" + elapsedSince(playback.startedAtMs));
                closeWebSocketLocked("timeout");
                fallbackCurrentWsLocked(playback.taskSent ? "first_audio_timeout" : "ready_timeout");
            }
        };
        playback.timeout = timeout;
        main.postDelayed(timeout, WS_READY_TIMEOUT_MS);
    }

    private void scheduleWsFirstAudioTimeoutLocked(WsPlayback playback) {
        cancelWsTimeoutLocked(playback);
        Runnable timeout = () -> {
            synchronized (wsLock) {
                if (currentWsPlayback != playback || playback.audioStarted) return;
                Log.i(tag, "gateway tts ws first audio timeout id=" + playback.id
                        + " elapsedMs=" + elapsedSince(playback.startedAtMs));
                closeWebSocketLocked("first_audio_timeout");
                fallbackCurrentWsLocked("first_audio_timeout");
            }
        };
        playback.timeout = timeout;
        main.postDelayed(timeout, WS_FIRST_AUDIO_TIMEOUT_MS);
    }

    private void cancelWsTimeoutLocked(WsPlayback playback) {
        if (playback == null || playback.timeout == null) return;
        main.removeCallbacks(playback.timeout);
        playback.timeout = null;
    }

    private void scheduleWsIdleCompleteLocked(WsPlayback playback) {
        cancelWsIdleLocked(playback);
        Runnable idle = () -> handleWsIdleComplete(playback);
        playback.idleCompletion = idle;
        main.postDelayed(idle, WS_AUDIO_IDLE_COMPLETE_MS);
    }

    private void handleWsIdleComplete(WsPlayback playback) {
        boolean shouldComplete = false;
        synchronized (wsLock) {
            if (currentWsPlayback == playback && playback.audioStarted) {
                currentWsPlayback = null;
                playback.idleCompletion = null;
                shouldComplete = true;
            }
        }
        if (shouldComplete) {
            Log.d(tag, "gateway tts ws idle complete id=" + playback.id);
            completeWsPlayback(playback);
        }
    }

    private void cancelWsIdleLocked(WsPlayback playback) {
        if (playback == null || playback.idleCompletion == null) return;
        main.removeCallbacks(playback.idleCompletion);
        playback.idleCompletion = null;
    }

    private boolean sendWsFrameLocked(int event, String sid, JSONObject payload) {
        if (ttsWebSocket == null) return false;
        try {
            return ttsWebSocket.send(ByteString.of(buildWsFrame(event, sid, payload)));
        } catch (Exception error) {
            Log.d(tag, "gateway tts ws send failed event=" + event + " message=" + error.getMessage());
            return false;
        }
    }

    private byte[] buildWsFrame(int event, String sid, JSONObject payload) throws IOException {
        byte[] sidBytes = sid == null ? null : sid.getBytes(StandardCharsets.UTF_8);
        byte[] payloadBytes = gzip(payload == null ? "{}" : payload.toString());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0x11);
        out.write(0x14);
        out.write(0x11);
        out.write(0);
        writeInt(out, event);
        if (sidBytes != null) {
            writeInt(out, sidBytes.length);
            out.write(sidBytes);
        }
        writeInt(out, payloadBytes.length);
        out.write(payloadBytes);
        return out.toByteArray();
    }

    private WsFrame parseWsFrame(byte[] data) throws IOException {
        if (data == null || data.length < 4) return new WsFrame(0, null, null, "empty frame");
        int headerSize = (data[0] & 0x0F) * 4;
        int messageType = (data[1] & 0xF0) >> 4;
        int flags = data[1] & 0x0F;
        int serialization = (data[2] & 0xF0) >> 4;
        int compression = data[2] & 0x0F;
        int offset = Math.max(4, headerSize);
        if (messageType == 15) {
            byte[] payload = readPayload(data, Math.min(8, data.length), compression);
            return new WsFrame(0, null, toText(payload), toText(payload));
        }
        int event = 0;
        if ((flags & 0x02) != 0) offset += 4;
        if ((flags & 0x04) != 0 && offset + 4 <= data.length) {
            event = readInt(data, offset);
            offset += 4;
        }
        if (offset + 4 > data.length) return new WsFrame(event, null, null, null);
        int firstLength = readInt(data, offset);
        offset += 4;
        if (firstLength >= 0 && offset + firstLength + 4 <= data.length) {
            offset += firstLength;
            int payloadLength = readInt(data, offset);
            offset += 4;
            if (payloadLength >= 0 && offset + payloadLength <= data.length) {
                byte[] payload = inflateIfNeeded(data, offset, payloadLength, compression);
                if (serialization == 1) return new WsFrame(event, null, toText(payload), null);
                return new WsFrame(event, payload, null, null);
            }
        }
        int payloadLength = firstLength;
        if (payloadLength >= 0 && offset + payloadLength <= data.length) {
            byte[] payload = inflateIfNeeded(data, offset, payloadLength, compression);
            if (serialization == 1) return new WsFrame(event, null, toText(payload), null);
            return new WsFrame(event, payload, null, null);
        }
        return new WsFrame(event, null, null, "malformed frame");
    }

    private static byte[] readPayload(byte[] data, int offset, int compression) throws IOException {
        if (offset >= data.length) return new byte[0];
        return inflateIfNeeded(data, offset, data.length - offset, compression);
    }

    private static byte[] gzip(String text) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
            gzip.write(text.getBytes(StandardCharsets.UTF_8));
        }
        return bytes.toByteArray();
    }

    private static byte[] inflateIfNeeded(byte[] data, int offset, int length, int compression)
            throws IOException {
        byte[] payload = new byte[length];
        System.arraycopy(data, offset, payload, 0, length);
        if (compression != 1) return payload;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(payload))) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = gzip.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
        return out.toByteArray();
    }

    private static String toText(byte[] payload) {
        if (payload == null || payload.length == 0) return "";
        return new String(payload, StandardCharsets.UTF_8);
    }

    private static int readInt(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 24)
                | ((data[offset + 1] & 0xFF) << 16)
                | ((data[offset + 2] & 0xFF) << 8)
                | (data[offset + 3] & 0xFF);
    }

    private static void writeInt(ByteArrayOutputStream out, int value) {
        out.write((value >>> 24) & 0xFF);
        out.write((value >>> 16) & 0xFF);
        out.write((value >>> 8) & 0xFF);
        out.write(value & 0xFF);
    }

    private void requestSpeech(int run, String id, String text, String responseFormat,
            PlaybackOptions playback, Listener listener, int voiceIndex, long startedAtMs) {
        if (!isCurrent(run)) return;
        String voice = voiceForIndex(voiceIndex);
        String resource = resourceForVoice(voice);
        JSONObject body = new JSONObject();
        try {
            body.put("model", model);
            body.put("input", text);
            body.put("voice", voice);
            body.put("resource", resource);
            body.put("response_format", responseFormat);
            body.put("speed", 1.0);
        } catch (Exception error) {
            postError(run, listener, id, error.getMessage());
            return;
        }
        Request request = new Request.Builder()
                .url(baseUrl + "/v1/audio/speech")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();
        Call requestCall = http.newCall(request);
        call = requestCall;
        requestCall.enqueue(new Callback() {
            @Override public void onFailure(Call requestCall, IOException error) {
                if (!isCurrent(run)) return;
                Log.i(tag, "gateway tts request failed id=" + id
                        + " elapsedMs=" + elapsedSince(startedAtMs)
                        + " message=" + error.getMessage());
                clearCallIfOwned(requestCall);
                postError(run, listener, id, error.getMessage());
            }

            @Override public void onResponse(Call requestCall, Response response) throws IOException {
                if (!isCurrent(run)) {
                    response.close();
                    return;
                }
                if (!response.isSuccessful()) {
                    String responseText = response.body() == null ? "" : response.body().string();
                    Log.i(tag, "gateway tts http failed id=" + id + " voice=" + voice
                            + " format=" + responseFormat + " code=" + response.code()
                            + " elapsedMs=" + elapsedSince(startedAtMs)
                            + " body=" + abbreviate(responseText, 180));
                    if (shouldFallbackVoice(response.code()) && voiceIndex + 1 < voices.length) {
                        clearCallIfOwned(requestCall);
                        preferredVoiceIndex = voiceIndex + 1;
                        Log.i(tag, "gateway tts voice fallback id=" + id
                                + " from=" + voice + " to=" + voiceForIndex(voiceIndex + 1));
                        requestSpeech(run, id, text, PCM_FORMAT, playback, listener, voiceIndex + 1, startedAtMs);
                        return;
                    }
                    String nextFormat = nextFallbackFormat(responseFormat);
                    if (nextFormat != null) {
                        clearCallIfOwned(requestCall);
                        requestSpeech(run, id, text, nextFormat, playback, listener, voiceIndex, startedAtMs);
                    } else {
                        clearCallIfOwned(requestCall);
                        postError(run, listener, id, "HTTP " + response.code() + " " + responseText);
                    }
                    return;
                }
                if (PCM_FORMAT.equals(responseFormat)) {
                    streamPcmResponse(run, id, text, playback, requestCall, response, listener, startedAtMs);
                } else {
                    playFileResponse(run, id, text, responseFormat, playback, requestCall, response, listener, voiceIndex, startedAtMs);
                }
            }
        });
    }

    private String nextFallbackFormat(String responseFormat) {
        if (PCM_FORMAT.equals(responseFormat)) return OPUS_FORMAT;
        if (OPUS_FORMAT.equals(responseFormat)) return MP3_FORMAT;
        return null;
    }

    private static boolean shouldFallbackVoice(int httpCode) {
        return httpCode == 400 || httpCode == 422;
    }

    private static String abbreviate(String text, int maxChars) {
        if (text == null) return "";
        String clean = text.replace('\n', ' ').replace('\r', ' ').trim();
        if (clean.length() <= maxChars) return clean;
        return clean.substring(0, maxChars) + "...";
    }

    private static String[] sanitizeVoices(String[] rawVoices) {
        if (rawVoices == null || rawVoices.length == 0) return new String[] { "" };
        String[] cleaned = new String[rawVoices.length];
        int count = 0;
        for (String voice : rawVoices) {
            if (voice == null || voice.trim().isEmpty()) continue;
            cleaned[count++] = voice.trim();
        }
        if (count == 0) return new String[] { "" };
        String[] result = new String[count];
        System.arraycopy(cleaned, 0, result, 0, count);
        return result;
    }

    private static String[] mergeVoices(String primaryVoice, String[] fallbackVoices) {
        String[] rawFallbacks = fallbackVoices == null ? new String[0] : fallbackVoices;
        String[] merged = new String[rawFallbacks.length + 1];
        merged[0] = primaryVoice;
        System.arraycopy(rawFallbacks, 0, merged, 1, rawFallbacks.length);
        return merged;
    }

    private String voiceForIndex(int voiceIndex) {
        if (voiceIndex < 0 || voiceIndex >= voices.length) return voices[0];
        return voices[voiceIndex];
    }

    private static long elapsedSince(long startedAtMs) {
        return System.currentTimeMillis() - startedAtMs;
    }

    void stop() {
        callbackGate.cancelCurrent();
        if (call != null || player != null || playing) {
            Log.d(tag, "gateway tts stop");
        }
        if (call != null) {
            call.cancel();
            call = null;
        }
        cancelCurrentWsPlayback();
        stopStreamTrack();
        stopPlayer();
    }

    private void cancelCurrentWsPlayback() {
        synchronized (wsLock) {
            if (currentWsPlayback == null) return;
            cancelWsTimeoutLocked(currentWsPlayback);
            cancelWsIdleLocked(currentWsPlayback);
            currentWsPlayback = null;
        }
    }

    private void streamPcmResponse(int run, String id, String text, PlaybackOptions playback,
            Call requestCall,
            Response response, Listener listener, long requestStartedAtMs) throws IOException {
        if (response.body() == null) {
            clearCallIfOwned(requestCall);
            postError(run, listener, id, "empty audio");
            return;
        }
        AudioTrack track = null;
        boolean started = false;
        long totalBytes = 0;
        long streamStartedAtMs = 0;
        try (Response ignored = response; InputStream in = response.body().byteStream()) {
            track = createStreamTrack(playback);
            track.setVolume(playback.volumeGain);
            if (!isCurrent(run)) {
                return;
            }
            streamTrack = track;
            playing = true;
            byte[] buffer = new byte[STREAM_CHUNK_BYTES];
            byte[] alignedBuffer = new byte[STREAM_CHUNK_BYTES + 1];
            PcmSampleBuffer sampleBuffer = new PcmSampleBuffer();
            int read;
            while (isCurrent(run) && (read = in.read(buffer)) != -1) {
                if (read == 0) continue;
                int alignedBytes = sampleBuffer.copyAligned(buffer, read, alignedBuffer);
                if (alignedBytes == 0) continue;
                if (!started) {
                    started = true;
                    streamStartedAtMs = System.currentTimeMillis();
                    Log.i(tag, "gateway tts stream started id=" + id
                            + " elapsedMs=" + elapsedSince(requestStartedAtMs));
                    postStarted(run, listener, id, text);
                }
                int written = track.write(alignedBuffer, 0, alignedBytes);
                if (written > 0) totalBytes += written;
            }
            if (sampleBuffer.hasPendingByte()) {
                Log.d(tag, "gateway tts stream discarded dangling pcm byte id=" + id);
            }
            if (isCurrent(run) && started) {
                waitForStreamDrain(run, streamStartedAtMs, totalBytes);
            }
        } catch (IOException error) {
            if (!isCurrent(run)) return;
            Log.d(tag, "gateway tts stream failed id=" + id + " message=" + error.getMessage());
            postError(run, listener, id, error.getMessage());
            return;
        } finally {
            clearCallIfOwned(requestCall);
            stopStreamTrackIfOwned(track);
        }
        if (!isCurrent(run)) return;
        if (!started || totalBytes == 0) {
            Log.i(tag, "gateway tts empty stream id=" + id);
            postError(run, listener, id, "empty audio");
            return;
        }
        Log.i(tag, "gateway tts stream completed id=" + id
                + " elapsedMs=" + elapsedSince(requestStartedAtMs)
                + " bytes=" + totalBytes);
        warmup();
        postCompleted(run, listener, id);
    }

    private AudioTrack createStreamTrack(PlaybackOptions playback) {
        int min = AudioTrack.getMinBufferSize(STREAM_SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        AudioTrack track = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(playback.voiceCommunication
                                ? AudioAttributes.USAGE_VOICE_COMMUNICATION
                                : AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(STREAM_SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setBufferSizeInBytes(Math.max(min, STREAM_SAMPLE_RATE))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();
        if (playback.preferredOutput != null) {
            try {
                boolean preferred = track.setPreferredDevice(playback.preferredOutput);
                Log.d(tag, "gateway tts AudioTrack preferred output type="
                        + playback.preferredOutput.getType()
                        + " id=" + playback.preferredOutput.getId()
                        + " applied=" + preferred);
            } catch (RuntimeException error) {
                Log.d(tag, "gateway tts AudioTrack preferred output failed: "
                        + error.getMessage());
            }
        }
        track.play();
        Log.d(tag, "gateway tts AudioTrack usage="
                + (playback.voiceCommunication ? "voice_communication" : "media")
                + " playState=" + track.getPlayState());
        return track;
    }

    private void waitForStreamDrain(int run, long startedAtMs, long totalBytes) {
        long durationMs = (totalBytes * 1000L) / (STREAM_SAMPLE_RATE * 2L);
        long elapsedMs = Math.max(0, System.currentTimeMillis() - startedAtMs);
        long remainingMs = Math.max(0, durationMs - elapsedMs) + 160;
        long deadline = System.currentTimeMillis() + remainingMs;
        while (isCurrent(run) && System.currentTimeMillis() < deadline) {
            long sleepMs = Math.min(80, deadline - System.currentTimeMillis());
            if (sleepMs <= 0) return;
            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void playFileResponse(int run, String id, String text, String responseFormat,
            PlaybackOptions playback, Call requestCall, Response response, Listener listener, int voiceIndex,
            long requestStartedAtMs)
            throws IOException {
        byte[] audioBytes;
        try (Response ignored = response) {
            audioBytes = response.body() == null ? new byte[0] : response.body().bytes();
        }
        if (!isCurrent(run)) return;
        if (audioBytes.length == 0) {
            Log.d(tag, "gateway tts empty audio id=" + id);
            clearCallIfOwned(requestCall);
            postError(run, listener, id, "empty audio");
            return;
        }
        Log.i(tag, "gateway tts audio ready id=" + id
                + " format=" + responseFormat
                + " elapsedMs=" + elapsedSince(requestStartedAtMs)
                + " bytes=" + audioBytes.length);
        File file = new File(cacheDir, "tts_" + run + "." + fileExtension(responseFormat));
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(audioBytes);
        }
        clearCallIfOwned(requestCall);
        main.post(() -> playFile(run, id, text, responseFormat, playback, file, listener, voiceIndex,
                requestStartedAtMs));
    }

    private String fileExtension(String responseFormat) {
        if (OPUS_FORMAT.equals(responseFormat)) return "ogg";
        return "mp3";
    }

    private void playFile(int run, String id, String text, String responseFormat,
            PlaybackOptions playback, File file, Listener listener, int voiceIndex, long requestStartedAtMs) {
        if (!isCurrent(run)) return;
        stopPlayer();
        MediaPlayer mediaPlayer = null;
        try {
            mediaPlayer = new MediaPlayer();
            MediaPlayer ownedPlayer = mediaPlayer;
            player = ownedPlayer;
            ownedPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(playback.voiceCommunication
                            ? AudioAttributes.USAGE_VOICE_COMMUNICATION
                            : AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build());
            if (playback.preferredOutput != null) {
                try {
                    boolean preferred = ownedPlayer.setPreferredDevice(playback.preferredOutput);
                    Log.d(tag, "gateway tts MediaPlayer preferred output type="
                            + playback.preferredOutput.getType()
                            + " id=" + playback.preferredOutput.getId()
                            + " applied=" + preferred);
                } catch (RuntimeException error) {
                    Log.d(tag, "gateway tts MediaPlayer preferred output failed: "
                            + error.getMessage());
                }
            }
            ownedPlayer.setDataSource(file.getAbsolutePath());
            ownedPlayer.setVolume(playback.volumeGain, playback.volumeGain);
            ownedPlayer.setOnCompletionListener(mp -> {
                if (!isCurrent(run)) return;
                Log.i(tag, "gateway tts playback completed id=" + id
                        + " elapsedMs=" + elapsedSince(requestStartedAtMs));
                stopPlayerIfOwned(ownedPlayer);
                listener.onCompleted(id);
            });
            ownedPlayer.setOnErrorListener((mp, what, extra) -> {
                if (!isCurrent(run)) return true;
                Log.d(tag, "gateway tts playback error format=" + responseFormat + " what=" + what + " extra=" + extra);
                stopPlayerIfOwned(ownedPlayer);
                String nextFormat = nextFallbackFormat(responseFormat);
                if (nextFormat != null) {
                    requestSpeech(run, id, text, nextFormat, playback, listener, voiceIndex, requestStartedAtMs);
                } else {
                    listener.onError(id, "playback error");
                }
                return true;
            });
            ownedPlayer.prepare();
            if (!isCurrent(run)) {
                stopPlayerIfOwned(ownedPlayer);
                return;
            }
            playing = true;
            Log.i(tag, "gateway tts playback started id=" + id
                    + " format=" + responseFormat
                    + " elapsedMs=" + elapsedSince(requestStartedAtMs));
            listener.onStarted(id, text);
            ownedPlayer.start();
            warmup();
        } catch (Exception error) {
            Log.d(tag, "gateway tts playback failed format=" + responseFormat + ": " + error.getMessage());
            stopPlayerIfOwned(mediaPlayer);
            if (!isCurrent(run)) return;
            String nextFormat = nextFallbackFormat(responseFormat);
            if (nextFormat != null) {
                requestSpeech(run, id, text, nextFormat, playback, listener, voiceIndex, requestStartedAtMs);
            } else {
                listener.onError(id, error.getMessage());
            }
        }
    }

    private static float clampGain(float gain) {
        if (Float.isNaN(gain)) return 1.0f;
        return Math.max(0.0f, Math.min(1.0f, gain));
    }

    private static String describeDevice(AudioDeviceInfo device) {
        if (device == null) return "none";
        return device.getType() + "/" + device.getId();
    }

    private void postStarted(int run, Listener listener, String id, String text) {
        main.post(() -> {
            if (isCurrent(run)) listener.onStarted(id, text);
        });
    }

    private void postCompleted(int run, Listener listener, String id) {
        main.post(() -> {
            if (isCurrent(run)) listener.onCompleted(id);
        });
    }

    private void postError(int run, Listener listener, String id, String message) {
        main.post(() -> {
            if (isCurrent(run)) listener.onError(id, message);
        });
    }

    private boolean isCurrent(int run) {
        return callbackGate.isCurrent(run);
    }

    private void clearCallIfOwned(Call requestCall) {
        if (call == requestCall) call = null;
    }

    private void stopPlayer() {
        MediaPlayer ownedPlayer = player;
        player = null;
        playing = false;
        releasePlayer(ownedPlayer);
    }

    private void stopPlayerIfOwned(MediaPlayer ownedPlayer) {
        if (ownedPlayer == null) return;
        if (player == ownedPlayer) {
            player = null;
            playing = false;
        }
        releasePlayer(ownedPlayer);
    }

    private void releasePlayer(MediaPlayer ownedPlayer) {
        if (ownedPlayer == null) return;
        try { ownedPlayer.stop(); } catch (Exception ignored) { }
        try { ownedPlayer.release(); } catch (Exception ignored) { }
    }

    private void stopStreamTrack() {
        AudioTrack ownedTrack = streamTrack;
        streamTrack = null;
        playing = false;
        releaseStreamTrack(ownedTrack);
    }

    private void stopStreamTrackIfOwned(AudioTrack ownedTrack) {
        if (ownedTrack == null) return;
        if (streamTrack == ownedTrack) {
            streamTrack = null;
            playing = false;
        }
        releaseStreamTrack(ownedTrack);
    }

    private void releaseStreamTrack(AudioTrack ownedTrack) {
        if (ownedTrack == null) return;
        try { ownedTrack.pause(); } catch (Exception ignored) { }
        try { ownedTrack.flush(); } catch (Exception ignored) { }
        try { ownedTrack.release(); } catch (Exception ignored) { }
    }
}
