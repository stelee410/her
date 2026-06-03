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

    private Call call;
    private MediaPlayer player;
    private AudioTrack streamTrack;
    private int generation = 0;
    private boolean playing = false;

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
        int run = ++generation;
        Log.d(tag, "gateway tts request id=" + id + " len=" + text.trim().length());
        if (apiKey == null || apiKey.isEmpty()) {
            main.post(() -> listener.onError(id, "Missing AGENTLLM_API_KEY"));
            return;
        }
        requestSpeech(run, id, text, PCM_FORMAT, listener);
    }

    private void requestSpeech(int run, String id, String text, String responseFormat, Listener listener) {
        JSONObject body = new JSONObject();
        try {
            body.put("model", model);
            body.put("input", text);
            body.put("voice", voice);
            body.put("response_format", responseFormat);
            body.put("speed", 1.0);
        } catch (Exception error) {
            main.post(() -> listener.onError(id, error.getMessage()));
            return;
        }
        Request request = new Request.Builder()
                .url(baseUrl + "/v1/audio/speech")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), JSON))
                .build();
        call = http.newCall(request);
        call.enqueue(new Callback() {
            @Override public void onFailure(Call requestCall, IOException error) {
                if (run != generation) return;
                Log.d(tag, "gateway tts request failed id=" + id + " message=" + error.getMessage());
                main.post(() -> listener.onError(id, error.getMessage()));
            }

            @Override public void onResponse(Call requestCall, Response response) throws IOException {
                if (run != generation) return;
                if (!response.isSuccessful()) {
                    String responseText = response.body() == null ? "" : response.body().string();
                    Log.d(tag, "gateway tts http failed id=" + id + " format=" + responseFormat + " code=" + response.code());
                    String nextFormat = nextFallbackFormat(responseFormat);
                    if (nextFormat != null) {
                        call = null;
                        requestSpeech(run, id, text, nextFormat, listener);
                    } else {
                        main.post(() -> listener.onError(id, "HTTP " + response.code() + " " + responseText));
                    }
                    return;
                }
                if (PCM_FORMAT.equals(responseFormat)) {
                    streamPcmResponse(run, id, text, response, listener);
                } else {
                    playFileResponse(run, id, text, responseFormat, response, listener);
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
        generation++;
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

    private void streamPcmResponse(int run, String id, String text, Response response, Listener listener) throws IOException {
        if (response.body() == null) {
            main.post(() -> listener.onError(id, "empty audio"));
            return;
        }
        AudioTrack track = null;
        boolean started = false;
        long totalBytes = 0;
        long startedAtMs = 0;
        try (Response ignored = response; InputStream in = response.body().byteStream()) {
            track = createStreamTrack();
            streamTrack = track;
            playing = true;
            byte[] buffer = new byte[STREAM_CHUNK_BYTES];
            byte[] alignedBuffer = new byte[STREAM_CHUNK_BYTES + 1];
            PcmSampleBuffer sampleBuffer = new PcmSampleBuffer();
            int read;
            while (run == generation && (read = in.read(buffer)) != -1) {
                if (read == 0) continue;
                int alignedBytes = sampleBuffer.copyAligned(buffer, read, alignedBuffer);
                if (alignedBytes == 0) continue;
                if (!started) {
                    started = true;
                    startedAtMs = System.currentTimeMillis();
                    Log.d(tag, "gateway tts stream started id=" + id);
                    main.post(() -> listener.onStarted(id, text));
                }
                int written = track.write(alignedBuffer, 0, alignedBytes);
                if (written > 0) totalBytes += written;
            }
            if (sampleBuffer.hasPendingByte()) {
                Log.d(tag, "gateway tts stream discarded dangling pcm byte id=" + id);
            }
            if (run == generation && started) {
                waitForStreamDrain(run, startedAtMs, totalBytes);
            }
        } catch (IOException error) {
            if (run != generation) return;
            Log.d(tag, "gateway tts stream failed id=" + id + " message=" + error.getMessage());
            main.post(() -> listener.onError(id, error.getMessage()));
            return;
        } finally {
            call = null;
            stopStreamTrack();
        }
        if (run != generation) return;
        if (!started || totalBytes == 0) {
            Log.d(tag, "gateway tts empty stream id=" + id);
            main.post(() -> listener.onError(id, "empty audio"));
            return;
        }
        Log.d(tag, "gateway tts stream completed id=" + id + " bytes=" + totalBytes);
        main.post(() -> listener.onCompleted(id));
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
        while (run == generation && System.currentTimeMillis() < deadline) {
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
            Response response, Listener listener) throws IOException {
        byte[] audioBytes;
        try (Response ignored = response) {
            audioBytes = response.body() == null ? new byte[0] : response.body().bytes();
        }
        if (run != generation) return;
        if (audioBytes.length == 0) {
            Log.d(tag, "gateway tts empty audio id=" + id);
            main.post(() -> listener.onError(id, "empty audio"));
            return;
        }
        Log.d(tag, "gateway tts audio ready id=" + id + " format=" + responseFormat + " bytes=" + audioBytes.length);
        File file = new File(cacheDir, "tts_" + run + "." + fileExtension(responseFormat));
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(audioBytes);
        }
        call = null;
        main.post(() -> playFile(run, id, text, responseFormat, file, listener));
    }

    private String fileExtension(String responseFormat) {
        if (OPUS_FORMAT.equals(responseFormat)) return "ogg";
        return "mp3";
    }

    private void playFile(int run, String id, String text, String responseFormat, File file, Listener listener) {
        if (run != generation) return;
        stopPlayer();
        try {
            player = new MediaPlayer();
            player.setDataSource(file.getAbsolutePath());
            player.setOnCompletionListener(mp -> {
                if (run != generation) return;
                Log.d(tag, "gateway tts playback completed id=" + id);
                stopPlayer();
                listener.onCompleted(id);
            });
            player.setOnErrorListener((mp, what, extra) -> {
                if (run != generation) return true;
                Log.d(tag, "gateway tts playback error format=" + responseFormat + " what=" + what + " extra=" + extra);
                stopPlayer();
                String nextFormat = nextFallbackFormat(responseFormat);
                if (nextFormat != null) {
                    requestSpeech(run, id, text, nextFormat, listener);
                } else {
                    listener.onError(id, "playback error");
                }
                return true;
            });
            player.prepare();
            playing = true;
            Log.d(tag, "gateway tts playback started id=" + id + " format=" + responseFormat);
            listener.onStarted(id, text);
            player.start();
        } catch (Exception error) {
            Log.d(tag, "gateway tts playback failed format=" + responseFormat + ": " + error.getMessage());
            stopPlayer();
            String nextFormat = nextFallbackFormat(responseFormat);
            if (nextFormat != null) {
                requestSpeech(run, id, text, nextFormat, listener);
            } else {
                listener.onError(id, error.getMessage());
            }
        }
    }

    private void stopPlayer() {
        playing = false;
        if (player != null) {
            try { player.stop(); } catch (Exception ignored) { }
            try { player.release(); } catch (Exception ignored) { }
            player = null;
        }
    }

    private void stopStreamTrack() {
        playing = false;
        if (streamTrack != null) {
            try { streamTrack.pause(); } catch (Exception ignored) { }
            try { streamTrack.flush(); } catch (Exception ignored) { }
            try { streamTrack.release(); } catch (Exception ignored) { }
            streamTrack = null;
        }
    }
}
