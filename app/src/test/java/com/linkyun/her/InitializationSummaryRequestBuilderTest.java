package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.junit.Test;

public final class InitializationSummaryRequestBuilderTest {
    @Test
    public void buildsSubconsciousJsonRequestWithTwoMessages() throws Exception {
        InitializationSummaryRequestBuilder.Request request = InitializationSummaryRequestBuilder.build(
                "c-her", "", "Mira, Nora", "assistant: 你叫什么？\nuser: 我叫史蒂芬\n");

        assertEquals("c-her", request.body.getString("model"));
        assertEquals(0, request.body.getInt("temperature"));
        assertFalse(request.body.getBoolean("stream"));

        JSONArray messages = request.body.getJSONArray("messages");
        assertEquals(2, messages.length());
        assertEquals("system", messages.getJSONObject(0).getString("role"));
        assertEquals(request.systemPrompt, messages.getJSONObject(0).getString("content"));
        assertEquals("user", messages.getJSONObject(1).getString("role"));
        assertEquals(request.userPrompt, messages.getJSONObject(1).getString("content"));
    }

    @Test
    public void promptsPreserveInitializationSafetyRequirements() throws Exception {
        InitializationSummaryRequestBuilder.Request request = InitializationSummaryRequestBuilder.build(
                "c-her", "", "Mira, Nora", "assistant: 你希望和我建立什么关系？\nuser: 女朋友\n");

        assertTrue(request.systemPrompt.contains("只输出 JSON"));
        assertTrue(request.systemPrompt.contains("\"agent_name\""));
        assertTrue(request.systemPrompt.contains("\"user_name\""));
        assertTrue(request.systemPrompt.contains("推荐候选：Mira, Nora"));
        assertTrue(request.systemPrompt.contains("禁止使用豆包、小包、包包、助手、AI、机器人、Doris"));
        assertTrue(request.userPrompt.contains("用户预设的 Agent 名字：未指定，请从初始化对话中自行确定"));
        assertTrue(request.userPrompt.contains("user_name 是用户希望被称呼的名字，必须从用户回答中提取"));
        assertTrue(request.userPrompt.contains("不要把已明确回答的名字、关系写成未明确"));
        assertTrue(request.userPrompt.contains("user: 女朋友"));
    }

    @Test
    public void existingAgentNameIsPassedAsUserPreset() throws Exception {
        InitializationSummaryRequestBuilder.Request request = InitializationSummaryRequestBuilder.build(
                "c-her", "Clara", "Mira, Nora", "user: 叫我小林\n");

        assertTrue(request.userPrompt.contains("用户预设的 Agent 名字：Clara"));
        assertTrue(request.userPrompt.contains("user: 叫我小林"));
    }
}
