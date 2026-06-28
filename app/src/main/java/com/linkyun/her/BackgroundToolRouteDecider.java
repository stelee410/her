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
                "当前可用工具：\n" +
                "- daily_news：读取每日新闻热点，来源 https://agentnews.linkyun.co/。\n" +
                "- weather：查询城市或当前位置天气。\n" +
                "- open_tv：打开 AI 电视台/视频频道。\n" +
                "- volume_up：调大语音通话音量。\n" +
                "- volume_down：调小语音通话音量。\n" +
                "如果用户想查、看、听、播报新闻/热点/每日新闻热点，返回 {\"tool\":\"daily_news\",\"confidence\":0.0到1.0,\"reason\":\"...\"}。\n" +
                "如果用户询问天气、气温、温度、下雨、出门穿衣或带伞，返回 {\"tool\":\"weather\",\"confidence\":0.0到1.0,\"reason\":\"...\"}。\n" +
                "如果用户想打开电视、看电视台、看视频频道、换到 AI TV 或找个视频看看，返回 {\"tool\":\"open_tv\",\"confidence\":0.0到1.0,\"reason\":\"...\"}。\n" +
                "如果用户要求调大声音/音量，返回 {\"tool\":\"volume_up\",\"confidence\":0.0到1.0,\"reason\":\"...\"}；要求调小声音/音量，返回 {\"tool\":\"volume_down\",\"confidence\":0.0到1.0,\"reason\":\"...\"}。\n" +
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
