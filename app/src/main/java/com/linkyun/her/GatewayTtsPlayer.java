package com.linkyun.her;

import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

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
        JSONObject body = new JSONObject();
        try {
            body.put("model", model);
            body.put("input", text);
            body.put("voice", voice);
            body.put("response_format", "mp3");
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
                    Log.d(tag, "gateway tts http failed id=" + id + " code=" + response.code());
                    main.post(() -> listener.onError(id, "HTTP " + response.code() + " " + responseText));
                    return;
                }
                byte[] audioBytes = response.body() == null ? new byte[0] : response.body().bytes();
                if (audioBytes.length == 0) {
                    Log.d(tag, "gateway tts empty audio id=" + id);
                    main.post(() -> listener.onError(id, "empty audio"));
                    return;
                }
                Log.d(tag, "gateway tts audio ready id=" + id + " bytes=" + audioBytes.length);
                File file = new File(cacheDir, "tts_" + run + ".mp3");
                try (FileOutputStream out = new FileOutputStream(file)) {
                    out.write(audioBytes);
                }
                call = null;
                main.post(() -> playFile(run, id, text, file, listener));
            }
        });
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
        stopPlayer();
    }

    private void playFile(int run, String id, String text, File file, Listener listener) {
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
                Log.d(tag, "gateway tts playback error what=" + what + " extra=" + extra);
                stopPlayer();
                listener.onError(id, "playback error");
                return true;
            });
            player.prepare();
            playing = true;
            Log.d(tag, "gateway tts playback started id=" + id);
            listener.onStarted(id, text);
            player.start();
        } catch (Exception error) {
            Log.d(tag, "gateway tts playback failed: " + error.getMessage());
            stopPlayer();
            listener.onError(id, error.getMessage());
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
}
