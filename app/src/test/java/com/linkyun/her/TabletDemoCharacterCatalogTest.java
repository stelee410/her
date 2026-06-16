package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class TabletDemoCharacterCatalogTest {
    @Test
    public void exposesFiveDemoCharacters() {
        TabletDemoCharacter[] characters = TabletDemoCharacterCatalog.all();

        assertEquals(5, characters.length);
        assertEquals("野渡 YEDU", characters[0].label);
        assertEquals("tablet_demo/star_1/greeting.mp4", characters[0].greetingAsset);
        assertEquals("tablet_demo/star_1/idle.mp4", characters[0].idleAsset);
        assertEquals("tablet_demo/star_1/speaking.mp4", characters[0].speakingAsset);
        assertEquals("tablet_demo/star_1/agent.md", characters[0].agentAsset);
        assertEquals(0, characters[0].voiceSlot);
        assertEquals(0, characters[4].voiceSlot);
    }

    @Test
    public void usesSafeRealtimeVoiceUntilCharacterVoicesAreVerified() {
        TabletDemoCharacter[] characters = TabletDemoCharacterCatalog.all();

        assertEquals(TabletDemoCharacterCatalog.REALTIME_SAFE_VOICE, characters[0].voiceIdOverride);
        assertEquals(TabletDemoCharacterCatalog.REALTIME_SAFE_VOICE, characters[1].voiceIdOverride);
        assertEquals(TabletDemoCharacterCatalog.REALTIME_SAFE_VOICE, characters[2].voiceIdOverride);
        assertEquals(TabletDemoCharacterCatalog.REALTIME_SAFE_VOICE, characters[3].voiceIdOverride);
        assertEquals(TabletDemoCharacterCatalog.REALTIME_SAFE_VOICE, characters[4].voiceIdOverride);
    }

    @Test
    public void keepsJessAsHiddenCharacterOutsidePublicList() {
        TabletDemoCharacter[] characters = TabletDemoCharacterCatalog.all();
        TabletDemoCharacter jess = TabletDemoCharacterCatalog.find("jess");

        assertEquals(5, characters.length);
        assertEquals("Jess", jess.label);
        assertEquals("tablet_demo/jess/greeting.mp4", jess.greetingAsset);
        assertEquals("tablet_demo/jess/idle.mp4", jess.idleAsset);
        assertEquals("tablet_demo/jess/speaking.mp4", jess.speakingAsset);
        assertEquals("tablet_demo/jess/agent.md", jess.agentAsset);
        for (TabletDemoCharacter character : characters) {
            assertTrue(!"jess".equals(character.id));
        }
    }

    @Test
    public void allCharactersUseNormalizedAssetNames() {
        TabletDemoCharacter luma = TabletDemoCharacterCatalog.find("star_2");
        TabletDemoCharacter vesper = TabletDemoCharacterCatalog.find("star_5");

        assertEquals("tablet_demo/star_2/speaking.mp4", luma.speakingAsset);
        assertTrue(vesper.greetingAsset.endsWith("/greeting.mp4"));
        assertTrue(vesper.idleAsset.endsWith("/idle.mp4"));
        assertTrue(vesper.speakingAsset.endsWith("/speaking.mp4"));
    }

    @Test
    public void unknownCharacterFallsBackToDefault() {
        assertEquals("star_1", TabletDemoCharacterCatalog.find("missing").id);
    }
}
