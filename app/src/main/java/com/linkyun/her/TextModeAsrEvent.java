package com.linkyun.her;

import org.json.JSONException;
import org.json.JSONObject;

final class TextModeAsrEvent {
    static final String MODEL = "fun-asr-realtime";
    static final int SAMPLE_RATE = 16000;
    static final String FORMAT = "pcm";

    private TextModeAsrEvent() { }

    static JSONObject runTask(String taskId) throws JSONException {
        JSONObject header = header("run-task", taskId);
        JSONObject parameters = new JSONObject()
                .put("sample_rate", SAMPLE_RATE)
                .put("format", FORMAT);
        JSONObject payload = new JSONObject()
                .put("task_group", "audio")
                .put("task", "asr")
                .put("function", "recognition")
                .put("model", MODEL)
                .put("input", new JSONObject())
                .put("parameters", parameters);
        return new JSONObject()
                .put("header", header)
                .put("payload", payload);
    }

    static JSONObject finishTask(String taskId) throws JSONException {
        return new JSONObject()
                .put("header", header("finish-task", taskId))
                .put("payload", new JSONObject().put("input", new JSONObject()));
    }

    static boolean isTaskStarted(JSONObject event) {
        return "task-started".equals(headerEvent(event));
    }

    static boolean isTaskFinished(JSONObject event) {
        return "task-finished".equals(headerEvent(event));
    }

    static boolean isResultGenerated(JSONObject event) {
        return "result-generated".equals(headerEvent(event));
    }

    static String sentenceText(JSONObject event) {
        JSONObject sentence = sentence(event);
        if (sentence == null) return "";
        return sentence.optString("text", "").trim();
    }

    static boolean isFinalSentence(JSONObject event) {
        JSONObject sentence = sentence(event);
        return sentence != null && sentence.optBoolean("sentence_end", false);
    }

    private static JSONObject header(String action, String taskId) throws JSONException {
        return new JSONObject()
                .put("action", action)
                .put("task_id", taskId == null ? "" : taskId)
                .put("streaming", "duplex");
    }

    private static String headerEvent(JSONObject event) {
        if (event == null) return "";
        JSONObject header = event.optJSONObject("header");
        return header == null ? "" : header.optString("event", "");
    }

    private static JSONObject sentence(JSONObject event) {
        if (event == null) return null;
        JSONObject payload = event.optJSONObject("payload");
        if (payload == null) return null;
        JSONObject output = payload.optJSONObject("output");
        return output == null ? null : output.optJSONObject("sentence");
    }
}
