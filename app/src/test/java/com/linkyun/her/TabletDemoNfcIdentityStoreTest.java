package com.linkyun.her;

import static org.junit.Assert.assertEquals;

import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public final class TabletDemoNfcIdentityStoreTest {
    @Test
    public void tagIdFormatsBytesAsUppercaseHex() {
        assertEquals("04A10BFF", TabletDemoNfcIdentityStore.tagId(
                new byte[] {0x04, (byte) 0xA1, 0x0B, (byte) 0xFF}));
    }

    @Test
    public void normalizeTagIdRemovesSeparatorsAndUppercases() {
        assertEquals("04A10BFF", TabletDemoNfcIdentityStore.normalizeTagId("04:a1-0b ff"));
    }

    @Test
    public void bindPersistsCharacterForNormalizedTagId() {
        FakeSharedPreferences prefs = new FakeSharedPreferences();
        TabletDemoNfcIdentityStore store = new TabletDemoNfcIdentityStore(prefs);
        TabletDemoCharacter character = TabletDemoCharacterCatalog.find("star_3");

        store.bind("04:a1:0b:ff", character);

        assertEquals("star_3", store.characterId("04A10BFF"));
        assertEquals("star_3", store.characterId("04-a1-0b-ff"));
    }

    private static final class FakeSharedPreferences implements SharedPreferences {
        private final Map<String, String> values = new HashMap<>();

        @Override public Map<String, ?> getAll() { return new HashMap<>(values); }
        @Override public String getString(String key, String defValue) {
            return values.containsKey(key) ? values.get(key) : defValue;
        }
        @Override public Set<String> getStringSet(String key, Set<String> defValues) {
            return defValues == null ? null : new HashSet<>(defValues);
        }
        @Override public int getInt(String key, int defValue) { return defValue; }
        @Override public long getLong(String key, long defValue) { return defValue; }
        @Override public float getFloat(String key, float defValue) { return defValue; }
        @Override public boolean getBoolean(String key, boolean defValue) { return defValue; }
        @Override public boolean contains(String key) { return values.containsKey(key); }
        @Override public Editor edit() { return new FakeEditor(); }
        @Override public void registerOnSharedPreferenceChangeListener(
                OnSharedPreferenceChangeListener listener) { }
        @Override public void unregisterOnSharedPreferenceChangeListener(
                OnSharedPreferenceChangeListener listener) { }

        private final class FakeEditor implements Editor {
            @Override public Editor putString(String key, String value) {
                values.put(key, value);
                return this;
            }
            @Override public Editor putStringSet(String key, Set<String> values) { return this; }
            @Override public Editor putInt(String key, int value) { return this; }
            @Override public Editor putLong(String key, long value) { return this; }
            @Override public Editor putFloat(String key, float value) { return this; }
            @Override public Editor putBoolean(String key, boolean value) { return this; }
            @Override public Editor remove(String key) {
                values.remove(key);
                return this;
            }
            @Override public Editor clear() {
                values.clear();
                return this;
            }
            @Override public boolean commit() { return true; }
            @Override public void apply() { }
        }
    }
}
