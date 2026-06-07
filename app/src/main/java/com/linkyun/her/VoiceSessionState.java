package com.linkyun.her;

import java.util.Locale;

final class VoiceSessionState {
    private final VoiceSessionStatus status;
    private final String legacyValue;

    private VoiceSessionState(VoiceSessionStatus status, String legacyValue) {
        this.status = status;
        this.legacyValue = legacyValue;
    }

    static VoiceSessionState initial() {
        return fromLegacy(VoiceSessionStatus.IDLE.legacyValue());
    }

    static VoiceSessionState fromLegacy(String value) {
        String normalized = VoiceSessionStatus.normalize(value);
        VoiceSessionStatus status = VoiceSessionStatus.fromLegacy(normalized);
        String legacyValue = status == VoiceSessionStatus.UNKNOWN ? normalized : status.legacyValue();
        return new VoiceSessionState(status, legacyValue);
    }

    VoiceSessionStatus status() {
        return status;
    }

    String legacyValue() {
        return legacyValue;
    }

    boolean isReady() {
        return status == VoiceSessionStatus.READY;
    }

    boolean isIdle() {
        return status == VoiceSessionStatus.IDLE;
    }

    boolean isListening() {
        return status == VoiceSessionStatus.LISTENING;
    }

    boolean isSpeaking() {
        return status == VoiceSessionStatus.SPEAKING;
    }

    boolean isProcessing() {
        return status == VoiceSessionStatus.PROCESSING;
    }

    boolean isTextOnly() {
        return status == VoiceSessionStatus.TEXT_ONLY;
    }

    boolean isNewsTool() {
        return status == VoiceSessionStatus.NEWS_TOOL;
    }

    boolean isWeatherTool() {
        return status == VoiceSessionStatus.WEATHER_TOOL;
    }

    boolean isResponsePending() {
        return status == VoiceSessionStatus.THINKING || status == VoiceSessionStatus.PROCESSING;
    }

    boolean shouldKeepScreenOn() {
        return status == VoiceSessionStatus.CONNECTING ||
                status == VoiceSessionStatus.LISTENING ||
                isResponsePending() ||
                status == VoiceSessionStatus.SPEAKING ||
                status == VoiceSessionStatus.NEWS_ACK ||
                status == VoiceSessionStatus.NEWS_TOOL ||
                status == VoiceSessionStatus.WEATHER_TOOL ||
                status == VoiceSessionStatus.SUMMARIZING;
    }

    boolean shouldApplyContextUpdateForNextTurn() {
        return status == VoiceSessionStatus.READY || status == VoiceSessionStatus.IDLE;
    }

    String labelText(boolean summaryInProgress,
                     boolean weatherInteractionActive,
                     boolean boundHeadsetConnected,
                     boolean hasBoundHeadset,
                     boolean hasConnectedHeadsets) {
        if (summaryInProgress) return "Summarizing";
        if (isResponsePending()) return "Processing";
        if (weatherInteractionActive) return "Checking weather";
        if (status == VoiceSessionStatus.NEWS_ACK) return "Checking news";
        if (status == VoiceSessionStatus.NEWS_TOOL) return "Reading agentNews";
        if (status == VoiceSessionStatus.WEATHER_TOOL) return "Checking weather";
        if (status == VoiceSessionStatus.SUMMARIZING) return "Summarizing";
        if (status == VoiceSessionStatus.TEXT_ONLY) return "Text only";
        if (status == VoiceSessionStatus.ERROR) return "Error";
        if (!boundHeadsetConnected) {
            if (hasBoundHeadset) return "Headset disconnected";
            if (hasConnectedHeadsets) return "Tap headset to bind";
            return "Text only · connect headphones";
        }
        return capitalize(legacyValue);
    }

    String voiceButtonText(boolean toolTtsAvailable,
                           boolean weatherInterruptionAvailable,
                           boolean newsInterruptionAvailable) {
        if (toolTtsAvailable) return "■";
        if (weatherInterruptionAvailable) return "■";
        if (isNewsTool() || isWeatherTool() || newsInterruptionAvailable) return "■";
        return "♩";
    }

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) return "";
        return value.substring(0, 1).toUpperCase(Locale.US) + value.substring(1);
    }
}
