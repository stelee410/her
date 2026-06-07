package com.linkyun.her;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class AssistantLaunchDecisionTest {
    @Test
    public void initializedLaunchShowsVoiceHome() {
        assertEquals(AssistantLaunchDecision.ScreenAction.SHOW_VOICE_HOME,
                AssistantLaunchDecision.screenAction(true, false));
    }

    @Test
    public void initializingLaunchReturnsToInitializationHome() {
        assertEquals(AssistantLaunchDecision.ScreenAction.SHOW_INITIALIZATION_HOME,
                AssistantLaunchDecision.screenAction(false, true));
    }

    @Test
    public void freshLaunchBeginsInitialization() {
        assertEquals(AssistantLaunchDecision.ScreenAction.BEGIN_INITIALIZATION,
                AssistantLaunchDecision.screenAction(false, false));
    }

    @Test
    public void voiceLaunchStartsOnlyWhenHeadsetReadyAndInputIdle() {
        assertEquals(AssistantLaunchDecision.VoiceAction.START,
                AssistantLaunchDecision.voiceAction(true, true, false, false, false, false));
        assertEquals(AssistantLaunchDecision.VoiceAction.PROMPT_HEADSET,
                AssistantLaunchDecision.voiceAction(false, true, false, false, false, false));
        assertEquals(AssistantLaunchDecision.VoiceAction.SKIP,
                AssistantLaunchDecision.voiceAction(true, true, true, false, false, false));
        assertEquals(AssistantLaunchDecision.VoiceAction.SKIP,
                AssistantLaunchDecision.voiceAction(true, true, false, true, false, false));
        assertEquals(AssistantLaunchDecision.VoiceAction.SKIP,
                AssistantLaunchDecision.voiceAction(true, true, false, false, true, false));
        assertEquals(AssistantLaunchDecision.VoiceAction.SKIP,
                AssistantLaunchDecision.voiceAction(true, true, false, false, false, true));
    }

    @Test
    public void delayedVoiceLaunchSkipsWhenUserLeftVoiceInputSurface() {
        assertEquals(AssistantLaunchDecision.VoiceAction.SKIP,
                AssistantLaunchDecision.voiceAction(true, false, false, false, false, false));
        assertEquals(AssistantLaunchDecision.VoiceAction.SKIP,
                AssistantLaunchDecision.voiceAction(false, false, false, false, false, false));
    }
}
