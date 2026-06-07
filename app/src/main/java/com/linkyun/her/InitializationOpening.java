package com.linkyun.her;

final class InitializationOpening {
    private InitializationOpening() {
    }

    static boolean shouldDeliver(boolean initializing, boolean openingDelivered) {
        return initializing && !openingDelivered;
    }

    static boolean shouldDeliverTts(boolean initializing, boolean openingDelivered,
            boolean textModeActive) {
        return shouldDeliver(initializing, openingDelivered) && !textModeActive;
    }

    static boolean shouldMarkRealtimeDelivered(boolean initializing, boolean openingDelivered) {
        return shouldDeliver(initializing, openingDelivered);
    }

    static String cleanAgentName(String agentName) {
        return agentName == null ? "" : agentName.trim();
    }

    static String openingText(String agentName) {
        String name = cleanAgentName(agentName);
        return "嗨，我是 " + name + "。我们先从你开始吧：你叫什么名字，平时希望我怎么称呼你？";
    }

    static boolean shouldAddSubtitle(boolean initializing, String opening, boolean hasExistingOpening) {
        return initializing
                && !hasExistingOpening
                && opening != null
                && !opening.trim().isEmpty();
    }

    static boolean shouldHandleTtsCallback(boolean initializing, boolean textModeActive,
            boolean voiceInputSurfaceActive) {
        return initializing && !textModeActive && voiceInputSurfaceActive;
    }
}
