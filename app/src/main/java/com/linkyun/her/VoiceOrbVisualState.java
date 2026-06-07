package com.linkyun.her;

final class VoiceOrbVisualState {
    static final int LISTENING_ACCENT = 0x33FF6377;
    static final int DEFAULT_ACCENT = 0x22B96A7C;

    final int accentColor;
    private final Motion motion;

    private VoiceOrbVisualState(Motion motion, int accentColor) {
        this.motion = motion;
        this.accentColor = accentColor;
    }

    static VoiceOrbVisualState from(VoiceSessionState state) {
        VoiceSessionStatus status = state == null
                ? VoiceSessionStatus.IDLE
                : state.status();
        if (status == VoiceSessionStatus.SPEAKING) {
            return new VoiceOrbVisualState(Motion.SPEAKING, DEFAULT_ACCENT);
        }
        if (status == VoiceSessionStatus.THINKING ||
                status == VoiceSessionStatus.PROCESSING ||
                status == VoiceSessionStatus.CONNECTING) {
            return new VoiceOrbVisualState(Motion.PROCESSING, DEFAULT_ACCENT);
        }
        if (status == VoiceSessionStatus.LISTENING) {
            return new VoiceOrbVisualState(Motion.LISTENING, LISTENING_ACCENT);
        }
        return new VoiceOrbVisualState(Motion.IDLE, DEFAULT_ACCENT);
    }

    float energy(float inputEnergy, float phase) {
        float clean = Math.max(0f, Math.min(1f, inputEnergy));
        if (motion == Motion.SPEAKING) {
            return Math.max(clean, 0.34f + 0.14f * (float) Math.sin(phase * Math.PI * 4));
        }
        if (motion == Motion.PROCESSING) {
            return Math.max(clean, 0.18f + 0.08f * (float) Math.sin(phase * Math.PI * 2));
        }
        if (motion == Motion.LISTENING) {
            return Math.max(clean, 0.12f);
        }
        return clean;
    }

    private enum Motion {
        IDLE,
        LISTENING,
        PROCESSING,
        SPEAKING
    }
}
