package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public final class WeatherIntentResolverTest {
    @Test
    public void parsesExplicitCityFromStructuredLlmOutput() throws Exception {
        WeatherIntentResolver.Result result = WeatherIntentResolver.parse(
                "{\"is_weather_query\":true,\"city\":\"深圳\",\"reason\":\"用户明确说深圳\"}");

        assertTrue(result.isWeatherQuery);
        assertEquals("深圳", result.city);
    }

    @Test
    public void keepsCityEmptyWhenUserDidNotSpecifyLocation() throws Exception {
        WeatherIntentResolver.Result result = WeatherIntentResolver.parse(
                "{\"is_weather_query\":true,\"city\":\"\",\"reason\":\"未指定城市\"}");

        assertTrue(result.isWeatherQuery);
        assertEquals("", result.city);
    }

    @Test
    public void clearsCityWhenLlmSaysItIsNotWeatherQuery() throws Exception {
        WeatherIntentResolver.Result result = WeatherIntentResolver.parse(
                "{\"is_weather_query\":false,\"city\":\"深圳\",\"reason\":\"不是天气问题\"}");

        assertFalse(result.isWeatherQuery);
        assertEquals("", result.city);
    }

    @Test
    public void acceptsAlternateStructuredLocationFields() throws Exception {
        WeatherIntentResolver.Result result = WeatherIntentResolver.parse(
                "{\"weather\":\"true\",\"location\":\"上海\",\"reason\":\"用户问天气\"}");

        assertTrue(result.isWeatherQuery);
        assertEquals("上海", result.city);
    }

    @Test
    public void parsesLooseWeatherResolverText() throws Exception {
        WeatherIntentResolver.Result result = WeatherIntentResolver.parse("是天气查询，城市：深圳");

        assertTrue(result.isWeatherQuery);
        assertEquals("深圳", result.city);
    }

    @Test
    public void localFallbackExtractsCityFromQuestion() {
        WeatherIntentResolver.Result result =
                WeatherIntentResolver.fallbackFromQuestion("能不能帮我查一下深圳的天气");

        assertTrue(result.isWeatherQuery);
        assertEquals("深圳", result.city);
    }

    @Test
    public void localFallbackKeepsCityEmptyForCurrentLocationWeather() {
        WeatherIntentResolver.Result result =
                WeatherIntentResolver.fallbackFromQuestion("今天天气怎么样");

        assertTrue(result.isWeatherQuery);
        assertEquals("", result.city);
    }

    @Test
    public void requestPromptForbidsTreatingPolitePhrasesAsCity() throws Exception {
        JSONObject body = WeatherIntentResolver.requestBody("c-her", "Doris", "能不能帮我查一下深圳的天气");
        JSONArray messages = body.getJSONArray("messages");
        String system = messages.getJSONObject(0).getString("content");

        assertEquals("c-her", body.getString("model"));
        assertEquals("json_object", body.getJSONObject("response_format").getString("type"));
        assertEquals(120, body.getInt("max_tokens"));
        assertTrue(system.contains("不要把请求语气"));
        assertTrue(system.contains("能不能"));
        assertTrue(system.contains("一行紧凑 JSON"));
        assertEquals("能不能帮我查一下深圳的天气", messages.getJSONObject(1).getString("content"));
    }

    @Test
    public void reusesLatestWeatherFactForSameTurnFollowups() {
        assertTrue(WeatherSkill.shouldReuseLatestFact("今天会下雨吗", true));
        assertTrue(WeatherSkill.shouldReuseLatestFact("那要带伞吗", true));
        assertTrue(WeatherSkill.shouldReuseLatestFact("刚才天气查出来了吗", true));
    }

    @Test
    public void doesNotReuseLatestWeatherFactForFreshQueries() {
        assertFalse(WeatherSkill.shouldReuseLatestFact("重新查一下天气", true));
        assertFalse(WeatherSkill.shouldReuseLatestFact("明天天气怎么样", true));
        assertFalse(WeatherSkill.shouldReuseLatestFact("北京天气怎么样", true));
        assertFalse(WeatherSkill.shouldReuseLatestFact("今天会下雨吗", false));
    }
}
