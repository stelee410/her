package com.linkyun.her;

import android.media.AudioDeviceInfo;

interface VoiceAudioAdapter {
    String name();
    boolean needsPermission();
    AudioDeviceInfo beginInput();
    void beginOutput();
    boolean useVoiceCommunicationPlayback();
    boolean supportsBargeIn();
    boolean keepRouteBetweenTurns();
    boolean isActive();
    void end();
}
