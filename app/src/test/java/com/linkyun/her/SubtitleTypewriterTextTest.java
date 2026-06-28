package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class SubtitleTypewriterTextTest {
    @Test
    public void shortTextDoesNotNeedTypewriter() {
        assertFalse(SubtitleTypewriterText.needsTypewriter("我在这里。", 18));
        assertEquals("我在这里。", SubtitleTypewriterText.frame("  我在这里。  ", 10, 18));
    }

    @Test
    public void longTextUsesSingleLineWindow() {
        String text = "我在这里。今天想从哪里开始？我们慢慢聊就好。";

        assertTrue(SubtitleTypewriterText.needsTypewriter(text, 18));
        assertEquals("我", SubtitleTypewriterText.frame(text, 0, 18));
        assertEquals("我在这里。今天想从哪里开始？我们慢慢", SubtitleTypewriterText.frame(text, 17, 18));
        assertEquals("在这里。今天想从哪里开始？我们慢慢聊", SubtitleTypewriterText.frame(text, 18, 18));
    }

    @Test
    public void pausesOnTailBeforeRestarting() {
        String text = "abcdefghijklmnopqrst";

        assertEquals("klmnopqrst", SubtitleTypewriterText.frame(text, 19, 10));
        assertEquals("klmnopqrst", SubtitleTypewriterText.frame(text, 20, 10));
        assertEquals("a", SubtitleTypewriterText.frame(text, 30, 10));
    }
}
