package com.linkyun.her;

import static org.junit.Assert.assertEquals;

import org.json.JSONObject;
import org.junit.Test;

public final class RealtimeErrorEventDecisionTest {
    @Test
    public void recoverableErrorRetriesWithMessage() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("recoverable", true);
        payload.put("message", "network hiccup");

        RealtimeErrorEventDecision decision = RealtimeErrorEventDecision.fromPayload(payload);

        assertEquals(RealtimeErrorEventDecision.Action.RETRY, decision.action);
        assertEquals("network hiccup", decision.message);
    }

    @Test
    public void fatalErrorShowsErrorState() throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("recoverable", false);
        payload.put("message", "bad session");

        RealtimeErrorEventDecision decision = RealtimeErrorEventDecision.fromPayload(payload);

        assertEquals(RealtimeErrorEventDecision.Action.ERROR, decision.action);
        assertEquals("bad session", decision.message);
    }

    @Test
    public void missingOrBlankMessageUsesDefault() throws Exception {
        assertEquals("Realtime error", RealtimeErrorEventDecision.fromPayload(null).message);

        JSONObject payload = new JSONObject();
        payload.put("message", "   ");

        RealtimeErrorEventDecision decision = RealtimeErrorEventDecision.fromPayload(payload);

        assertEquals(RealtimeErrorEventDecision.Action.ERROR, decision.action);
        assertEquals("Realtime error", decision.message);
    }
}
