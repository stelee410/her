package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public final class OnlineTvPlaylistTest {
    @Test
    public void channelsIncludeDomesticFirstSourceAndHlsFallbacks() {
        List<TvChannel> channels = OnlineTvPlaylist.channels();
        assertFalse(channels.isEmpty());
        assertTrue(channels.get(0).uri.contains("huoshanstatic.com"));
        assertTrue(hasHlsChannel(channels));
    }

    @Test
    public void channelsIncludeBusinessContentForFinanceDemo() {
        List<TvChannel> channels = OnlineTvPlaylist.channels();
        assertTrue(hasBusinessChannel(channels));
    }

    @Test
    public void defaultFinanceChannelExistsInPlaylist() {
        List<TvChannel> channels = OnlineTvPlaylist.channels();

        int index = OnlineTvPlaylist.indexOfChannel(channels, OnlineTvPlaylist.DEFAULT_FINANCE_CHANNEL_ID);

        assertTrue(index >= 0);
        assertEquals("CGTN Global Biz", channels.get(index).title);
    }

    private static boolean hasHlsChannel(List<TvChannel> channels) {
        for (TvChannel channel : channels) {
            if (channel.uri.endsWith(".m3u8")) return true;
        }
        return false;
    }

    private static boolean hasBusinessChannel(List<TvChannel> channels) {
        for (TvChannel channel : channels) {
            if (channel.title.contains("Business") || channel.subtitle.contains("财经")) return true;
        }
        return false;
    }
}
