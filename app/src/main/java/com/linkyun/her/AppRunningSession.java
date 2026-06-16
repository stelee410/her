package com.linkyun.her;

final class AppRunningSession {
    static final String MY_TV = "my_tv";

    private final String appId;
    private final boolean resumeVoiceAfterExit;

    private AppRunningSession(String appId, boolean resumeVoiceAfterExit) {
        this.appId = appId == null || appId.trim().isEmpty() ? "app" : appId;
        this.resumeVoiceAfterExit = resumeVoiceAfterExit;
    }

    static AppRunningSession start(String appId, boolean voiceSurfaceActive, boolean textModeActive) {
        return new AppRunningSession(appId, voiceSurfaceActive && !textModeActive);
    }

    String appId() {
        return appId;
    }

    boolean shouldResumeVoiceAfterExit(boolean restoreVoice) {
        return restoreVoice && resumeVoiceAfterExit;
    }
}
