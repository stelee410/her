package com.linkyun.her;

import static org.junit.Assert.assertEquals;

import org.json.JSONObject;
import org.junit.Test;

public final class RealtimeEventPayloadTest {
    @Test
    public void textFieldsDefaultToEmptyString() throws Exception {
        JSONObject payload = new JSONObject();

        assertEquals("", RealtimeEventPayload.asrFinalText(null));
        assertEquals("", RealtimeEventPayload.asrFinalText(payload));
        assertEquals("", RealtimeEventPayload.assistantTextDelta(null));
        assertEquals("", RealtimeEventPayload.assistantTextDelta(payload));
    }

    @Test
    public void textFieldsPreservePayloadText() throws Exception {
        JSONObject payload = new JSONObject().put("text", "  你好  ");

        assertEquals("  你好  ", RealtimeEventPayload.asrFinalText(payload));
        assertEquals("  你好  ", RealtimeEventPayload.assistantTextDelta(payload));
    }

    @Test
    public void assistantStateDefaultsToReady() throws Exception {
        assertEquals("ready", RealtimeEventPayload.assistantState(null));
        assertEquals("ready", RealtimeEventPayload.assistantState(new JSONObject()));
        assertEquals("thinking", RealtimeEventPayload.assistantState(
                new JSONObject().put("state", "thinking")));
    }

    @Test
    public void outputSampleRateDefaultsToRealtimeRate() throws Exception {
        assertEquals(24000, RealtimeEventPayload.outputSampleRate(null));
        assertEquals(24000, RealtimeEventPayload.outputSampleRate(new JSONObject()));
        assertEquals(24000, RealtimeEventPayload.outputSampleRate(
                new JSONObject().put("sample_rate", "bad")));
        assertEquals(16000, RealtimeEventPayload.outputSampleRate(
                new JSONObject().put("sample_rate", 16000)));
    }
}
