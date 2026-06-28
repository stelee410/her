package com.linkyun.her;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class AgentApiClientStreamingTest {
    @Test
    public void extractsContentFromSseDeltas() throws Exception {
        String sse = ""
                + "data: {\"choices\":[{\"delta\":{\"role\":\"assistant\"}}]}\n\n"
                + "data: {\"choices\":[{\"delta\":{\"content\":\"你\"}}]}\n\n"
                + "data: {\"choices\":[{\"delta\":{\"content\":\"好\"}}]}\n\n"
                + "data: [DONE]\n";

        assertEquals("你好", AgentApiClient.extractStreamingAssistantContent(sse));
    }

    @Test
    public void autoDetectsStreamingOrPlainJsonResponses() throws Exception {
        String json = "{\"choices\":[{\"message\":{\"content\":\"普通回复\"}}]}";
        String sse = "data: {\"choices\":[{\"delta\":{\"content\":\"流式\"}}]}\n";

        assertEquals("普通回复", AgentApiClient.extractAssistantContentAny(json));
        assertEquals("流式", AgentApiClient.extractAssistantContentAny(sse));
    }
}
