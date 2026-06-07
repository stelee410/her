package com.linkyun.her;

final class VoiceSessionStateReducer {
    private VoiceSessionStateReducer() {
    }

    static VoiceSessionState reduce(VoiceSessionState current, String nextLegacyState) {
        return VoiceSessionState.fromLegacy(nextLegacyState);
    }
}
