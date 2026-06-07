package com.linkyun.her;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class VoiceRuntimeShutdownCleanupDecisionTest {
    @Test
    public void shutdownCancelsAllVoiceRuntimeWorkBeforeRelease() {
        VoiceRuntimeShutdownCleanupDecision decision =
                VoiceRuntimeShutdownCleanupDecision.shutdown();

        assertTrue(decision.cancelVoiceInputRequests);
        assertTrue(decision.clearToolInteractions);
        assertTrue(decision.invalidateBackgroundToolRoute);
        assertTrue(decision.cancelMemoryCompaction);
        assertTrue(decision.cancelVoiceCardTimeouts);
        assertTrue(decision.stopToolTtsPlayback);
    }
}
