package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ResponsePendingTimeoutDecisionTest {
    @Test
    public void ignoresTimeoutWhenStateNoLongerResponsePending() {
        assertNull(ResponsePendingTimeoutDecision.decide(false, false, false, false, false, true));
    }

    @Test
    public void ignoresTimeoutWhileInputOrSummaryIsActive() {
        assertNull(ResponsePendingTimeoutDecision.decide(true, true, false, false, false, true));
        assertNull(ResponsePendingTimeoutDecision.decide(true, false, true, false, false, true));
        assertNull(ResponsePendingTimeoutDecision.decide(true, false, false, true, false, true));
    }

    @Test
    public void textModeReturnsTextOnlyWithoutListeningSchedule() {
        ResponsePendingTimeoutDecision decision =
                ResponsePendingTimeoutDecision.decide(true, false, false, false, true, true);

        assertEquals("text_only", decision.nextState);
        assertFalse(decision.scheduleContinuousListening);
    }

    @Test
    public void openRealtimeReturnsReadyAndSchedulesListening() {
        ResponsePendingTimeoutDecision decision =
                ResponsePendingTimeoutDecision.decide(true, false, false, false, false, true);

        assertEquals("ready", decision.nextState);
        assertTrue(decision.scheduleContinuousListening);
    }

    @Test
    public void closedRealtimeFallsBackToIdleWithoutListeningSchedule() {
        ResponsePendingTimeoutDecision decision =
                ResponsePendingTimeoutDecision.decide(true, false, false, false, false, false);

        assertEquals("idle", decision.nextState);
        assertFalse(decision.scheduleContinuousListening);
    }
}
