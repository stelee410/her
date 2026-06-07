package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public final class MemoryCompactorTest {
    @Test
    public void requestBodyIncludesModelPromptsAndTranscript() throws Exception {
        MemoryChunk chunk = new MemoryChunk(1, 2, "user: hi\nassistant: hello\n");

        JSONObject body = MemoryCompactor.requestBody("c-her", "Luna", "userMemory", "conversation", chunk);

        assertEquals("c-her", body.getString("model"));
        assertFalse(body.getBoolean("stream"));
        JSONArray messages = body.getJSONArray("messages");
        assertEquals(2, messages.length());
        assertTrue(messages.getJSONObject(0).getString("content").contains("memory_md"));
        String user = messages.getJSONObject(1).getString("content");
        assertTrue(user.contains("Agent 名字：Luna"));
        assertTrue(user.contains("user: hi"));
    }

    @Test
    public void requestBodyTrimsLongMemoryFromTail() throws Exception {
        String longMemory = repeat("x", 1300) + "TAIL";

        JSONObject body = MemoryCompactor.requestBody("c-her", "Luna", longMemory, "", new MemoryChunk(1, 1, ""));

        String user = body.getJSONArray("messages").getJSONObject(1).getString("content");
        assertTrue(user.contains("TAIL"));
        assertFalse(user.contains(repeat("x", 1200)));
    }

    @Test
    public void parseResultAcceptsJsonWrappedInText() {
        MemoryCompactor.Result result = MemoryCompactor.parseResult(
                "好的 {\"memory_md\":\"记住用户喜欢安静\",\"tone_guidance\":\"更轻柔\"} 完成");

        assertEquals("记住用户喜欢安静", result.memory);
        assertEquals("更轻柔", result.tone);
    }

    @Test
    public void parseResultFallsBackToRawContentWhenNotJson() {
        MemoryCompactor.Result result = MemoryCompactor.parseResult("普通摘要");

        assertEquals("普通摘要", result.memory);
        assertEquals("", result.tone);
    }

    private static String repeat(String value, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }
}
