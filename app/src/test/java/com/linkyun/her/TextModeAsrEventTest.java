package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

public final class TextModeAsrEventTest {
    @Test
    public void runTaskUsesDashScopeRealtimeAsrPcm16k() throws Exception {
        JSONObject event = TextModeAsrEvent.runTask("task-1");

        assertEquals("run-task", event.getJSONObject("header").getString("action"));
        assertEquals("task-1", event.getJSONObject("header").getString("task_id"));
        assertEquals("duplex", event.getJSONObject("header").getString("streaming"));
        assertEquals("audio", event.getJSONObject("payload").getString("task_group"));
        assertEquals("asr", event.getJSONObject("payload").getString("task"));
        assertEquals("recognition", event.getJSONObject("payload").getString("function"));
        assertEquals("fun-asr-realtime", event.getJSONObject("payload").getString("model"));
        assertEquals(16000, event.getJSONObject("payload").getJSONObject("parameters").getInt("sample_rate"));
        assertEquals("pcm", event.getJSONObject("payload").getJSONObject("parameters").getString("format"));
    }

    @Test
    public void finishTaskUsesDashScopeFinishEnvelope() throws Exception {
        JSONObject event = TextModeAsrEvent.finishTask("task-2");

        assertEquals("finish-task", event.getJSONObject("header").getString("action"));
        assertEquals("task-2", event.getJSONObject("header").getString("task_id"));
        assertTrue(event.getJSONObject("payload").has("input"));
    }

    @Test
    public void parsesResultGeneratedSentenceTextAndFinalFlag() throws Exception {
        JSONObject partial = new JSONObject()
                .put("header", new JSONObject().put("event", "result-generated"))
                .put("payload", new JSONObject()
                        .put("output", new JSONObject()
                                .put("sentence", new JSONObject()
                                        .put("text", "  今天天气怎么样  ")
                                        .put("sentence_end", false))));
        JSONObject finished = new JSONObject(partial.toString());
        finished.getJSONObject("payload")
                .getJSONObject("output")
                .getJSONObject("sentence")
                .put("sentence_end", true);

        assertTrue(TextModeAsrEvent.isResultGenerated(partial));
        assertEquals("今天天气怎么样", TextModeAsrEvent.sentenceText(partial));
        assertFalse(TextModeAsrEvent.isFinalSentence(partial));
        assertTrue(TextModeAsrEvent.isFinalSentence(finished));
    }

    @Test
    public void identifiesTaskLifecycleEvents() throws Exception {
        assertTrue(TextModeAsrEvent.isTaskStarted(
                new JSONObject().put("header", new JSONObject().put("event", "task-started"))));
        assertTrue(TextModeAsrEvent.isTaskFinished(
                new JSONObject().put("header", new JSONObject().put("event", "task-finished"))));
        assertFalse(TextModeAsrEvent.isTaskStarted(new JSONObject()));
        assertEquals("", TextModeAsrEvent.sentenceText(new JSONObject()));
    }
}
