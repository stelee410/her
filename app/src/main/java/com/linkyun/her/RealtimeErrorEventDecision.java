package com.linkyun.her;

import org.json.JSONObject;

final class RealtimeErrorEventDecision {
    enum Action {
        RETRY,
        ERROR
    }

    final Action action;
    final String message;

    private RealtimeErrorEventDecision(Action action, String message) {
        this.action = action;
        this.message = message;
    }

    static RealtimeErrorEventDecision fromPayload(JSONObject payload) {
        if (payload != null && payload.optBoolean("recoverable", false)) {
            return new RealtimeErrorEventDecision(Action.RETRY, message(payload));
        }
        return new RealtimeErrorEventDecision(Action.ERROR, message(payload));
    }

    private static String message(JSONObject payload) {
        if (payload == null) return "Realtime error";
        String value = payload.optString("message", "Realtime error");
        return value == null || value.trim().isEmpty() ? "Realtime error" : value;
    }
}
