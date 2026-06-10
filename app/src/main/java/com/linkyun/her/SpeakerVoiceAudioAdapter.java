package com.linkyun.her;

import android.media.AudioDeviceInfo;

final class SpeakerVoiceAudioAdapter implements VoiceAudioAdapter {
    @Override public String name() {
        return "speaker";
    }

    @Override public boolean needsPermission() {
        return false;
    }

    @Override public AudioDeviceInfo beginInput() {
        return null;
    }

    @Override public void beginOutput() {
    }

    @Override public boolean useVoiceCommunicationPlayback() {
        return false;
    }

    @Override public boolean supportsBargeIn() {
        return false;
    }

    @Override public boolean keepRouteBetweenTurns() {
        return false;
    }

    @Override public boolean isActive() {
        return false;
    }

    @Override public void end() {
    }
}
