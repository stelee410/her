package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

public final class RealtimeMemorySnapshotTest {
    @Test
    public void buildsSnapshotFromInstructionsAndDialogue() throws JSONException {
        JSONObject payload = new JSONObject();
        payload.put("instructions", "abcdef");
        JSONArray dialogue = new JSONArray();
        dialogue.put(new JSONObject().put("role", "user").put("text", "你好"));
        dialogue.put("skip");
        dialogue.put(new JSONObject().put("role", "assistant").put("text", "我在"));
        payload.put("dialogue", dialogue);

        String snapshot = RealtimeMemorySnapshot.fromPayload(payload, 4);

        assertEquals("## AgentVoice working instructions\n" +
                "cdef\n" +
                "## AgentVoice recent dialogue\n" +
                "user: 你好\n" +
                "assistant: 我在", snapshot);
        assertTrue(RealtimeMemorySnapshot.shouldPersist(snapshot));
    }

    @Test
    public void emptyPayloadProducesNoPersistentSnapshot() {
        String snapshot = RealtimeMemorySnapshot.fromPayload(new JSONObject(), 1400);

        assertEquals("", snapshot);
        assertFalse(RealtimeMemorySnapshot.shouldPersist(snapshot));
        assertFalse(RealtimeMemorySnapshot.shouldPersist(null));
    }

    @Test
    public void weatherTransientSnapshotIsFiltered() throws JSONException {
        JSONObject payload = new JSONObject();
        payload.put("instructions", "当前临时工具结果：\n【天气查询结果】上海晴。");

        String snapshot = RealtimeMemorySnapshot.fromPayload(payload, 1400);

        assertFalse(RealtimeMemorySnapshot.shouldPersist(snapshot));
    }
}
