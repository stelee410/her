package com.linkyun.her;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class TextModeAsrGestureTest {
    @Test
    public void neutralUntilCrossingThreshold() {
        assertEquals(TextModeAsrGesture.NEUTRAL, TextModeAsrGesture.decide(0, 60));
        assertEquals(TextModeAsrGesture.NEUTRAL, TextModeAsrGesture.decide(59, 60));
        assertEquals(TextModeAsrGesture.NEUTRAL, TextModeAsrGesture.decide(-59, 60));
    }

    @Test
    public void leftCancelsAndRightSends() {
        assertEquals(TextModeAsrGesture.CANCEL, TextModeAsrGesture.decide(-60, 60));
        assertEquals(TextModeAsrGesture.SEND, TextModeAsrGesture.decide(60, 60));
    }

    @Test
    public void labelsExposeGestureState() {
        assertEquals("← 取消    ▌ ▌ ▌", TextModeAsrGesture.label(TextModeAsrGesture.CANCEL));
        assertEquals("▌ ▌ ▌    发送 →", TextModeAsrGesture.label(TextModeAsrGesture.SEND));
        assertEquals("← 取消    ▌ ▌ ▌    发送 →", TextModeAsrGesture.label(TextModeAsrGesture.NEUTRAL));
    }
}
