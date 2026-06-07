package com.linkyun.her;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class AsrFinalTimeoutDecisionTest {
    @Test
    public void ignoresTimeoutWhenNotProcessing() {
        assertNull(AsrFinalTimeoutDecision.decide(false, false, false, false, false, false));
    }

    @Test
    public void ignoresTimeoutWhileAudioSummaryOrTextModeIsActive() {
        assertNull(AsrFinalTimeoutDecision.decide(true, true, false, false, false, false));
        assertNull(AsrFinalTimeoutDecision.decide(true, false, true, false, false, false));
        assertNull(AsrFinalTimeoutDecision.decide(true, false, false, true, false, false));
        assertNull(AsrFinalTimeoutDecision.decide(true, false, false, false, true, false));
        assertNull(AsrFinalTimeoutDecision.decide(true, false, false, false, false, true));
    }

    @Test
    public void processingTimeoutResumesListeningWhenNothingIsActive() {
        AsrFinalTimeoutDecision decision =
                AsrFinalTimeoutDecision.decide(true, false, false, false, false, false);

        assertNotNull(decision);
        assertTrue(decision.resumeListening);
    }
}
