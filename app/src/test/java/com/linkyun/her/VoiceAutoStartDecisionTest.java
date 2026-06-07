package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class VoiceAutoStartDecisionTest {
    @Test
    public void voiceSurfaceStartsOnlyWhenReadyAndHeadsetBound() {
        assertTrue(VoiceAutoStartDecision.shouldStartOnVoiceSurface(
                true, false, false, false, false, false, false, true));
    }

    @Test
    public void voiceSurfaceDoesNotStartDuringInitializationOrSummary() {
        assertFalse(VoiceAutoStartDecision.shouldStartOnVoiceSurface(
                false, false, false, false, false, false, false, true));
        assertFalse(VoiceAutoStartDecision.shouldStartOnVoiceSurface(
                true, true, false, false, false, false, false, true));
        assertFalse(VoiceAutoStartDecision.shouldStartOnVoiceSurface(
                true, false, true, false, false, false, false, true));
    }

    @Test
    public void voiceSurfaceDoesNotOverlapInputOrToolSpeech() {
        assertFalse(VoiceAutoStartDecision.shouldStartOnVoiceSurface(
                true, false, false, true, false, false, false, true));
        assertFalse(VoiceAutoStartDecision.shouldStartOnVoiceSurface(
                true, false, false, false, true, false, false, true));
        assertFalse(VoiceAutoStartDecision.shouldStartOnVoiceSurface(
                true, false, false, false, false, true, false, true));
        assertFalse(VoiceAutoStartDecision.shouldStartOnVoiceSurface(
                true, false, false, false, false, false, true, true));
        assertFalse(VoiceAutoStartDecision.shouldStartOnVoiceSurface(
                true, false, false, false, false, false, false, false));
    }

    @Test
    public void openingStartsListeningOnlyWhenStillInitializingAndHeadsetBound() {
        assertEquals(VoiceAutoStartDecision.OpeningAction.START_LISTENING,
                VoiceAutoStartDecision.afterInitializationOpening(true, false, true));
        assertEquals(VoiceAutoStartDecision.OpeningAction.PROMPT_HEADSET,
                VoiceAutoStartDecision.afterInitializationOpening(true, false, false));
        assertEquals(VoiceAutoStartDecision.OpeningAction.SKIP,
                VoiceAutoStartDecision.afterInitializationOpening(false, false, true));
        assertEquals(VoiceAutoStartDecision.OpeningAction.SKIP,
                VoiceAutoStartDecision.afterInitializationOpening(true, true, true));
    }
}
