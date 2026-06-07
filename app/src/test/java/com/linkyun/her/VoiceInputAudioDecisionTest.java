package com.linkyun.her;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class VoiceInputAudioDecisionTest {
    @Test
    public void canStartOnlyWhenVoiceModeIdleAndPermissionGranted() {
        assertTrue(VoiceInputAudioDecision.canStart(false, true, false, false, false, true));

        assertFalse(VoiceInputAudioDecision.canStart(true, true, false, false, false, true));
        assertFalse(VoiceInputAudioDecision.canStart(false, false, false, false, false, true));
        assertFalse(VoiceInputAudioDecision.canStart(false, true, true, false, false, true));
        assertFalse(VoiceInputAudioDecision.canStart(false, true, false, true, false, true));
        assertFalse(VoiceInputAudioDecision.canStart(false, true, false, false, true, true));
        assertFalse(VoiceInputAudioDecision.canStart(false, true, false, false, false, false));
    }

    @Test
    public void stopSendsInputEndOnlyWhenInputAudioWasOpen() {
        VoiceInputAudioDecision.StopDecision open =
                VoiceInputAudioDecision.stop(true, "ready");
        VoiceInputAudioDecision.StopDecision closed =
                VoiceInputAudioDecision.stop(false, "ready");

        assertTrue(open.sendInputEnd);
        assertFalse(closed.sendInputEnd);
    }

    @Test
    public void stopSchedulesAsrTimeoutOnlyWhenOpenInputEndsIntoProcessing() {
        assertTrue(VoiceInputAudioDecision.stop(true, "processing").scheduleAsrFinalTimeout);

        assertFalse(VoiceInputAudioDecision.stop(false, "processing").scheduleAsrFinalTimeout);
        assertFalse(VoiceInputAudioDecision.stop(true, "thinking").scheduleAsrFinalTimeout);
        assertFalse(VoiceInputAudioDecision.stop(true, "ready").scheduleAsrFinalTimeout);
        assertFalse(VoiceInputAudioDecision.stop(true, "speaking").scheduleAsrFinalTimeout);
    }
}
