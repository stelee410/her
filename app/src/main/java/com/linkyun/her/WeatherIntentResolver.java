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
        JSONObject object;
        try {
            object = parseJsonObject(content);
        } catch (JSONException error) {
            return parseLooseText(content);
        }
        boolean isWeatherQuery = optBoolean(object,
                "is_weather_query", "isWeatherQuery", "weather", "is_weather");
        String city = optString(object, "city", "location", "place", "地点").trim();
        String reason = optString(object, "reason", "理由").trim();
        if (!isWeatherQuery) city = "";
        return new Result(isWeatherQuery, city, reason);
    }

    static Result fallbackFromQuestion(String question) {
        boolean isWeatherQuery = WeatherSkill.isWeatherQuestion(question);
        String city = isWeatherQuery ? extractCityFromQuestion(question) : "";
        return new Result(isWeatherQuery, city, "local weather intent fallback");
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

    private static Result parseLooseText(String content) {
        String value = content == null ? "" : content.trim();
        if (value.isEmpty() || saysNotWeather(value)) return new Result(false, "", value);
        String city = cityAfterLabel(value);
        boolean isWeatherQuery = !city.isEmpty() || WeatherSkill.isWeatherQuestion(value);
        return new Result(isWeatherQuery, isWeatherQuery ? city : "", value);
    }

    private static boolean optBoolean(JSONObject object, String... keys) {
        for (String key : keys) {
            if (!object.has(key)) continue;
            if (object.optBoolean(key, false)) return true;
            String value = object.optString(key, "").trim();
            if ("true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value) || "是".equals(value)) {
                return true;
            }
        }
        return false;
    }

    private static String optString(JSONObject object, String... keys) {
        for (String key : keys) {
            String value = object.optString(key, "").trim();
            if (!value.isEmpty()) return value;
        }
        return "";
    }

    private static boolean saysNotWeather(String value) {
        String lower = value.toLowerCase();
        return value.contains("不是天气") ||
                value.contains("非天气") ||
                lower.contains("not weather") ||
                lower.contains("is_weather_query\":false");
    }

    private static String cityAfterLabel(String value) {
        String[] labels = {"城市", "地点", "city", "location", "place"};
        for (String label : labels) {
            int index = value.toLowerCase().indexOf(label.toLowerCase());
            if (index < 0) continue;
            String tail = value.substring(index + label.length())
                    .replaceFirst("^[：:=\\s\"'“”]+", "");
            String city = cleanCityCandidate(tail);
            if (!city.isEmpty()) return city;
        }
        return "";
    }

    private static String extractCityFromQuestion(String question) {
        String value = question == null ? "" : question.trim();
        if (value.isEmpty()) return "";
        String[] terms = {"天气", "气温", "温度", "下雨", "会不会雨", "weather"};
        for (String term : terms) {
            int index = value.toLowerCase().indexOf(term.toLowerCase());
            if (index <= 0) continue;
            String city = cleanCityCandidate(value.substring(0, index));
            if (!city.isEmpty()) return city;
        }
        return "";
    }

    private static String cleanCityCandidate(String text) {
        String value = text == null ? "" : text;
        value = value.replaceAll("[，。！？?、,.;；：:\\s\"'“”]+", "");
        String[] noise = {
                "能不能", "可不可以", "可以", "麻烦", "帮忙", "帮我", "请问", "问一下",
                "我想知道", "想知道", "查一下", "查一查", "查下", "查查", "查询", "查",
                "看一下", "看一看", "看下", "看看", "一下", "现在", "今天", "今晚",
                "明天", "后天", "大后天", "当地", "这里", "我这里", "我这边", "这边",
                "那边", "这个", "的", "会不会", "会不会有", "有"
        };
        for (String item : noise) {
            value = value.replace(item, "");
        }
        value = value.replace("天气", "")
                .replace("气温", "")
                .replace("温度", "")
                .replace("下雨", "")
                .replace("weather", "")
                .trim();
        if (value.length() < 2 || value.length() > 12) return "";
        if (isGenericPlace(value)) return "";
        return value;
    }

    private static boolean isGenericPlace(String value) {
        return "今天".equals(value) ||
                "现在".equals(value) ||
                "今晚".equals(value) ||
                "当地".equals(value) ||
                "这里".equals(value) ||
                "这边".equals(value) ||
                "我这里".equals(value) ||
                "我这边".equals(value);
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
