package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.List;

import org.junit.Test;

public final class TtsVoiceCatalogTest {
    @Test
    public void catalogContainsOnlyVerifiedVoices() {
        List<Voice> voices = TtsVoiceCatalog.usableVoices();

        assertEquals(10, voices.size());
        assertEquals("zh_female_vv_uranus_bigtts", voices.get(0).id);
        assertEquals("seed2", voices.get(0).resource);
        assertEquals("S_VCQjam1U1", voices.get(9).id);
        assertEquals("v1", voices.get(9).resource);
    }

    @Test
    public void unknownVoiceFallsBackToDefault() {
        assertEquals(TtsVoiceCatalog.DEFAULT_ID, TtsVoiceCatalog.find("missing").id);
        assertEquals("vivi 2.0", TtsVoiceCatalog.labelFor("missing"));
    }

    @Test
    public void fallbackOrderExcludesSelectedVoice() {
        String selected = "zh_female_cancan_mars_bigtts";
        String[] fallback = TtsVoiceCatalog.playbackOrder(selected);

        assertEquals(TtsVoiceCatalog.DEFAULT_ID, fallback[0]);
        for (String voice : fallback) {
            assertFalse(selected.equals(voice));
        }
    }

    @Test
    public void voiceOrderStartsWithSelectedVoice() {
        String selected = "saturn_zh_female_keainvsheng_tob";
        String[] order = TtsVoiceCatalog.voiceOrder(selected);

        assertEquals(selected, order[0]);
        assertEquals(TtsVoiceCatalog.DEFAULT_ID, order[1]);
    }
}
