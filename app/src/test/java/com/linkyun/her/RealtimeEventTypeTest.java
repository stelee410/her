package com.linkyun.her;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class RealtimeEventTypeTest {
    @Test
    public void classifiesEventsThatDoNotNeedPayload() {
        assertEquals(RealtimeEventType.Kind.SESSION_CREATED,
                RealtimeEventType.classify("session.created", false));
        assertEquals(RealtimeEventType.Kind.OUTPUT_AUDIO_DONE,
                RealtimeEventType.classify("output_audio.done", false));
        assertEquals(RealtimeEventType.Kind.OUTPUT_AUDIO_STOP,
                RealtimeEventType.classify("output_audio.stop", false));
    }

    @Test
    public void classifiesPayloadEventsWhenPayloadExists() {
        assertEquals(RealtimeEventType.Kind.ASR_FINAL,
                RealtimeEventType.classify("asr.final", true));
        assertEquals(RealtimeEventType.Kind.ASSISTANT_STATE,
                RealtimeEventType.classify("assistant.state", true));
        assertEquals(RealtimeEventType.Kind.ASSISTANT_TEXT_DELTA,
                RealtimeEventType.classify("assistant.text.delta", true));
        assertEquals(RealtimeEventType.Kind.OUTPUT_AUDIO_START,
                RealtimeEventType.classify("output_audio.start", true));
        assertEquals(RealtimeEventType.Kind.MEMORY_SNAPSHOT,
                RealtimeEventType.classify("memory.snapshot", true));
        assertEquals(RealtimeEventType.Kind.ERROR,
                RealtimeEventType.classify("error", true));
    }

    @Test
    public void ignoresPayloadEventsWhenPayloadIsMissing() {
        assertEquals(RealtimeEventType.Kind.IGNORE,
                RealtimeEventType.classify("asr.final", false));
        assertEquals(RealtimeEventType.Kind.IGNORE,
                RealtimeEventType.classify("assistant.state", false));
        assertEquals(RealtimeEventType.Kind.IGNORE,
                RealtimeEventType.classify("assistant.text.delta", false));
        assertEquals(RealtimeEventType.Kind.IGNORE,
                RealtimeEventType.classify("output_audio.start", false));
        assertEquals(RealtimeEventType.Kind.IGNORE,
                RealtimeEventType.classify("memory.snapshot", false));
        assertEquals(RealtimeEventType.Kind.IGNORE,
                RealtimeEventType.classify("error", false));
    }

    @Test
    public void ignoresUnknownOrNullEvents() {
        assertEquals(RealtimeEventType.Kind.IGNORE,
                RealtimeEventType.classify("unknown", true));
        assertEquals(RealtimeEventType.Kind.IGNORE,
                RealtimeEventType.classify(null, true));
    }
}
