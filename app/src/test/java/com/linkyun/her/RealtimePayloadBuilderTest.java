package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

public final class RealtimePayloadBuilderTest {
    @Test
    public void sessionPayloadContainsRealtimeAudioVoiceAndClientMetadata() throws Exception {
        JSONObject payload = RealtimePayloadBuilder.sessionPayload("instructions", "voice-a");
        JSONObject audio = payload.getJSONObject("audio");
        JSONObject client = payload.getJSONObject("client");

        assertEquals("omnia_default", payload.getString("agent_id"));
        assertEquals("realtime", payload.getString("mode"));
        assertEquals("realtime_doubao", payload.getString("model_profile"));
        assertEquals("instructions", payload.getString("instructions"));
        assertEquals("voice-a", payload.getString("voice"));
        assertEquals("pcm16", audio.getString("input_format"));
        assertEquals("pcm16", audio.getString("output_format"));
        assertEquals(16000, audio.getInt("sample_rate"));
        assertEquals(1, audio.getInt("channels"));
        assertEquals(20, audio.getInt("frame_duration_ms"));
        assertEquals("android", client.getString("type"));
        assertEquals("1.0.0", client.getString("version"));
    }

    @Test
    public void contextUpdateIncludesTrimmedFactOnlyWhenRequested() throws Exception {
        JSONObject payload = RealtimePayloadBuilder.contextUpdate("instructions", "abcdef", true, 3);

        assertEquals("instructions", payload.getString("instructions"));
        assertEquals("def", payload.getString("fact"));

        JSONObject noFact = RealtimePayloadBuilder.contextUpdate("instructions", "abcdef", false, 3);
        assertFalse(noFact.has("fact"));

        JSONObject blankFact = RealtimePayloadBuilder.contextUpdate("instructions", "   ", true, 3);
        assertFalse(blankFact.has("fact"));
    }

    @Test
    public void factContextUpdateReturnsTrimmedPayloadOnlyForNonBlankFact() throws Exception {
        JSONObject payload = RealtimePayloadBuilder.factContextUpdate("abcdef", 4);

        assertEquals("cdef", payload.getString("fact"));
        assertEquals(null, RealtimePayloadBuilder.factContextUpdate("   ", 4));
        assertEquals(null, RealtimePayloadBuilder.factContextUpdate(null, 4));
    }

    @Test
    public void trimTailHandlesNullAndLimit() {
        assertEquals("", RealtimePayloadBuilder.trimTail(null, 10));
        assertEquals("", RealtimePayloadBuilder.trimTail("abc", 0));
        assertEquals("abc", RealtimePayloadBuilder.trimTail("abc", 10));
        assertEquals("bc", RealtimePayloadBuilder.trimTail("abc", 2));
    }

    @Test
    public void sessionPayloadCoercesNullStrings() throws Exception {
        JSONObject payload = RealtimePayloadBuilder.sessionPayload(null, null);

        assertTrue(payload.getString("instructions").isEmpty());
        assertTrue(payload.getString("voice").isEmpty());
    }
}
