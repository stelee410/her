package com.linkyun.her;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class InteractionKeepScreenOnDecisionTest {
    @Test
    public void initializationAndSummaryAlwaysKeepScreenOn() {
        assertTrue(InteractionKeepScreenOnDecision.shouldKeepScreenOn(
                true, false, false, false, false, false, false, VoiceSessionState.fromLegacy("idle")));
        assertTrue(InteractionKeepScreenOnDecision.shouldKeepScreenOn(
                false, true, false, false, false, false, false, VoiceSessionState.fromLegacy("idle")));
    }

    @Test
    public void activeInputOrToolOrToolInteractionKeepsScreenOn() {
        VoiceSessionState idle = VoiceSessionState.fromLegacy("idle");

        assertTrue(InteractionKeepScreenOnDecision.shouldKeepScreenOn(false, false, true, false, false, false, false, idle));
        assertTrue(InteractionKeepScreenOnDecision.shouldKeepScreenOn(false, false, false, true, false, false, false, idle));
        assertTrue(InteractionKeepScreenOnDecision.shouldKeepScreenOn(false, false, false, false, true, false, false, idle));
        assertTrue(InteractionKeepScreenOnDecision.shouldKeepScreenOn(false, false, false, false, false, true, false, idle));
        assertTrue(InteractionKeepScreenOnDecision.shouldKeepScreenOn(false, false, false, false, false, false, true, idle));
    }

    @Test
    public void fallsBackToVoiceStateKeepScreenRule() {
        assertTrue(InteractionKeepScreenOnDecision.shouldKeepScreenOn(
                false, false, false, false, false, false, false, VoiceSessionState.fromLegacy("processing")));
        assertFalse(InteractionKeepScreenOnDecision.shouldKeepScreenOn(
                false, false, false, false, false, false, false, VoiceSessionState.fromLegacy("ready")));
        assertFalse(InteractionKeepScreenOnDecision.shouldKeepScreenOn(
                false, false, false, false, false, false, false, null));
    }
}
