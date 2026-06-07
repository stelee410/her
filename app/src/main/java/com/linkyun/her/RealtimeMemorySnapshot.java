package com.linkyun.her;

import org.json.JSONArray;
import org.json.JSONObject;

final class RealtimeMemorySnapshot {
    private RealtimeMemorySnapshot() { }

    static String fromPayload(JSONObject payload, int instructionLimit) {
        if (payload == null) return "";
        StringBuilder snapshot = new StringBuilder();
        String instructions = payload.optString("instructions", "").trim();
        if (!instructions.isEmpty()) {
            snapshot.append("## AgentVoice working instructions\n")
                    .append(RealtimePayloadBuilder.trimTail(instructions, instructionLimit))
                    .append('\n');
        }
        JSONArray dialogue = payload.optJSONArray("dialogue");
        if (dialogue != null && dialogue.length() > 0) {
            snapshot.append("## AgentVoice recent dialogue\n");
            for (int i = 0; i < dialogue.length(); i++) {
                JSONObject item = dialogue.optJSONObject(i);
                if (item == null) continue;
                snapshot.append(item.optString("role", "unknown"))
                        .append(": ")
                        .append(item.optString("text", ""))
                        .append('\n');
            }
        }
        return snapshot.toString().trim();
    }

    static boolean shouldPersist(String text) {
        return text != null && !text.trim().isEmpty() && !WeatherSkill.isTransientMemory(text);
    }
}
