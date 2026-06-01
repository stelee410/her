package com.linkyun.her;

import android.os.Handler;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

final class RealtimeClient extends WebSocketListener {
    interface Host {
        Handler mainHandler();
        JSONObject buildRealtimeSessionPayload();
        void onRealtimeConnecting();
        void onRealtimeEvent(String type, JSONObject payload);
        void onRealtimeAudio(byte[] bytes);
        void onRealtimeError(String message);
        void onRealtimeClosed();
    }

    private final String tag;
    private final Host host;
    private final OkHttpClient http = new OkHttpClient.Builder()
            .pingInterval(20, TimeUnit.SECONDS)
            .build();
    private WebSocket socket;
    private boolean sessionCreated = false;

    RealtimeClient(String tag, Host host) {
        this.tag = tag;
        this.host = host;
    }

    boolean isOpen() {
        return socket != null && sessionCreated;
    }

    void connect() {
        if (BuildConfig.AGENTVOICE_API_KEY.isEmpty()) {
            host.onRealtimeError("Missing AGENTVOICE_API_KEY in gradle.properties");
            return;
        }
        if (socket != null) return;
        Log.d(tag, "connect realtime");
        host.onRealtimeConnecting();
        String key;
        try {
            key = URLEncoder.encode(BuildConfig.AGENTVOICE_API_KEY, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            key = BuildConfig.AGENTVOICE_API_KEY;
        }
        Request request = new Request.Builder()
                .url(BuildConfig.AGENTVOICE_REALTIME_URL + "?api_key=" + key)
                .build();
        socket = http.newWebSocket(request, this);
    }

    @Override public void onOpen(WebSocket webSocket, Response response) {
        Log.d(tag, "websocket open");
        sendEvent("session.start", host.buildRealtimeSessionPayload());
    }

    @Override public void onMessage(WebSocket webSocket, String text) {
        try {
            JSONObject event = new JSONObject(text);
            JSONObject payload = event.optJSONObject("payload");
            String type = event.optString("type");
            Log.d(tag, "event " + type + " payload=" + (payload == null ? "{}" : payload.toString()));
            host.mainHandler().post(() -> host.onRealtimeEvent(type, payload));
        } catch (JSONException e) {
            host.onRealtimeError("Bad realtime event");
        }
    }

    @Override public void onMessage(WebSocket webSocket, ByteString bytes) {
        Log.d(tag, "audio bytes " + bytes.size());
        host.onRealtimeAudio(bytes.toByteArray());
    }

    @Override public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        socket = null;
        sessionCreated = false;
        Log.e(tag, "websocket failure", t);
        host.mainHandler().post(() -> host.onRealtimeError(t.getMessage() == null ? "Realtime failed" : t.getMessage()));
    }

    @Override public void onClosed(WebSocket webSocket, int code, String reason) {
        socket = null;
        sessionCreated = false;
        Log.d(tag, "websocket closed code=" + code + " reason=" + reason);
        host.mainHandler().post(host::onRealtimeClosed);
    }

    void markSessionCreated() {
        sessionCreated = true;
    }

    void close() {
        sessionCreated = false;
        if (socket != null) {
            socket.close(1000, "client closing");
            socket = null;
        }
    }

    void sendInputText(String text) {
        Log.d(tag, "send input_text " + text);
        JSONObject payload = new JSONObject();
        try {
            payload.put("text", text);
        } catch (JSONException ignored) { }
        sendEvent("input_text", payload);
    }

    void sendAudio(byte[] bytes) {
        if (socket != null) socket.send(ByteString.of(bytes));
    }

    void sendEvent(String type, JSONObject payload) {
        if (socket == null) return;
        Log.d(tag, "send event " + type);
        JSONObject envelope = new JSONObject();
        try {
            envelope.put("type", type);
            if (payload != null) envelope.put("payload", payload);
        } catch (JSONException ignored) { }
        socket.send(envelope.toString());
    }
}
