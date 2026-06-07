package com.linkyun.her;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class WeatherIntentCompletionDecisionTest {
    @Test
    public void staleFetchDoesNotUpdateStateOrScheduleListening() {
        WeatherIntentCompletionDecision decision =
                WeatherIntentCompletionDecision.notWeatherQuery(false, true, false, true);

        assertFalse(decision.shouldUpdateState);
        assertFalse(decision.shouldScheduleListening);
    }

    @Test
    public void currentRealtimeFetchReturnsReadyAndSchedulesListening() {
        WeatherIntentCompletionDecision decision =
                WeatherIntentCompletionDecision.notWeatherQuery(true, true, false, true);

        assertTrue(decision.shouldUpdateState);
        assertTrue(decision.shouldScheduleListening);
        assertEquals("ready", decision.nextState);
    }

    @Test
    public void currentTextFetchReturnsReadyWithoutListeningSchedule() {
        WeatherIntentCompletionDecision decision =
                WeatherIntentCompletionDecision.notWeatherQuery(true, false, false, true);

        assertTrue(decision.shouldUpdateState);
        assertFalse(decision.shouldScheduleListening);
        assertEquals("ready", decision.nextState);
    }

    @Test
    public void textModeRealtimeFetchPreservesTextOnlyAndDoesNotScheduleListening() {
        WeatherIntentCompletionDecision decision =
                WeatherIntentCompletionDecision.notWeatherQuery(true, true, true, true);

        assertTrue(decision.shouldUpdateState);
        assertFalse(decision.shouldScheduleListening);
        assertEquals("text_only", decision.nextState);
    }

    @Test
    public void realtimeFetchOffVoiceSurfaceDoesNotScheduleListening() {
        WeatherIntentCompletionDecision decision =
                WeatherIntentCompletionDecision.notWeatherQuery(true, true, false, false);

        assertTrue(decision.shouldUpdateState);
        assertFalse(decision.shouldScheduleListening);
        assertEquals("ready", decision.nextState);
    }
}
