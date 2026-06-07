package com.linkyun.her;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class BackgroundToolRouteDecider {
    private BackgroundToolRouteDecider() {
    }

    static JSONObject requestBody(String model, String agentName, String userText) throws JSONException {
        JSONArray messages = new JSONArray();
        JSONObject system = new JSONObject();
        system.put("role", "system");
        system.put("content",
                "你是 " + cleanOr(agentName, "Doris") + " 的后台意识模型，负责判断用户当前这句话是否需要客户端工具。\n" +
                "当前可用工具：daily_news（读取每日新闻热点，来源 https://agentnews.linkyun.co/）。\n" +
                "如果用户想查、看、听、播报新闻/热点/每日新闻热点，返回 {\"tool\":\"daily_news\",\"confidence\":0.0到1.0,\"reason\":\"...\"}。\n" +
                "如果只是普通聊天、评价刚才内容、追问旧回答、闲聊或不确定，返回 {\"tool\":\"none\",\"confidence\":0.0到1.0,\"reason\":\"...\"}。\n" +
                "只输出 JSON，不要解释。");
        messages.put(system);

        JSONObject user = new JSONObject();
        user.put("role", "user");
        user.put("content", clean(userText));
        messages.put(user);

        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", 0.0);
        body.put("stream", false);
        return body;
    }

    static Decision parse(String content) throws JSONException {
        JSONObject object = parseJsonObject(content);
        return new Decision(
                object.optString("tool", "none").trim(),
                object.optDouble("confidence", 0.0),
                object.optString("reason", "").trim());
    }

    private static JSONObject parseJsonObject(String content) throws JSONException {
        String trimmed = clean(content);
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            trimmed = trimmed.substring(start, end + 1);
        }
        return new JSONObject(trimmed);
    }

    private static String cleanOr(String value, String fallback) {
        String cleaned = clean(value);
        return cleaned.isEmpty() ? fallback : cleaned;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    static final class Decision {
        final String tool;
        final double confidence;
        final String reason;

        Decision(String tool, double confidence, String reason) {
            this.tool = tool;
            this.confidence = confidence;
            this.reason = reason;
        }
    }
}
