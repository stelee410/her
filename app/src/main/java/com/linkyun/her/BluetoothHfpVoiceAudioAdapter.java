package com.linkyun.her;

import android.media.AudioDeviceInfo;

final class BluetoothHfpVoiceAudioAdapter implements VoiceAudioAdapter {
    private final VoiceAudioRouteManager routeManager;
    private final HeadsetBindingManager headsets;
    private final boolean allowAnyConnectedBluetooth;

    BluetoothHfpVoiceAudioAdapter(VoiceAudioRouteManager routeManager,
            HeadsetBindingManager headsets,
            boolean allowAnyConnectedBluetooth) {
        this.routeManager = routeManager;
        this.headsets = headsets;
        this.allowAnyConnectedBluetooth = allowAnyConnectedBluetooth;
    }

    @Override public String name() {
        return "bluetooth_hfp";
    }

    @Override public boolean needsPermission() {
        return routeManager != null &&
                routeManager.needsBluetoothPermission(headsets, allowAnyConnectedBluetooth);
    }

    @Override public AudioDeviceInfo beginInput() {
        if (routeManager == null) return null;
        return routeManager.begin(headsets, allowAnyConnectedBluetooth);
    }

    @Override public AudioDeviceInfo beginOutput() {
        if (routeManager == null) return null;
        return routeManager.beginOutput(headsets, allowAnyConnectedBluetooth);
    }

    @Override public boolean useVoiceCommunicationPlayback() {
        return true;
    }

    @Override public boolean supportsBargeIn() {
        return true;
    }

    @Override public boolean keepRouteBetweenTurns() {
        return true;
    }

    @Override public boolean isActive() {
        return routeManager != null && routeManager.isActive();
    }

    @Override public void end() {
        if (routeManager != null) routeManager.end();
    }
}
