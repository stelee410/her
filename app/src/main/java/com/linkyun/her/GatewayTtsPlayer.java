package com.linkyun.her;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

final class GatewayTtsPlayer {
    interface Listener {
        void onStarted(String id, String text);
        void onCompleted(String id);
        void onError(String id, String message);
    }

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String OPUS_FORMAT = "opus";
    private static final String PCM_FORMAT = "pcm";
    private static final String MP3_FORMAT = "mp3";
    private static final int STREAM_SAMPLE_RATE = 24000;
    private static final int STREAM_CHUNK_BYTES = 4096;

    private final String tag;
    private final OkHttpClient http;
    private final Handler main;
    private final File cacheDir;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final String voice;

    private final GatewayTtsCallbackGate callbackGate = new GatewayTtsCallbackGate();

    private volatile Call call;
    private volatile MediaPlayer player;
    private volatile AudioTrack streamTrack;
    private volatile boolean playing = false;

    GatewayTtsPlayer(String tag, OkHttpClient http, Handler main, File cacheDir,
            String baseUrl, String apiKey, String model, String voice) {
        this.tag = tag;
        this.http = http;
        this.main = main;
        this.cacheDir = cacheDir;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.voice = voice;
    }

    boolean isPlaying() {
        return playing;
    }

    void play(String id, String text, Listener listener) {
        stop();
        if (text == null || text.trim().isEmpty()) return;
        int run = callbackGate.nextRun();
        Log.d(tag, "gateway tts request id=" + id + " len=" + text.trim().length());
        if (apiKey == null || apiKey.isEmpty()) {
            postError(run, listener, id, "Missing AGENTLLM_API_KEY");
            return;
        }
        requestSpeech(run, id, text, PCM_FORMAT, listener);
    }

    private void requestSpeech(int run, String id, String text, String responseFormat, Listener listener) {
        if (!isCurrent(run)) return;
        JSONObject body = new JSONObject();
        try {
            body.put("model", model);
            body.put("input", text);
            body.put("voice", voice);
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
                Log.d(tag, "gateway tts request failed id=" + id + " message=" + error.getMessage());
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
                    Log.d(tag, "gateway tts http failed id=" + id + " format=" + responseFormat + " code=" + response.code());
                    String nextFormat = nextFallbackFormat(responseFormat);
                    if (nextFormat != null) {
                        clearCallIfOwned(requestCall);
                        requestSpeech(run, id, text, nextFormat, listener);
                    } else {
                        clearCallIfOwned(requestCall);
                        postError(run, listener, id, "HTTP " + response.code() + " " + responseText);
                    }
                    return;
                }
                if (PCM_FORMAT.equals(responseFormat)) {
                    streamPcmResponse(run, id, text, requestCall, response, listener);
                } else {
                    playFileResponse(run, id, text, responseFormat, requestCall, response, listener);
                }
            }
        });
    }

    private String nextFallbackFormat(String responseFormat) {
        if (PCM_FORMAT.equals(responseFormat)) return OPUS_FORMAT;
        if (OPUS_FORMAT.equals(responseFormat)) return MP3_FORMAT;
        return null;
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
        stopStreamTrack();
        stopPlayer();
    }

    private void streamPcmResponse(int run, String id, String text, Call requestCall,
            Response response, Listener listener) throws IOException {
        if (response.body() == null) {
            clearCallIfOwned(requestCall);
            postError(run, listener, id, "empty audio");
            return;
        }
        AudioTrack track = null;
        boolean started = false;
        long totalBytes = 0;
        long startedAtMs = 0;
        try (Response ignored = response; InputStream in = response.body().byteStream()) {
            track = createStreamTrack();
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
                    startedAtMs = System.currentTimeMillis();
                    Log.d(tag, "gateway tts stream started id=" + id);
                    postStarted(run, listener, id, text);
                }
                int written = track.write(alignedBuffer, 0, alignedBytes);
                if (written > 0) totalBytes += written;
            }
            if (sampleBuffer.hasPendingByte()) {
                Log.d(tag, "gateway tts stream discarded dangling pcm byte id=" + id);
            }
            if (isCurrent(run) && started) {
                waitForStreamDrain(run, startedAtMs, totalBytes);
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
            Log.d(tag, "gateway tts empty stream id=" + id);
            postError(run, listener, id, "empty audio");
            return;
        }
        Log.d(tag, "gateway tts stream completed id=" + id + " bytes=" + totalBytes);
        postCompleted(run, listener, id);
    }

    private AudioTrack createStreamTrack() {
        int min = AudioTrack.getMinBufferSize(STREAM_SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        AudioTrack track = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
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
        track.play();
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
            Call requestCall, Response response, Listener listener) throws IOException {
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
        Log.d(tag, "gateway tts audio ready id=" + id + " format=" + responseFormat + " bytes=" + audioBytes.length);
        File file = new File(cacheDir, "tts_" + run + "." + fileExtension(responseFormat));
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(audioBytes);
        }
        clearCallIfOwned(requestCall);
        main.post(() -> playFile(run, id, text, responseFormat, file, listener));
    }

    private String fileExtension(String responseFormat) {
        if (OPUS_FORMAT.equals(responseFormat)) return "ogg";
        return "mp3";
    }

    private void playFile(int run, String id, String text, String responseFormat, File file, Listener listener) {
        if (!isCurrent(run)) return;
        stopPlayer();
        MediaPlayer mediaPlayer = null;
        try {
            mediaPlayer = new MediaPlayer();
            MediaPlayer ownedPlayer = mediaPlayer;
            player = ownedPlayer;
            ownedPlayer.setDataSource(file.getAbsolutePath());
            ownedPlayer.setOnCompletionListener(mp -> {
                if (!isCurrent(run)) return;
                Log.d(tag, "gateway tts playback completed id=" + id);
                stopPlayerIfOwned(ownedPlayer);
                listener.onCompleted(id);
            });
            ownedPlayer.setOnErrorListener((mp, what, extra) -> {
                if (!isCurrent(run)) return true;
                Log.d(tag, "gateway tts playback error format=" + responseFormat + " what=" + what + " extra=" + extra);
                stopPlayerIfOwned(ownedPlayer);
                String nextFormat = nextFallbackFormat(responseFormat);
                if (nextFormat != null) {
                    requestSpeech(run, id, text, nextFormat, listener);
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
            Log.d(tag, "gateway tts playback started id=" + id + " format=" + responseFormat);
            listener.onStarted(id, text);
            ownedPlayer.start();
        } catch (Exception error) {
            Log.d(tag, "gateway tts playback failed format=" + responseFormat + ": " + error.getMessage());
            stopPlayerIfOwned(mediaPlayer);
            if (!isCurrent(run)) return;
            String nextFormat = nextFallbackFormat(responseFormat);
            if (nextFormat != null) {
                requestSpeech(run, id, text, nextFormat, listener);
            } else {
                listener.onError(id, error.getMessage());
            }
        }
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
