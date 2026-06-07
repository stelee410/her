package com.linkyun.her;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class RealtimeConnectingDecisionTest {
    @Test
    public void voiceModeShowsConnecting() {
        assertEquals("connecting", RealtimeConnectingDecision.nextState(false));
    }

    @Test
    public void textModePreservesTextOnly() {
        assertEquals("text_only", RealtimeConnectingDecision.nextState(true));
    }
}
