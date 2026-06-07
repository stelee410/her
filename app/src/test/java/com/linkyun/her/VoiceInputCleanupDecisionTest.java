package com.linkyun.her;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class VoiceInputCleanupDecisionTest {
    @Test
    public void clearsVoiceInputAndCancelsAsrTimeoutWhenVoiceInputExists() {
        VoiceInputCleanupDecision decision = VoiceInputCleanupDecision.clearRequests(true);

        assertTrue(decision.cancelAsrFinalTimeout);
        assertTrue(decision.clearPendingStart);
        assertTrue(decision.cancelContinuousListening);
    }

    @Test
    public void stillCancelsAsrTimeoutWithoutVoiceInputCoordinator() {
        VoiceInputCleanupDecision decision = VoiceInputCleanupDecision.clearRequests(false);

        assertTrue(decision.cancelAsrFinalTimeout);
        assertFalse(decision.clearPendingStart);
        assertFalse(decision.cancelContinuousListening);
    }
}
