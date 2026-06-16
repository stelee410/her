package com.linkyun.her;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class TvVoiceCommandTest {
    @Test
    public void matchesChineseTvCommands() {
        assertTrue(TvVoiceCommand.shouldOpen("我要看电视"));
        assertTrue(TvVoiceCommand.shouldOpen("打开电视！"));
        assertTrue(TvVoiceCommand.shouldOpen("我想看电视。"));
    }

    @Test
    public void ignoresUnrelatedText() {
        assertFalse(TvVoiceCommand.shouldOpen("我要看天气"));
        assertFalse(TvVoiceCommand.shouldOpen(""));
        assertFalse(TvVoiceCommand.shouldOpen(null));
    }
}
