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
        if (isRecoverable(payload)) {
            return new RealtimeErrorEventDecision(Action.RETRY, message(payload));
        }
        return new RealtimeErrorEventDecision(Action.ERROR, message(payload));
    }

    private static boolean isRecoverable(JSONObject payload) {
        if (payload == null) return false;
        if (payload.optBoolean("recoverable", false)) return true;
        String code = payload.optString("code", "");
        String message = payload.optString("message", "");
        return "realtime_unavailable".equals(code) ||
                message.contains("Unexpected server response: 521");
    }

    private static String message(JSONObject payload) {
        if (payload == null) return "Realtime error";
        String value = payload.optString("message", "Realtime error");
        return value == null || value.trim().isEmpty() ? "Realtime error" : value;
    }
}
