package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class AppRunningSessionTest {
    @Test
    public void recordsAppIdentityAndRestoresOnlyFromVoiceSurface() {
        AppRunningSession session = AppRunningSession.start("music", true, false);

        assertEquals("music", session.appId());
        assertTrue(session.shouldResumeVoiceAfterExit(true));
        assertFalse(session.shouldResumeVoiceAfterExit(false));
    }

    @Test
    public void doesNotResumeVoiceWhenStartedFromTextModeOrInactiveSurface() {
        assertFalse(AppRunningSession.start("quick_app", true, true)
                .shouldResumeVoiceAfterExit(true));
        assertFalse(AppRunningSession.start("pad_case", false, false)
                .shouldResumeVoiceAfterExit(true));
    }

    @Test
    public void normalizesBlankAppId() {
        assertEquals("app", AppRunningSession.start(" ", true, false).appId());
    }
}
