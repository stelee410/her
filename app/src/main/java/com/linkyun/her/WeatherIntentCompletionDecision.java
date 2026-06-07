package com.linkyun.her;

final class WeatherIntentCompletionDecision {
    final boolean shouldUpdateState;
    final boolean shouldScheduleListening;
    final String nextState;

    private WeatherIntentCompletionDecision(boolean shouldUpdateState,
            boolean shouldScheduleListening,
            String nextState) {
        this.shouldUpdateState = shouldUpdateState;
        this.shouldScheduleListening = shouldScheduleListening;
        this.nextState = nextState;
    }

    static WeatherIntentCompletionDecision notWeatherQuery(boolean currentFetchCompleted,
            boolean realtimeMode,
            boolean textModeActive,
            boolean voiceInputSurfaceActive) {
        if (!currentFetchCompleted) {
            return new WeatherIntentCompletionDecision(false, false, null);
        }
        return new WeatherIntentCompletionDecision(
                true,
                realtimeMode && !textModeActive && voiceInputSurfaceActive,
                textModeActive ? "text_only" : "ready");
    }
}
