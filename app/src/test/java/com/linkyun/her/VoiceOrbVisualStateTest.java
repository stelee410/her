package com.linkyun.her;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class VoiceOrbVisualStateTest {
    @Test
    public void listeningUsesListeningAccentAndMinimumEnergy() {
        VoiceOrbVisualState visual =
                VoiceOrbVisualState.from(VoiceSessionState.fromLegacy("listening"));

        assertEquals(VoiceOrbVisualState.LISTENING_ACCENT, visual.accentColor);
        assertEquals(0.12f, visual.energy(0f, 0f), 0.001f);
    }

    @Test
    public void processingStatesPulseAboveInputEnergy() {
        VoiceOrbVisualState processing =
                VoiceOrbVisualState.from(VoiceSessionState.fromLegacy("processing"));
        VoiceOrbVisualState thinking =
                VoiceOrbVisualState.from(VoiceSessionState.fromLegacy("thinking"));
        VoiceOrbVisualState connecting =
                VoiceOrbVisualState.from(VoiceSessionState.fromLegacy("connecting"));

        assertEquals(0.26f, processing.energy(0f, 0.25f), 0.001f);
        assertEquals(0.26f, thinking.energy(0f, 0.25f), 0.001f);
        assertEquals(0.26f, connecting.energy(0f, 0.25f), 0.001f);
    }

    @Test
    public void speakingPulsesMoreStronglyAndIdleKeepsInputEnergy() {
        VoiceOrbVisualState speaking =
                VoiceOrbVisualState.from(VoiceSessionState.fromLegacy("speaking"));
        VoiceOrbVisualState idle =
                VoiceOrbVisualState.from(VoiceSessionState.fromLegacy("ready"));

        assertEquals(0.48f, speaking.energy(0f, 0.125f), 0.001f);
        assertEquals(0.42f, idle.energy(0.42f, 0.125f), 0.001f);
        assertEquals(VoiceOrbVisualState.DEFAULT_ACCENT, idle.accentColor);
    }
}
