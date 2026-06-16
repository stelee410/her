package com.linkyun.her;

final class TabletDemoCharacterCatalog {
    private static final String ROOT = "tablet_demo";
    static final String REALTIME_SAFE_VOICE = BuildConfig.AGENTVOICE_CLONED_VOICE;

    private static final TabletDemoCharacter[] CHARACTERS = new TabletDemoCharacter[] {
            character("star_1", "野渡 YEDU", 0, REALTIME_SAFE_VOICE),
            character("star_2", "眠音 NEMU", 0, REALTIME_SAFE_VOICE),
            character("star_3", "绮罗 KIRA", 0, REALTIME_SAFE_VOICE),
            character("star_4", "白墙 WALLE", 0, REALTIME_SAFE_VOICE),
            character("star_5", "灰客 GRAY", 0, REALTIME_SAFE_VOICE)
    };
    private static final TabletDemoCharacter HIDDEN_JESS = character(
            "jess", "Jess", 5, BuildConfig.AGENTVOICE_CLONED_VOICE);

    private TabletDemoCharacterCatalog() {
    }

    static TabletDemoCharacter defaultCharacter() {
        return CHARACTERS[0];
    }

    static TabletDemoCharacter[] all() {
        return CHARACTERS.clone();
    }

    static TabletDemoCharacter hiddenJess() {
        return HIDDEN_JESS;
    }

    static TabletDemoCharacter find(String id) {
        if (id != null) {
            if (HIDDEN_JESS.id.equals(id)) return HIDDEN_JESS;
            for (TabletDemoCharacter character : CHARACTERS) {
                if (character.id.equals(id)) return character;
            }
        }
        return defaultCharacter();
    }

    private static TabletDemoCharacter character(String id, String label, int voiceSlot) {
        return character(id, label, voiceSlot, null);
    }

    private static TabletDemoCharacter character(String id, String label, int voiceSlot,
            String voiceIdOverride) {
        String directory = ROOT + "/" + id;
        return new TabletDemoCharacter(id, label, directory,
                directory + "/greeting.mp4",
                directory + "/idle.mp4",
                directory + "/speaking.mp4",
                voiceSlot,
                voiceIdOverride);
    }
}
