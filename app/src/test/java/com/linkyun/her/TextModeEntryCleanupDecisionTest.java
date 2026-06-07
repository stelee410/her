package com.linkyun.her;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class TextModeEntryCleanupDecisionTest {
    @Test
    public void enteringTextModeStopsVoicePlaybackAndPendingVoiceWork() {
        TextModeEntryCleanupDecision decision = TextModeEntryCleanupDecision.enterTextMode();

        assertTrue(decision.resetRealtimeRetries);
        assertTrue(decision.stopToolTtsPlayback);
        assertTrue(decision.stopRealtimeAudio);
        assertTrue(decision.resetRealtimeOutput);
        assertTrue(decision.clearVoiceInputRequests);
        assertTrue(decision.clearPendingBroadcasts);
    }
}
