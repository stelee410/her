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
    public void skipsUsageOnlyStreamingChunks() throws Exception {
        String sse = ""
                + "data: {\"choices\":[{\"delta\":{\"content\":\"好\"}}]}\n\n"
                + "data: {\"choices\":[],\"usage\":{\"prompt_tokens\":1}}\n\n"
                + "data: [DONE]\n";

        assertEquals("好", AgentApiClient.extractStreamingAssistantContent(sse));
    }

    @Test
    public void skipsNullStreamingContent() throws Exception {
        String sse = ""
                + "data: {\"choices\":[{\"delta\":{\"content\":null}}]}\n\n"
                + "data: {\"choices\":[{\"delta\":{\"content\":\"云\"}}]}\n\n"
                + "data: {\"choices\":[{\"message\":{\"content\":null}}]}\n\n"
                + "data: [DONE]\n";

        assertEquals("云", AgentApiClient.extractStreamingAssistantContent(sse));
    }

    @Test
    public void autoDetectsStreamingOrPlainJsonResponses() throws Exception {
        String json = "{\"choices\":[{\"message\":{\"content\":\"普通回复\"}}]}";
        String sse = "data: {\"choices\":[{\"delta\":{\"content\":\"流式\"}}]}\n";

        assertEquals("普通回复", AgentApiClient.extractAssistantContentAny(json));
        assertEquals("流式", AgentApiClient.extractAssistantContentAny(sse));
    }
}
