package com.linkyun.her;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class WeatherIntentResolver {
    private WeatherIntentResolver() { }

    static JSONObject requestBody(String model, String agentName, String text) throws JSONException {
        JSONObject body = new JSONObject();
        JSONArray messages = new JSONArray();

        JSONObject system = new JSONObject();
        system.put("role", "system");
        system.put("content",
                "你是 " + agentName + " 的后台天气意图解析器，只做结构化槽位抽取。\n" +
                "判断用户是否在询问天气、气温、温度或下雨；如果是，抽取用户明确指定的城市/地点。\n" +
                "重要：不要把请求语气、助词、寒暄或动作词当成地点，例如“能不能”“可不可以”“帮我”“查一下”“看一下”“请问”都不是地点。\n" +
                "如果用户没有明确指定城市/地点，city 必须是空字符串，客户端会用定位查询。\n" +
                "必须只输出一行紧凑 JSON，不要 Markdown，不要换行，不要解释：{\"is_weather_query\":true,\"city\":\"深圳\",\"reason\":\"...\"}。\n" +
                "如果不是天气问题，输出：{\"is_weather_query\":false,\"city\":\"\",\"reason\":\"...\"}。");
        messages.put(system);

        JSONObject user = new JSONObject();
        user.put("role", "user");
        user.put("content", text == null ? "" : text);
        messages.put(user);

        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", 0.0);
        body.put("max_tokens", 120);
        body.put("response_format", new JSONObject().put("type", "json_object"));
        body.put("stream", false);
        return body;
    }

    static Result parse(String content) throws JSONException {
        JSONObject object = parseJsonObject(content);
        boolean isWeatherQuery = object.optBoolean("is_weather_query", false);
        String city = object.optString("city", "").trim();
        String reason = object.optString("reason", "").trim();
        if (!isWeatherQuery) city = "";
        return new Result(isWeatherQuery, city, reason);
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

    static final class Result {
        final boolean isWeatherQuery;
        final String city;
        final String reason;

        Result(boolean isWeatherQuery, String city, String reason) {
            this.isWeatherQuery = isWeatherQuery;
            this.city = city;
            this.reason = reason;
        }
    }
}
