package com.linkyun.her;

final class TabletDemoCharacter {
    final String id;
    final String label;
    final String directory;
    final String greetingAsset;
    final String idleAsset;
    final String speakingAsset;
    final String agentAsset;
    final int voiceSlot;
    final String voiceIdOverride;

    TabletDemoCharacter(String id, String label, String directory,
            String greetingAsset, String idleAsset, String speakingAsset, int voiceSlot) {
        this(id, label, directory, greetingAsset, idleAsset, speakingAsset, voiceSlot, null);
    }

    TabletDemoCharacter(String id, String label, String directory,
            String greetingAsset, String idleAsset, String speakingAsset, int voiceSlot,
            String voiceIdOverride) {
        this.id = id;
        this.label = label;
        this.directory = directory;
        this.greetingAsset = greetingAsset;
        this.idleAsset = idleAsset;
        this.speakingAsset = speakingAsset;
        this.agentAsset = directory + "/agent.md";
        this.voiceSlot = voiceSlot;
        this.voiceIdOverride = voiceIdOverride;
    }
}
