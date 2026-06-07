package com.linkyun.her;

import org.json.JSONObject;

final class RealtimeEventPayload {
    static final int DEFAULT_OUTPUT_SAMPLE_RATE = 24000;

    private RealtimeEventPayload() {
    }

    static String asrFinalText(JSONObject payload) {
        return optString(payload, "text", "");
    }

    static String assistantState(JSONObject payload) {
        return optString(payload, "state", "ready");
    }

    static String assistantTextDelta(JSONObject payload) {
        return optString(payload, "text", "");
    }

    static int outputSampleRate(JSONObject payload) {
        if (payload == null) return DEFAULT_OUTPUT_SAMPLE_RATE;
        return payload.optInt("sample_rate", DEFAULT_OUTPUT_SAMPLE_RATE);
    }

    private static String optString(JSONObject payload, String name, String fallback) {
        if (payload == null) return fallback;
        return payload.optString(name, fallback);
    }
}
