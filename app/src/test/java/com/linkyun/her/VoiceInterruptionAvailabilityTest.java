package com.linkyun.her;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class VoiceInterruptionAvailabilityTest {
    @Test
    public void newsInterruptionIsAvailableFromTypedStateOrRuntimeFacts() {
        assertTrue(VoiceInterruptionAvailability.hasNewsInterruption(
                VoiceSessionState.fromLegacy("news_tool"), false, false, false));
        assertTrue(VoiceInterruptionAvailability.hasNewsInterruption(
                VoiceSessionState.fromLegacy("ready"), true, false, false));
        assertTrue(VoiceInterruptionAvailability.hasNewsInterruption(
                VoiceSessionState.fromLegacy("ready"), false, true, false));
        assertTrue(VoiceInterruptionAvailability.hasNewsInterruption(
                VoiceSessionState.fromLegacy("ready"), false, false, true));
    }

    @Test
    public void weatherInterruptionIsAvailableFromTypedStateOrRuntimeFacts() {
        assertTrue(VoiceInterruptionAvailability.hasWeatherInterruption(
                VoiceSessionState.fromLegacy("weather_tool"), false, false, false));
        assertTrue(VoiceInterruptionAvailability.hasWeatherInterruption(
                VoiceSessionState.fromLegacy("ready"), true, false, false));
        assertTrue(VoiceInterruptionAvailability.hasWeatherInterruption(
                VoiceSessionState.fromLegacy("ready"), false, true, false));
        assertTrue(VoiceInterruptionAvailability.hasWeatherInterruption(
                VoiceSessionState.fromLegacy("ready"), false, false, true));
    }

    @Test
    public void unrelatedStateWithoutRuntimeFactsDoesNotInterrupt() {
        assertFalse(VoiceInterruptionAvailability.hasNewsInterruption(
                VoiceSessionState.fromLegacy("weather_tool"), false, false, false));
        assertFalse(VoiceInterruptionAvailability.hasWeatherInterruption(
                VoiceSessionState.fromLegacy("news_tool"), false, false, false));
        assertFalse(VoiceInterruptionAvailability.hasNewsInterruption(
                null, false, false, false));
        assertFalse(VoiceInterruptionAvailability.hasWeatherInterruption(
                null, false, false, false));
    }
}
