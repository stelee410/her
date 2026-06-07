package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class InitializationOpeningTest {
    @Test
    public void deliversOnlyWhileInitializingAndNotAlreadyDelivered() {
        assertTrue(InitializationOpening.shouldDeliver(true, false));
        assertFalse(InitializationOpening.shouldDeliver(false, false));
        assertFalse(InitializationOpening.shouldDeliver(true, true));
    }

    @Test
    public void ttsOpeningDoesNotDeliverInTextMode() {
        assertTrue(InitializationOpening.shouldDeliverTts(true, false, false));
        assertFalse(InitializationOpening.shouldDeliverTts(true, false, true));
        assertFalse(InitializationOpening.shouldDeliverTts(false, false, false));
        assertFalse(InitializationOpening.shouldDeliverTts(true, true, false));
    }

    @Test
    public void realtimeDeltaMarksOpeningOnlyOnceDuringInitialization() {
        assertTrue(InitializationOpening.shouldMarkRealtimeDelivered(true, false));
        assertFalse(InitializationOpening.shouldMarkRealtimeDelivered(false, false));
        assertFalse(InitializationOpening.shouldMarkRealtimeDelivered(true, true));
    }

    @Test
    public void cleansAgentNameAndBuildsOpeningText() {
        assertEquals("Mira", InitializationOpening.cleanAgentName(" Mira "));
        assertEquals("", InitializationOpening.cleanAgentName(null));
        assertEquals("嗨，我是 Mira。我们先从你开始吧：你叫什么名字，平时希望我怎么称呼你？",
                InitializationOpening.openingText(" Mira "));
    }

    @Test
    public void subtitleIsAddedOnlyOnceWhenOpeningIsVisible() {
        assertTrue(InitializationOpening.shouldAddSubtitle(true, "hello", false));
        assertFalse(InitializationOpening.shouldAddSubtitle(false, "hello", false));
        assertFalse(InitializationOpening.shouldAddSubtitle(true, "   ", false));
        assertFalse(InitializationOpening.shouldAddSubtitle(true, "hello", true));
    }

    @Test
    public void ttsCallbacksAreIgnoredOutsideActiveVoiceInitialization() {
        assertTrue(InitializationOpening.shouldHandleTtsCallback(true, false, true));
        assertFalse(InitializationOpening.shouldHandleTtsCallback(true, true, true));
        assertFalse(InitializationOpening.shouldHandleTtsCallback(false, false, true));
        assertFalse(InitializationOpening.shouldHandleTtsCallback(true, false, false));
    }
}
