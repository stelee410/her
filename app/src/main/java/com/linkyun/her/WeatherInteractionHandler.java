package com.linkyun.her;

import android.location.Location;

final class WeatherInteractionHandler {
    interface Source {
        void queryCity(String city, WeatherTool.CallbackResult callback);
        void queryLocation(Location location, WeatherTool.CallbackResult callback);
    }

    interface Callback {
        void onResult(ToolInteractionResult<WeatherTool.WeatherResult> result);
    }

    private static final String TOOL = "weather";

    private final Source source;

    WeatherInteractionHandler(WeatherTool weatherTool) {
        this(weatherTool == null ? null : new Source() {
            @Override public void queryCity(String city, WeatherTool.CallbackResult callback) {
                weatherTool.queryCity(city, callback);
            }

            @Override public void queryLocation(Location location, WeatherTool.CallbackResult callback) {
                weatherTool.queryLocation(location, callback);
            }
        });
    }

    WeatherInteractionHandler(Source source) {
        this.source = source;
    }

    void queryCity(String question, String city, Callback callback) {
        String normalizedQuestion = normalizeQuestion(question);
        if (source == null) {
            callback.onResult(failure(normalizedQuestion, "天气工具不可用"));
            return;
        }
        source.queryCity(city, callbackFor(normalizedQuestion, callback));
    }

    void queryLocation(String question, Location location, Callback callback) {
        String normalizedQuestion = normalizeQuestion(question);
        if (source == null) {
            callback.onResult(failure(normalizedQuestion, "天气工具不可用"));
            return;
        }
        source.queryLocation(location, callbackFor(normalizedQuestion, callback));
    }

    void fail(String question, String message, Callback callback) {
        callback.onResult(failure(normalizeQuestion(question), message));
    }

    private static WeatherTool.CallbackResult callbackFor(String question, Callback callback) {
        return new WeatherTool.CallbackResult() {
            @Override public void onSuccess(WeatherTool.WeatherResult result) {
                callback.onResult(ToolInteractionResult.success(
                        TOOL,
                        question,
                        result.fact(question),
                        result.shortAnswer(),
                        result));
            }

            @Override public void onError(String message) {
                callback.onResult(failure(question, message));
            }
        };
    }

    private static ToolInteractionResult<WeatherTool.WeatherResult> failure(String question, String message) {
        String error = message == null ? "" : message;
        return ToolInteractionResult.failure(
                TOOL,
                question,
                WeatherSkill.failureFact(error),
                "暂时没查到天气，" + error + "。你可以稍后再试，或者告诉我具体城市。",
                error);
    }

    private static String normalizeQuestion(String question) {
        if (question == null || question.trim().isEmpty()) return "天气";
        return question.trim();
    }
}
