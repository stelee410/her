package com.linkyun.her;

final class AssistantLaunchDecision {
    private AssistantLaunchDecision() {
    }

    enum ScreenAction {
        SHOW_VOICE_HOME,
        SHOW_INITIALIZATION_HOME,
        BEGIN_INITIALIZATION
    }

    enum VoiceAction {
        START,
        PROMPT_HEADSET,
        SKIP
    }

    static ScreenAction screenAction(boolean initialized, boolean initializing) {
        if (initialized) return ScreenAction.SHOW_VOICE_HOME;
        if (initializing) return ScreenAction.SHOW_INITIALIZATION_HOME;
        return ScreenAction.BEGIN_INITIALIZATION;
    }

    static VoiceAction voiceAction(
            boolean boundHeadsetConnected,
            boolean voiceInputSurfaceActive,
            boolean summaryInProgress,
            boolean playbackActive,
            boolean micRunning,
            boolean inputAudioOpen) {
        if (!voiceInputSurfaceActive) return VoiceAction.SKIP;
        if (!boundHeadsetConnected) return VoiceAction.PROMPT_HEADSET;
        if (summaryInProgress || playbackActive || micRunning || inputAudioOpen) {
            return VoiceAction.SKIP;
        }
        return VoiceAction.START;
    }
}
