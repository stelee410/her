package com.linkyun.her;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class TextModeAsrClientTest {
    @Test
    public void wsUrlConvertsHttpsBaseAndEncodesApiKey() {
        assertEquals(
                "wss://agentllm.linkyun.co/v1beta/dashscope/asr/ws?api_key=sk-a%2Bb",
                TextModeAsrClient.wsUrl("https://agentllm.linkyun.co/", "sk-a+b"));
    }

    @Test
    public void wsUrlConvertsHttpBaseForLocalGateway() {
        assertEquals(
                "ws://localhost:3000/v1beta/dashscope/asr/ws?api_key=sk-test",
                TextModeAsrClient.wsUrl("http://localhost:3000", "sk-test"));
    }
}
