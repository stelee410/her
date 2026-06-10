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
        return begin(headsets, false);
    }

    AudioDeviceInfo begin(HeadsetBindingManager headsets, boolean allowAnyConnectedBluetooth) {
        if (audioManager == null || headsets == null) return null;
        if (active) return headsets.voiceInputDevice(allowAnyConnectedBluetooth);
        AudioDeviceInfo input = headsets.voiceInputDevice(allowAnyConnectedBluetooth);
        if (input == null && !headsets.hasBluetoothHeadset(allowAnyConnectedBluetooth)) return null;
        previousMode = audioManager.getMode();
        previousScoOn = audioManager.isBluetoothScoOn();
        active = true;
        try {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        } catch (RuntimeException ignored) { }
        if (headsets.hasBluetoothHeadset(allowAnyConnectedBluetooth)) {
            routeBluetooth(headsets, allowAnyConnectedBluetooth);
            input = headsets.voiceInputDevice(allowAnyConnectedBluetooth);
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
        return needsBluetoothPermission(headsets, false);
    }

    boolean needsBluetoothPermission(HeadsetBindingManager headsets, boolean allowAnyConnectedBluetooth) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && headsets != null
                && headsets.hasBluetoothHeadset(allowAnyConnectedBluetooth)
                && context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                        != PackageManager.PERMISSION_GRANTED;
    }

    private void routeBluetooth(HeadsetBindingManager headsets) {
        routeBluetooth(headsets, false);
    }

    private void routeBluetooth(HeadsetBindingManager headsets, boolean allowAnyConnectedBluetooth) {
        if (needsBluetoothPermission(headsets, allowAnyConnectedBluetooth)) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AudioDeviceInfo communicationDevice =
                    headsets.bluetoothCommunicationDevice(allowAnyConnectedBluetooth);
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
