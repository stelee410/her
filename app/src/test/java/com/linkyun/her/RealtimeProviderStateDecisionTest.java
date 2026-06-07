package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class RealtimeProviderStateDecisionTest {
    @Test
    public void thinkingOnlyLogsAndDoesNotChangePlaybackOrState() {
        RealtimeProviderStateDecision decision =
                RealtimeProviderStateDecision.decide("thinking", true, true, true, false, true);

        assertTrue(decision.logThinking);
        assertFalse(decision.outputStarted);
        assertNull(decision.nextState);
    }

    @Test
    public void activeToolTtsSuppressesProviderStateChanges() {
        RealtimeProviderStateDecision streaming =
                RealtimeProviderStateDecision.decide("tts_streaming", true, false, false, false, true);
        RealtimeProviderStateDecision idle =
                RealtimeProviderStateDecision.decide("idle", true, false, false, false, true);

        assertFalse(streaming.outputStarted);
        assertNull(streaming.nextState);
        assertNull(idle.nextState);
    }

    @Test
    public void gatewayTtsSuppressesProviderStateChanges() {
        RealtimeProviderStateDecision decision =
                RealtimeProviderStateDecision.decide("listening", false, true, true, false, true);

        assertFalse(decision.outputStarted);
        assertNull(decision.nextState);
    }

    @Test
    public void ttsStreamingStartsRealtimeOutputWithoutChangingState() {
        RealtimeProviderStateDecision decision =
                RealtimeProviderStateDecision.decide("tts_streaming", false, false, false, false, true);

        assertTrue(decision.outputStarted);
        assertNull(decision.nextState);
    }

    @Test
    public void textModeSuppressesProviderAudioAndKeepsTextOnly() {
        RealtimeProviderStateDecision streaming =
                RealtimeProviderStateDecision.decide("tts_streaming", false, false, false, true, false);
        RealtimeProviderStateDecision idle =
                RealtimeProviderStateDecision.decide("idle", false, false, false, true, false);
        RealtimeProviderStateDecision listening =
                RealtimeProviderStateDecision.decide("listening", false, false, true, true, false);

        assertFalse(streaming.outputStarted);
        assertEquals("text_only", streaming.nextState);
        assertEquals("text_only", idle.nextState);
        assertEquals("text_only", listening.nextState);
    }

    @Test
    public void inactiveVoiceSurfaceSuppressesProviderAudioAndStateChanges() {
        RealtimeProviderStateDecision streaming =
                RealtimeProviderStateDecision.decide("tts_streaming", false, false, false, false, false);
        RealtimeProviderStateDecision idle =
                RealtimeProviderStateDecision.decide("idle", false, false, false, false, false);
        RealtimeProviderStateDecision listening =
                RealtimeProviderStateDecision.decide("listening", false, false, true, false, false);

        assertFalse(streaming.outputStarted);
        assertNull(streaming.nextState);
        assertNull(idle.nextState);
        assertNull(listening.nextState);
    }

    @Test
    public void idleAndListeningReturnInputAwareState() {
        assertEquals("listening", RealtimeProviderStateDecision
                .decide("idle", false, false, true, false, true).nextState);
        assertEquals("ready", RealtimeProviderStateDecision
                .decide("idle", false, false, false, false, true).nextState);
        assertEquals("listening", RealtimeProviderStateDecision
                .decide("listening", false, false, true, false, true).nextState);
        assertEquals("ready", RealtimeProviderStateDecision
                .decide("listening", false, false, false, false, true).nextState);
    }

    @Test
    public void unknownOrMissingStateDoesNothing() {
        assertNull(RealtimeProviderStateDecision.decide("ready", false, false, false, false, true).nextState);
        assertNull(RealtimeProviderStateDecision.decide(null, false, false, false, false, true).nextState);
    }
}
