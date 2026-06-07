package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class VoiceSurfaceExitCleanupDecisionTest {
    @Test
    public void leavingVoiceSurfaceCancelsVoiceWorkAndClearsViews() {
        VoiceSurfaceExitCleanupDecision decision =
                VoiceSurfaceExitCleanupDecision.leaveVoiceSurface(false);

        assertTrue(decision.cancelVoiceInputRequests);
        assertFalse(decision.stopActiveInput);
        assertEquals("ready", decision.nextInputState);
        assertTrue(decision.stopToolTtsPlayback);
        assertTrue(decision.stopRealtimeAudio);
        assertTrue(decision.resetRealtimeOutput);
        assertTrue(decision.clearVoiceSurfaceViews);
    }

    @Test
    public void leavingVoiceSurfaceStopsActiveInputAsReady() {
        VoiceSurfaceExitCleanupDecision decision =
                VoiceSurfaceExitCleanupDecision.leaveVoiceSurface(true);

        assertTrue(decision.stopActiveInput);
        assertEquals("ready", decision.nextInputState);
    }
}
