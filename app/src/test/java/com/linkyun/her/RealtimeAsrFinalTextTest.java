package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class RealtimeAsrFinalTextTest {
    @Test
    public void hiddenSystemEventsAreAlwaysSuppressed() {
        RealtimeAsrFinalText.Result chinese =
                RealtimeAsrFinalText.classify("  【系统事件】用户刚打开应用  ", false);
        RealtimeAsrFinalText.Result bracket =
                RealtimeAsrFinalText.classify("[系统事件] wake", true);

        assertTrue(chinese.hidden);
        assertTrue(chinese.resetIgnoreNextInitTrigger);
        assertEquals("【系统事件】用户刚打开应用", chinese.text);
        assertTrue(bracket.hidden);
        assertTrue(bracket.resetIgnoreNextInitTrigger);
    }

    @Test
    public void initializationTriggersAreSuppressedOnlyDuringInitialization() {
        RealtimeAsrFinalText.Result initializing =
                RealtimeAsrFinalText.classify("Agent 主动问候", true);
        RealtimeAsrFinalText.Result normal =
                RealtimeAsrFinalText.classify("Agent 主动问候", false);

        assertTrue(initializing.hidden);
        assertTrue(initializing.resetIgnoreNextInitTrigger);
        assertFalse(normal.hidden);
        assertFalse(normal.resetIgnoreNextInitTrigger);
    }

    @Test
    public void normalTextIsTrimmedAndKeptVisible() {
        RealtimeAsrFinalText.Result result =
                RealtimeAsrFinalText.classify("  今天天气怎么样  ", true);

        assertEquals("今天天气怎么样", result.text);
        assertFalse(result.hidden);
        assertFalse(result.resetIgnoreNextInitTrigger);
    }

    @Test
    public void nullTextBecomesEmptyVisibleText() {
        RealtimeAsrFinalText.Result result = RealtimeAsrFinalText.classify(null, false);

        assertEquals("", result.text);
        assertFalse(result.hidden);
    }
}
