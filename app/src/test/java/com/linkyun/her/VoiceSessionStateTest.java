package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class VoiceSessionStateTest {
    @Test
    public void mapsLegacyStringsToTypedStatus() {
        VoiceSessionState state = VoiceSessionState.fromLegacy("processing");

        assertEquals(VoiceSessionStatus.PROCESSING, state.status());
        assertEquals("processing", state.legacyValue());
        assertTrue(state.isResponsePending());
    }

    @Test
    public void nullOrBlankStateFallsBackToIdle() {
        assertEquals(VoiceSessionStatus.IDLE, VoiceSessionState.fromLegacy(null).status());
        assertEquals(VoiceSessionStatus.IDLE, VoiceSessionState.fromLegacy(" ").status());
    }

    @Test
    public void preservesUnknownLegacyValueForCompatibility() {
        VoiceSessionState state = VoiceSessionState.fromLegacy("custom_state");

        assertEquals(VoiceSessionStatus.UNKNOWN, state.status());
        assertEquals("custom_state", state.legacyValue());
        assertEquals("Custom_state", state.labelText(false, false, true, false, false));
    }

    @Test
    public void derivesLabelsFromTypedStateAndContext() {
        assertEquals("Processing", VoiceSessionState.fromLegacy("thinking")
                .labelText(false, false, true, false, false));
        assertEquals("Checking news", VoiceSessionState.fromLegacy("news_ack")
                .labelText(false, false, true, false, false));
        assertEquals("Reading agentNews", VoiceSessionState.fromLegacy("news_tool")
                .labelText(false, false, true, false, false));
        assertEquals("Checking weather", VoiceSessionState.fromLegacy("weather_tool")
                .labelText(false, false, true, false, false));
        assertEquals("Summarizing", VoiceSessionState.fromLegacy("summarizing")
                .labelText(false, false, false, true, false));
        assertEquals("Text only", VoiceSessionState.fromLegacy("text_only")
                .labelText(false, false, true, false, false));
        assertEquals("Error", VoiceSessionState.fromLegacy("error")
                .labelText(false, false, false, true, false));
        assertEquals("Checking weather", VoiceSessionState.fromLegacy("ready")
                .labelText(false, true, true, false, false));
        assertEquals("Headset disconnected", VoiceSessionState.fromLegacy("ready")
                .labelText(false, false, false, true, false));
        assertEquals("Tap headset to bind", VoiceSessionState.fromLegacy("ready")
                .labelText(false, false, false, false, true));
        assertEquals("Text only · connect headphones", VoiceSessionState.fromLegacy("ready")
                .labelText(false, false, false, false, false));
    }

    @Test
    public void derivesKeepScreenOnFromStatus() {
        assertTrue(VoiceSessionState.fromLegacy("connecting").shouldKeepScreenOn());
        assertTrue(VoiceSessionState.fromLegacy("listening").shouldKeepScreenOn());
        assertTrue(VoiceSessionState.fromLegacy("processing").shouldKeepScreenOn());
        assertTrue(VoiceSessionState.fromLegacy("speaking").shouldKeepScreenOn());
        assertTrue(VoiceSessionState.fromLegacy("news_tool").shouldKeepScreenOn());
        assertTrue(VoiceSessionState.fromLegacy("weather_tool").shouldKeepScreenOn());
        assertFalse(VoiceSessionState.fromLegacy("ready").shouldKeepScreenOn());
        assertFalse(VoiceSessionState.fromLegacy("idle").shouldKeepScreenOn());
    }

    @Test
    public void derivesVoiceButtonTextFromStatusAndInterruptions() {
        assertEquals("■", VoiceSessionState.fromLegacy("ready").voiceButtonText(true, false, false));
        assertEquals("■", VoiceSessionState.fromLegacy("ready").voiceButtonText(false, true, false));
        assertEquals("■", VoiceSessionState.fromLegacy("news_tool").voiceButtonText(false, false, false));
        assertEquals("■", VoiceSessionState.fromLegacy("weather_tool").voiceButtonText(false, false, false));
        assertEquals("■", VoiceSessionState.fromLegacy("ready").voiceButtonText(false, false, true));
        assertEquals("♩", VoiceSessionState.fromLegacy("ready").voiceButtonText(false, false, false));
    }

    @Test
    public void reducerKeepsLegacyTransitionBoundary() {
        VoiceSessionState current = VoiceSessionState.fromLegacy("idle");

        VoiceSessionState next = VoiceSessionStateReducer.reduce(current, "ready");

        assertEquals(VoiceSessionStatus.READY, next.status());
        assertTrue(next.shouldApplyContextUpdateForNextTurn());
    }
}
