package com.linkyun.her;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class MyTvPlaylistTest {
    @Test
    public void recognizesVideoExtensionsCaseInsensitively() {
        assertTrue(MyTvPlaylist.isVideo("sample.mp4"));
        assertTrue(MyTvPlaylist.isVideo("EP01.WEBM"));
        assertFalse(MyTvPlaylist.isVideo("cover.jpg"));
    }

    @Test
    public void naturalCompareKeepsEpisodesInNumericOrder() {
        assertTrue(MyTvPlaylist.compareNatural("episode2.mp4", "episode10.mp4") < 0);
        assertTrue(MyTvPlaylist.compareNatural("episode10.mp4", "episode2.mp4") > 0);
        assertTrue(MyTvPlaylist.compareNatural("episode01.mp4", "episode2.mp4") < 0);
    }
}
