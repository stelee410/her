package com.linkyun.her;

import static org.junit.Assert.assertEquals;
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
    public void matchesFinanceNewsAsTvChannelIntent() {
        assertTrue(TvVoiceCommand.shouldOpen("我想了解财经新闻"));
        assertTrue(TvVoiceCommand.shouldOpen("看财经新闻"));
        assertTrue(TvVoiceCommand.shouldOpen("打开财经频道"));
    }

    @Test
    public void prefersFinanceChannelForFinanceNewsIntent() {
        assertEquals(OnlineTvPlaylist.DEFAULT_FINANCE_CHANNEL_ID,
                TvVoiceCommand.preferredChannelId("我想了解财经新闻"));
        assertTrue(TvVoiceCommand.preferredChannelId("打开电视").isEmpty());
    }

    @Test
    public void ignoresUnrelatedText() {
        assertFalse(TvVoiceCommand.shouldOpen("我要看天气"));
        assertFalse(TvVoiceCommand.shouldOpen(""));
        assertFalse(TvVoiceCommand.shouldOpen(null));
    }
}
