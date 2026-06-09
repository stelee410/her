package com.linkyun.her;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;

final class VoiceAudioRouteManager {
    private final Context context;
    private final AudioManager audioManager;
    private boolean active;
    private boolean startedSco;
    private int previousMode = AudioManager.MODE_NORMAL;
    private boolean previousScoOn;

    VoiceAudioRouteManager(Context context) {
        this.context = context.getApplicationContext();
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    AudioDeviceInfo begin(HeadsetBindingManager headsets) {
        if (audioManager == null || headsets == null) return null;
        if (active) return headsets.boundVoiceInputDevice();
        AudioDeviceInfo input = headsets.boundVoiceInputDevice();
        if (input == null && !headsets.isBoundBluetoothHeadset()) return null;
        previousMode = audioManager.getMode();
        previousScoOn = audioManager.isBluetoothScoOn();
        active = true;
        try {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        } catch (RuntimeException ignored) { }
        if (headsets.isBoundBluetoothHeadset()) {
            routeBluetooth(headsets);
            input = headsets.boundVoiceInputDevice();
        }
        return input;
    }

    void end() {
        if (audioManager == null || !active) return;
        active = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                audioManager.clearCommunicationDevice();
            } catch (RuntimeException ignored) { }
        }
        if (startedSco) {
            try {
                audioManager.stopBluetoothSco();
            } catch (RuntimeException ignored) { }
            startedSco = false;
        }
        try {
            audioManager.setBluetoothScoOn(previousScoOn);
        } catch (RuntimeException ignored) { }
        try {
            audioManager.setMode(previousMode);
        } catch (RuntimeException ignored) { }
    }

    boolean isActive() {
        return active;
    }

    boolean needsBluetoothPermission(HeadsetBindingManager headsets) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && headsets != null
                && headsets.isBoundBluetoothHeadset()
                && context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED;
    }

    private void routeBluetooth(HeadsetBindingManager headsets) {
        if (needsBluetoothPermission(headsets)) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AudioDeviceInfo communicationDevice = headsets.boundBluetoothCommunicationDevice();
            if (communicationDevice != null) {
                try {
                    audioManager.setCommunicationDevice(communicationDevice);
                } catch (RuntimeException ignored) { }
            }
        }
        try {
            audioManager.startBluetoothSco();
            startedSco = true;
        } catch (RuntimeException ignored) { }
        try {
            audioManager.setBluetoothScoOn(true);
        } catch (RuntimeException ignored) { }
    }
}
