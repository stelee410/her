package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class VoiceSurfaceNavigationDecisionTest {
    @Test
    public void initializedShowsVoiceHome() {
        assertEquals(VoiceSurfaceNavigationDecision.ScreenAction.SHOW_VOICE_HOME,
                VoiceSurfaceNavigationDecision.screenAction(true, false));
    }

    @Test
    public void initializingShowsInitializationHome() {
        assertEquals(VoiceSurfaceNavigationDecision.ScreenAction.SHOW_INITIALIZATION_HOME,
                VoiceSurfaceNavigationDecision.screenAction(false, true));
    }

    @Test
    public void freshStateDoesNotOpenVoiceSurface() {
        assertEquals(VoiceSurfaceNavigationDecision.ScreenAction.NONE,
                VoiceSurfaceNavigationDecision.screenAction(false, false));
    }

    @Test
    public void promptsHeadsetOnlyWhenNotBoundConnected() {
        assertTrue(VoiceSurfaceNavigationDecision.shouldPromptHeadset(false, true));
        assertFalse(VoiceSurfaceNavigationDecision.shouldPromptHeadset(true, true));
    }

    @Test
    public void promptAndBindAutoStartRequireActiveVoiceInputSurface() {
        assertFalse(VoiceSurfaceNavigationDecision.shouldPromptHeadset(false, false));
        assertTrue(VoiceSurfaceNavigationDecision.shouldStartAfterHeadsetBind(true, true));
        assertFalse(VoiceSurfaceNavigationDecision.shouldStartAfterHeadsetBind(true, false));
        assertFalse(VoiceSurfaceNavigationDecision.shouldStartAfterHeadsetBind(false, true));
    }
}
