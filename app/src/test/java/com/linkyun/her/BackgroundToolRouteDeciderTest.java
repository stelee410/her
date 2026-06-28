package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public final class BackgroundToolRouteDeciderTest {
    @Test
    public void requestBodyContainsToolRoutingPromptAndUserText() throws Exception {
        JSONObject body = BackgroundToolRouteDecider.requestBody("c-her", "Luna", "看看今天新闻");

        assertEquals("c-her", body.getString("model"));
        assertEquals(0.0, body.getDouble("temperature"), 0.0);
        assertFalse(body.getBoolean("stream"));

        JSONArray messages = body.getJSONArray("messages");
        assertEquals(2, messages.length());
        assertEquals("system", messages.getJSONObject(0).getString("role"));
        assertTrue(messages.getJSONObject(0).getString("content").contains("你是 Luna 的后台意识模型"));
        assertTrue(messages.getJSONObject(0).getString("content").contains("daily_news"));
        assertTrue(messages.getJSONObject(0).getString("content").contains("weather"));
        assertTrue(messages.getJSONObject(0).getString("content").contains("open_tv"));
        assertTrue(messages.getJSONObject(0).getString("content").contains("财经新闻"));
        assertTrue(messages.getJSONObject(0).getString("content").contains("volume_up"));
        assertTrue(messages.getJSONObject(0).getString("content").contains("只输出 JSON，不要解释"));
        assertEquals("user", messages.getJSONObject(1).getString("role"));
        assertEquals("看看今天新闻", messages.getJSONObject(1).getString("content"));
    }

    @Test
    public void emptyAgentNameFallsBackInPrompt() throws Exception {
        JSONObject body = BackgroundToolRouteDecider.requestBody("c-her", "", "看看新闻");

        String systemPrompt = body.getJSONArray("messages").getJSONObject(0).getString("content");
        assertTrue(systemPrompt.contains("你是 Doris 的后台意识模型"));
    }

    @Test
    public void parseAcceptsJsonInsideModelPreamble() throws Exception {
        BackgroundToolRouteDecider.Decision decision = BackgroundToolRouteDecider.parse(
                "好的 {\"tool\":\"daily_news\",\"confidence\":0.78,\"reason\":\"用户想听新闻\"} 完成");

        assertEquals("daily_news", decision.tool);
        assertEquals(0.78, decision.confidence, 0.0001);
        assertEquals("用户想听新闻", decision.reason);
    }

    @Test
    public void parseDefaultsMissingFieldsToNoRoute() throws Exception {
        BackgroundToolRouteDecider.Decision decision = BackgroundToolRouteDecider.parse("{}");

        assertEquals("none", decision.tool);
        assertEquals(0.0, decision.confidence, 0.0);
        assertEquals("", decision.reason);
    }
}
