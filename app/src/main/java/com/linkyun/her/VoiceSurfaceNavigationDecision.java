package com.linkyun.her;

final class VoiceSurfaceNavigationDecision {
    private VoiceSurfaceNavigationDecision() {
    }

    enum ScreenAction {
        SHOW_VOICE_HOME,
        SHOW_INITIALIZATION_HOME,
        NONE
    }

    static ScreenAction screenAction(boolean initialized, boolean initializing) {
        if (initialized) return ScreenAction.SHOW_VOICE_HOME;
        if (initializing) return ScreenAction.SHOW_INITIALIZATION_HOME;
        return ScreenAction.NONE;
    }

    static boolean shouldPromptHeadset(boolean boundHeadsetConnected, boolean voiceInputSurfaceActive) {
        return voiceInputSurfaceActive && !boundHeadsetConnected;
    }

    static boolean shouldStartAfterHeadsetBind(boolean startVoiceAfterBind,
            boolean voiceInputSurfaceActive) {
        return startVoiceAfterBind && voiceInputSurfaceActive;
    }
}
