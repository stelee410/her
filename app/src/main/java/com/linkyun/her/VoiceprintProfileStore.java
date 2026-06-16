package com.linkyun.her;

import android.content.SharedPreferences;

import java.util.Base64;
import java.util.Locale;

final class VoiceprintProfileStore {
    private static final String PREF_ENABLED = "voiceprint_enabled";
    private static final String PREF_EMBEDDING = "voiceprint_embedding";
    private static final String PREF_SAMPLES = "voiceprint_samples";
    private final SharedPreferences prefs;

    VoiceprintProfileStore(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    boolean isEnabled() {
        return prefs.getBoolean(PREF_ENABLED, false) && hasProfile();
    }

    boolean hasProfile() {
        return !prefs.getString(PREF_EMBEDDING, "").isEmpty();
    }

    int samples() {
        return prefs.getInt(PREF_SAMPLES, 0);
    }

    void setEnabled(boolean enabled) {
        prefs.edit().putBoolean(PREF_ENABLED, enabled).apply();
    }

    void save(float[] embedding, int samples) {
        prefs.edit()
                .putString(PREF_EMBEDDING, encode(embedding))
                .putInt(PREF_SAMPLES, samples)
                .putBoolean(PREF_ENABLED, true)
                .apply();
    }

    float[] embedding() {
        return decode(prefs.getString(PREF_EMBEDDING, ""));
    }

    void clear() {
        prefs.edit()
                .remove(PREF_EMBEDDING)
                .remove(PREF_SAMPLES)
                .putBoolean(PREF_ENABLED, false)
                .apply();
    }

    String label() {
        if (!hasProfile()) return "Not enrolled";
        return String.format(Locale.US, "%s · %d samples", isEnabled() ? "On" : "Off", samples());
    }

    private static String encode(float[] values) {
        if (values == null || values.length == 0) return "";
        byte[] bytes = new byte[values.length * 4];
        for (int i = 0; i < values.length; i++) {
            int bits = Float.floatToIntBits(values[i]);
            int j = i * 4;
            bytes[j] = (byte) (bits & 0xff);
            bytes[j + 1] = (byte) ((bits >> 8) & 0xff);
            bytes[j + 2] = (byte) ((bits >> 16) & 0xff);
            bytes[j + 3] = (byte) ((bits >> 24) & 0xff);
        }
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static float[] decode(String encoded) {
        if (encoded == null || encoded.isEmpty()) return new float[0];
        try {
            byte[] bytes = Base64.getDecoder().decode(encoded);
            float[] values = new float[bytes.length / 4];
            for (int i = 0; i < values.length; i++) {
                int j = i * 4;
                int bits = (bytes[j] & 0xff)
                        | ((bytes[j + 1] & 0xff) << 8)
                        | ((bytes[j + 2] & 0xff) << 16)
                        | ((bytes[j + 3] & 0xff) << 24);
                values[i] = Float.intBitsToFloat(bits);
            }
            return values;
        } catch (IllegalArgumentException error) {
            return new float[0];
        }
    }
}
