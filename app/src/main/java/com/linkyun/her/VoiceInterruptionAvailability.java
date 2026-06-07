package com.linkyun.her;

final class VoiceInterruptionAvailability {
    private VoiceInterruptionAvailability() {
    }

    static boolean hasNewsInterruption(VoiceSessionState voiceState,
            boolean pendingBroadcast,
            boolean interactionActive,
            boolean hasVoiceCard) {
        return (voiceState != null && voiceState.isNewsTool()) ||
                pendingBroadcast ||
                interactionActive ||
                hasVoiceCard;
    }

    static boolean hasWeatherInterruption(VoiceSessionState voiceState,
            boolean pendingBroadcast,
            boolean interactionActive,
            boolean hasVoiceCard) {
        return (voiceState != null && voiceState.isWeatherTool()) ||
                pendingBroadcast ||
                interactionActive ||
                hasVoiceCard;
    }
}
