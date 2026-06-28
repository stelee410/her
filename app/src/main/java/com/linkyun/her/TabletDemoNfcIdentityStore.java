package com.linkyun.her;

import android.content.SharedPreferences;

import java.util.Locale;

final class TabletDemoNfcIdentityStore {
    private static final String KEY_PREFIX = "tablet_demo_nfc_identity_";

    private final SharedPreferences prefs;

    TabletDemoNfcIdentityStore(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    void bind(String tagId, TabletDemoCharacter character) {
        String normalized = normalizeTagId(tagId);
        if (prefs == null || normalized.isEmpty() || character == null) return;
        prefs.edit().putString(key(normalized), character.id).apply();
    }

    String characterId(String tagId) {
        String normalized = normalizeTagId(tagId);
        if (prefs == null || normalized.isEmpty()) return "";
        return prefs.getString(key(normalized), "");
    }

    static String tagId(byte[] id) {
        if (id == null || id.length == 0) return "";
        StringBuilder builder = new StringBuilder(id.length * 2);
        for (byte value : id) {
            builder.append(String.format(Locale.US, "%02X", value & 0xFF));
        }
        return builder.toString();
    }

    static String normalizeTagId(String tagId) {
        if (tagId == null) return "";
        return tagId.replace(":", "")
                .replace("-", "")
                .replace(" ", "")
                .trim()
                .toUpperCase(Locale.US);
    }

    private static String key(String tagId) {
        return KEY_PREFIX + tagId;
    }
}
