package com.linkyun.her;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class VoiceSilenceTextModeDecisionTest {
    @Test
    public void monitorsInitializedVoiceSurfaceWhenReadyOrListening() {
        assertTrue(VoiceSilenceTextModeDecision.shouldMonitor(
                true, true, false, false, false, false, false, false,
                VoiceSessionState.fromLegacy("ready")));
        assertTrue(VoiceSilenceTextModeDecision.shouldMonitor(
                true, true, false, false, false, false, false, false,
                VoiceSessionState.fromLegacy("listening")));
    }

    @Test
    public void doesNotMonitorWhenNotInVoiceConversationIdleState() {
        assertFalse(VoiceSilenceTextModeDecision.shouldMonitor(
                false, true, false, false, false, false, false, false,
                VoiceSessionState.fromLegacy("ready")));
        assertFalse(VoiceSilenceTextModeDecision.shouldMonitor(
                true, false, false, false, false, false, false, false,
                VoiceSessionState.fromLegacy("ready")));
        assertFalse(VoiceSilenceTextModeDecision.shouldMonitor(
                true, true, true, false, false, false, false, false,
                VoiceSessionState.fromLegacy("ready")));
        assertFalse(VoiceSilenceTextModeDecision.shouldMonitor(
                true, true, false, false, false, false, false, false,
                VoiceSessionState.fromLegacy("processing")));
        assertFalse(VoiceSilenceTextModeDecision.shouldMonitor(
                true, true, false, false, false, false, false, false,
                VoiceSessionState.fromLegacy("speaking")));
    }

    @Test
    public void pausesDuringToolsAndPlayback() {
        assertFalse(VoiceSilenceTextModeDecision.shouldMonitor(
                true, true, false, false, true, false, false, false,
                VoiceSessionState.fromLegacy("listening")));
        assertFalse(VoiceSilenceTextModeDecision.shouldMonitor(
                true, true, false, false, false, true, false, false,
                VoiceSessionState.fromLegacy("listening")));
        assertFalse(VoiceSilenceTextModeDecision.shouldMonitor(
                true, true, false, false, false, false, true, false,
                VoiceSessionState.fromLegacy("listening")));
        assertFalse(VoiceSilenceTextModeDecision.shouldMonitor(
                true, true, false, false, false, false, false, true,
                VoiceSessionState.fromLegacy("listening")));
    }
}
