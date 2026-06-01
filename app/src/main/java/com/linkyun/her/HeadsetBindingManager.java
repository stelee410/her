package com.linkyun.her;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class HeadsetBindingManager {
    private static final String PREF_BOUND_HEADSET_KEY = "bound_headset_key";
    private static final String PREF_BOUND_HEADSET_LABEL = "bound_headset_label";

    private final Context context;
    private final AudioManager audioManager;
    private final SharedPreferences prefs;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable listener;
    private AudioDeviceCallback callback;

    HeadsetBindingManager(Context context, SharedPreferences prefs, Runnable listener) {
        this.context = context.getApplicationContext();
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.prefs = prefs;
        this.listener = listener;
    }

    void start() {
        if (audioManager == null || callback != null) return;
        callback = new AudioDeviceCallback() {
            @Override public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
                notifyChanged();
            }

            @Override public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
                notifyChanged();
            }
        };
        audioManager.registerAudioDeviceCallback(callback, main);
    }

    void stop() {
        if (audioManager == null || callback == null) return;
        audioManager.unregisterAudioDeviceCallback(callback);
        callback = null;
    }

    List<Device> connectedHeadsets() {
        Map<String, Device> devices = new LinkedHashMap<>();
        collect(devices, AudioManager.GET_DEVICES_OUTPUTS);
        collect(devices, AudioManager.GET_DEVICES_INPUTS);
        return new ArrayList<>(devices.values());
    }

    boolean hasBoundHeadset() {
        return !boundKey().isEmpty();
    }

    String boundLabel() {
        return prefs.getString(PREF_BOUND_HEADSET_LABEL, "");
    }

    boolean isBoundConnected() {
        String key = boundKey();
        if (key.isEmpty()) return false;
        for (Device device : connectedHeadsets()) {
            if (key.equals(device.key)) return true;
        }
        return false;
    }

    void bind(Device device) {
        if (device == null) return;
        prefs.edit()
                .putString(PREF_BOUND_HEADSET_KEY, device.key)
                .putString(PREF_BOUND_HEADSET_LABEL, device.label)
                .apply();
        notifyChanged();
    }

    void clearBinding() {
        prefs.edit()
                .remove(PREF_BOUND_HEADSET_KEY)
                .remove(PREF_BOUND_HEADSET_LABEL)
                .apply();
        notifyChanged();
    }

    private String boundKey() {
        return prefs.getString(PREF_BOUND_HEADSET_KEY, "");
    }

    private void collect(Map<String, Device> out, int flag) {
        if (audioManager == null) return;
        AudioDeviceInfo[] infos = audioManager.getDevices(flag);
        for (AudioDeviceInfo info : infos) {
            if (!isHeadset(info.getType())) continue;
            Device device = Device.from(info);
            if (!out.containsKey(device.key)) out.put(device.key, device);
        }
    }

    private boolean isHeadset(int type) {
        return type == AudioDeviceInfo.TYPE_WIRED_HEADSET
                || type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                || type == AudioDeviceInfo.TYPE_USB_HEADSET
                || type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                || type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                || type == AudioDeviceInfo.TYPE_HEARING_AID
                || type == AudioDeviceInfo.TYPE_BLE_HEADSET
                || type == AudioDeviceInfo.TYPE_BLE_SPEAKER
                || type == AudioDeviceInfo.TYPE_BLE_BROADCAST;
    }

    private void notifyChanged() {
        if (listener != null) main.post(listener);
    }

    static final class Device {
        final String key;
        final String label;

        private Device(String key, String label) {
            this.key = key;
            this.label = label;
        }

        static Device from(AudioDeviceInfo info) {
            String product = info.getProductName() == null ? "" : info.getProductName().toString().trim();
            if (product.isEmpty()) product = defaultLabel(info.getType());
            String normalized = product.toLowerCase(Locale.US).replaceAll("\\s+", " ");
            return new Device(info.getType() + ":" + normalized, product);
        }

        private static String defaultLabel(int type) {
            if (type == AudioDeviceInfo.TYPE_WIRED_HEADSET) return "Wired headset";
            if (type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES) return "Wired headphones";
            if (type == AudioDeviceInfo.TYPE_USB_HEADSET) return "USB headset";
            if (type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) return "Bluetooth headphones";
            if (type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) return "Bluetooth headset";
            if (type == AudioDeviceInfo.TYPE_HEARING_AID) return "Hearing aid";
            if (type == AudioDeviceInfo.TYPE_BLE_HEADSET) return "Bluetooth LE headset";
            if (type == AudioDeviceInfo.TYPE_BLE_SPEAKER) return "Bluetooth LE audio";
            if (type == AudioDeviceInfo.TYPE_BLE_BROADCAST) return "Bluetooth LE broadcast";
            return "Headphones";
        }
    }
}
