package com.linkyun.her;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class RealtimeAudioPlaybackDecisionTest {
    @Test
    public void voiceModePlaysWhenAudioIsNotDiscarded() {
        assertTrue(RealtimeAudioPlaybackDecision.shouldPlay(false, true, false));
    }

    @Test
    public void textModeSuppressesRealtimeAudio() {
        assertFalse(RealtimeAudioPlaybackDecision.shouldPlay(true, true, false));
    }

    @Test
    public void inactiveVoiceSurfaceSuppressesRealtimeAudio() {
        assertFalse(RealtimeAudioPlaybackDecision.shouldPlay(false, false, false));
    }

    @Test
    public void discardStateSuppressesRealtimeAudio() {
        assertFalse(RealtimeAudioPlaybackDecision.shouldPlay(false, true, true));
    }
}
