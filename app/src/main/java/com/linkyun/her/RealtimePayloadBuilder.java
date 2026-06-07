package com.linkyun.her;

import org.json.JSONException;
import org.json.JSONObject;

final class RealtimePayloadBuilder {
    private RealtimePayloadBuilder() {
    }

    static JSONObject sessionPayload(String instructions, String voiceId) throws JSONException {
        JSONObject audio = new JSONObject();
        audio.put("input_format", "pcm16");
        audio.put("output_format", "pcm16");
        audio.put("sample_rate", 16000);
        audio.put("channels", 1);
        audio.put("frame_duration_ms", 20);

        JSONObject client = new JSONObject();
        client.put("type", "android");
        client.put("version", "1.0.0");

        JSONObject payload = new JSONObject();
        payload.put("agent_id", "omnia_default");
        payload.put("mode", "realtime");
        payload.put("model_profile", "realtime_doubao");
        payload.put("audio", audio);
        payload.put("instructions", instructions == null ? "" : instructions);
        payload.put("voice", voiceId == null ? "" : voiceId);
        payload.put("client", client);
        return payload;
    }

    static JSONObject contextUpdate(String instructions, String fact, boolean includeFact, int factLimit)
            throws JSONException {
        JSONObject payload = new JSONObject();
        payload.put("instructions", instructions == null ? "" : instructions);
        if (includeFact && fact != null && !fact.trim().isEmpty()) {
            payload.put("fact", trimTail(fact, factLimit));
        }
        return payload;
    }

    static JSONObject factContextUpdate(String fact, int factLimit) throws JSONException {
        if (fact == null || fact.trim().isEmpty()) return null;
        JSONObject payload = new JSONObject();
        payload.put("fact", trimTail(fact, factLimit));
        return payload;
    }

    static String trimTail(String value, int limit) {
        if (value == null) return "";
        if (limit <= 0) return "";
        if (value.length() <= limit) return value;
        return value.substring(value.length() - limit);
    }
}
