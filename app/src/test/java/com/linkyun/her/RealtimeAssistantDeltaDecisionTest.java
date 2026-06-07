package com.linkyun.her;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class RealtimeAssistantDeltaDecisionTest {
    @Test
    public void voiceModeAppendsRealtimeAssistantDelta() {
        RealtimeAssistantDeltaDecision decision =
                RealtimeAssistantDeltaDecision.decide(false, false);

        assertTrue(decision.appendDelta);
        assertFalse(decision.discardActiveDraft);
        assertFalse(decision.refreshAfterDiscard);
    }

    @Test
    public void textModeDropsDeltaWithoutRefreshWhenThereIsNoDraft() {
        RealtimeAssistantDeltaDecision decision =
                RealtimeAssistantDeltaDecision.decide(true, false);

        assertFalse(decision.appendDelta);
        assertFalse(decision.discardActiveDraft);
        assertFalse(decision.refreshAfterDiscard);
    }

    @Test
    public void textModeDropsDeltaAndRefreshesAfterDiscardingActiveDraft() {
        RealtimeAssistantDeltaDecision decision =
                RealtimeAssistantDeltaDecision.decide(true, true);

        assertFalse(decision.appendDelta);
        assertTrue(decision.discardActiveDraft);
        assertTrue(decision.refreshAfterDiscard);
    }
}
