package com.linkyun.her;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class MemoryCompactor {
    private MemoryCompactor() {
    }

    static JSONObject requestBody(String model, String agentName, String userMemory,
            String conversationMemory, MemoryChunk chunk) throws JSONException {
        JSONObject body = new JSONObject();
        JSONArray messages = new JSONArray();

        JSONObject system = new JSONObject();
        system.put("role", "system");
        system.put("content",
                "你是长期记忆压缩器。根据近期对话生成两类内容：\n" +
                "1. memory_md：稳定、可检索、可长期保存的事实/偏好/关系/目标/边界。\n" +
                "2. tone_guidance：下一阶段 Agent 应如何调整说话语气，必须短而具体。\n" +
                "只输出 JSON：{\"memory_md\":\"...\",\"tone_guidance\":\"...\"}");
        messages.put(system);

        JSONObject user = new JSONObject();
        user.put("role", "user");
        user.put("content",
                "Agent 名字：" + agentName + "\n" +
                "已有用户初始化画像：\n" + trimTail(userMemory, 1100) + "\n\n" +
                "已有长期摘要：\n" + trimTail(conversationMemory, 1200) + "\n\n" +
                "请压缩这段新对话，不要丢掉能影响陪伴方式的细节：\n" +
                (chunk == null ? "" : chunk.transcript));
        messages.put(user);

        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", 0.2);
        body.put("stream", false);
        return body;
    }

    static Result parseResult(String content) {
        String value = content == null ? "" : content.trim();
        if (value.isEmpty()) return new Result("", "");
        try {
            JSONObject compact = parseJsonObject(value);
            return new Result(
                    compact.optString("memory_md", value).trim(),
                    compact.optString("tone_guidance", "").trim());
        } catch (JSONException error) {
            return new Result(value, "");
        }
    }

    private static JSONObject parseJsonObject(String content) throws JSONException {
        String trimmed = content == null ? "" : content.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            trimmed = trimmed.substring(start, end + 1);
        }
        return new JSONObject(trimmed);
    }

    private static String trimTail(String value, int limit) {
        if (value == null) return "";
        if (limit <= 0) return "";
        if (value.length() <= limit) return value;
        return value.substring(value.length() - limit);
    }

    static final class Result {
        final String memory;
        final String tone;

        Result(String memory, String tone) {
            this.memory = memory;
            this.tone = tone;
        }
    }
}
