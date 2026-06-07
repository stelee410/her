package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class RealtimeOutputStartDecisionTest {
    @Test
    public void voiceModeStartsRealtimeOutput() {
        RealtimeOutputStartDecision decision = RealtimeOutputStartDecision.decide(false);

        assertTrue(decision.startRealtimeOutput);
        assertNull(decision.nextState);
    }

    @Test
    public void textModeKeepsTextOnlyAndSkipsRealtimeOutput() {
        RealtimeOutputStartDecision decision = RealtimeOutputStartDecision.decide(true);

        assertFalse(decision.startRealtimeOutput);
        assertEquals("text_only", decision.nextState);
    }
}
