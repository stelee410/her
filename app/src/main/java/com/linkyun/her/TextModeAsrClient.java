package com.linkyun.her;

import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

final class TextModeAsrClient extends WebSocketListener {
    interface Listener {
        void onStarted();
        void onFinalText(String text);
        void onError(String message);
        void onClosed();
    }

    private final String tag;
    private final Handler main;
    private final String baseUrl;
    private final String apiKey;
    private final OkHttpClient http;
    private final RealtimeCallbackGate callbackGate = new RealtimeCallbackGate();

    private WebSocket socket;
    private Listener listener;
    private String taskId;
    private int generation;
    private boolean taskStarted;
    private boolean finishSent;
    private String latestText = "";
    private String finalText = "";
    private long startedAtMs;
    private long finishSentAtMs;

    TextModeAsrClient(String tag, Handler main, String baseUrl, String apiKey) {
        this.tag = tag;
        this.main = main;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.http = new OkHttpClient.Builder()
                .pingInterval(20, TimeUnit.SECONDS)
                .build();
    }

    boolean isCapturing() {
        return socket != null && taskStarted && !finishSent;
    }

    void start(Listener nextListener) {
        if (socket != null) return;
        if (apiKey == null || apiKey.isEmpty()) {
            nextListener.onError("Missing AGENTLLM_API_KEY");
            return;
        }
        listener = nextListener;
        taskId = UUID.randomUUID().toString();
        taskStarted = false;
        finishSent = false;
        latestText = "";
        finalText = "";
        startedAtMs = SystemClock.elapsedRealtime();
        finishSentAtMs = 0;
        generation = callbackGate.nextGeneration();
        Log.i(tag, "text asr start task=" + taskId);
        Request request = new Request.Builder()
                .url(wsUrl(baseUrl, apiKey))
                .build();
        socket = http.newWebSocket(request, this);
    }

    void sendAudio(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return;
        if (socket != null && taskStarted && !finishSent) {
            socket.send(ByteString.of(bytes));
        }
    }

    void finish() {
        if (socket == null || finishSent) return;
        finishSent = true;
        finishSentAtMs = SystemClock.elapsedRealtime();
        Log.i(tag, "text asr finish sent elapsedMs=" + elapsedSinceStart());
        try {
            socket.send(TextModeAsrEvent.finishTask(taskId).toString());
        } catch (JSONException error) {
            postError(generation, "ASR finish failed");
        }
    }

    void cancel() {
        callbackGate.invalidate();
        taskStarted = false;
        finishSent = false;
        latestText = "";
        finalText = "";
        if (socket != null) {
            socket.close(1000, "client closing");
            socket = null;
        }
        listener = null;
    }

    @Override public void onOpen(WebSocket webSocket, Response response) {
        if (webSocket != socket) {
            webSocket.close(1000, "stale connection");
            return;
        }
        try {
            webSocket.send(TextModeAsrEvent.runTask(taskId).toString());
        } catch (JSONException error) {
            postError(generation, "ASR start failed");
        }
    }

    @Override public void onMessage(WebSocket webSocket, String text) {
        if (webSocket != socket) return;
        int activeGeneration = generation;
        try {
            JSONObject event = new JSONObject(text);
            Log.d(tag, "text asr event " + event.optJSONObject("header"));
            if (TextModeAsrEvent.isTaskStarted(event)) {
                taskStarted = true;
                Log.i(tag, "text asr task started elapsedMs=" + elapsedSinceStart());
                postIfCurrent(webSocket, activeGeneration, () -> {
                    if (listener != null) listener.onStarted();
                });
                return;
            }
            if (TextModeAsrEvent.isResultGenerated(event)) {
                String sentence = TextModeAsrEvent.sentenceText(event);
                if (!sentence.isEmpty()) latestText = sentence;
                if (TextModeAsrEvent.isFinalSentence(event)) {
                    finalText = latestText;
                    Log.i(tag, "text asr final sentence elapsedMs=" + elapsedSinceStart()
                            + " afterFinishMs=" + elapsedSinceFinish()
                            + " chars=" + finalText.length());
                }
                return;
            }
            if (TextModeAsrEvent.isTaskFinished(event)) {
                String result = finalText.isEmpty() ? latestText : finalText;
                Log.i(tag, "text asr task finished elapsedMs=" + elapsedSinceStart()
                        + " afterFinishMs=" + elapsedSinceFinish()
                        + " chars=" + result.length());
                postIfGeneration(activeGeneration, () -> {
                    if (listener != null) listener.onFinalText(result);
                });
                closeIfOwned(webSocket);
            }
        } catch (JSONException error) {
            postError(activeGeneration, "Bad ASR event");
        }
    }

    @Override public void onFailure(WebSocket webSocket, Throwable error, Response response) {
        if (webSocket != socket) return;
        int activeGeneration = generation;
        closeIfOwned(webSocket);
        Log.i(tag, "text asr failure elapsedMs=" + elapsedSinceStart()
                + " message=" + (error.getMessage() == null ? "" : error.getMessage()));
        postError(activeGeneration, error.getMessage() == null ? "ASR failed" : error.getMessage());
    }

    @Override public void onClosed(WebSocket webSocket, int code, String reason) {
        if (webSocket != socket) return;
        int activeGeneration = generation;
        closeIfOwned(webSocket);
        Log.i(tag, "text asr closed elapsedMs=" + elapsedSinceStart()
                + " code=" + code + " reason=" + reason);
        postIfGeneration(activeGeneration, () -> {
            if (listener != null) listener.onClosed();
        });
    }

    private void closeIfOwned(WebSocket webSocket) {
        if (webSocket == socket) {
            socket = null;
            taskStarted = false;
            finishSent = false;
        }
    }

    private long elapsedSinceStart() {
        return startedAtMs == 0 ? 0 : SystemClock.elapsedRealtime() - startedAtMs;
    }

    private long elapsedSinceFinish() {
        return finishSentAtMs == 0 ? 0 : SystemClock.elapsedRealtime() - finishSentAtMs;
    }

    private void postError(int activeGeneration, String message) {
        main.post(() -> {
            if (callbackGate.accepts(activeGeneration) && listener != null) {
                listener.onError(message);
            }
        });
    }

    private void postIfCurrent(WebSocket webSocket, int activeGeneration, Runnable runnable) {
        main.post(() -> {
            if (webSocket == socket && callbackGate.accepts(activeGeneration)) runnable.run();
        });
    }

    private void postIfGeneration(int activeGeneration, Runnable runnable) {
        main.post(() -> {
            if (callbackGate.accepts(activeGeneration)) runnable.run();
        });
    }

    static String wsUrl(String baseUrl, String apiKey) {
        String base = baseUrl == null || baseUrl.isEmpty()
                ? "https://agentllm.linkyun.co"
                : baseUrl;
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.startsWith("https://")) {
            base = "wss://" + base.substring("https://".length());
        } else if (base.startsWith("http://")) {
            base = "ws://" + base.substring("http://".length());
        }
        return base + "/v1beta/dashscope/asr/ws?api_key=" + encode(apiKey);
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8");
        } catch (UnsupportedEncodingException error) {
            return value == null ? "" : value;
        }
    }
}
