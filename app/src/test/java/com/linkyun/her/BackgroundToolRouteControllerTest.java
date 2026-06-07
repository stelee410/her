package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.junit.Test;

public final class BackgroundToolRouteControllerTest {
    @Test
    public void emptyTextOrMissingApiKeyDoesNotSend() {
        Host host = new Host();
        Sender sender = new Sender();
        BackgroundToolRouteController controller =
                new BackgroundToolRouteController("model", sender, host);

        assertFalse(controller.route("   "));
        host.apiKey = false;
        assertFalse(controller.route("看看新闻"));

        assertEquals(0, sender.sendCount);
    }

    @Test
    public void sendsRequestAndRoutesParsedDecision() throws Exception {
        Host host = new Host();
        host.agentName = "Luna";
        Sender sender = new Sender();
        BackgroundToolRouteController controller =
                new BackgroundToolRouteController("c-her", sender, host);

        assertTrue(controller.route("看看新闻"));

        assertEquals(1, sender.sendCount);
        JSONObject body = sender.body;
        assertEquals("c-her", body.getString("model"));
        assertEquals("看看新闻", body.getJSONArray("messages").getJSONObject(1).getString("content"));

        sender.callback.onSuccess("{\"tool\":\"daily_news\",\"confidence\":0.8,\"reason\":\"想听新闻\"}");

        assertEquals("log:tool route tool=daily_news confidence=0.8 text=看看新闻", host.events.get(0));
        assertEquals("route:daily_news:0.8:看看新闻", host.events.get(1));
    }

    @Test
    public void trimsTextBeforeRequestLoggingAndRouting() throws Exception {
        Host host = new Host();
        Sender sender = new Sender();
        BackgroundToolRouteController controller =
                new BackgroundToolRouteController("c-her", sender, host);

        assertTrue(controller.route("  看看新闻  "));

        assertEquals("看看新闻", sender.body.getJSONArray("messages").getJSONObject(1).getString("content"));

        sender.callback.onSuccess("{\"tool\":\"daily_news\",\"confidence\":0.8}");

        assertEquals("log:tool route tool=daily_news confidence=0.8 text=看看新闻", host.events.get(0));
        assertEquals("route:daily_news:0.8:看看新闻", host.events.get(1));
    }

    @Test
    public void staleDecisionIsIgnoredAfterNewRequest() {
        Host host = new Host();
        Sender sender = new Sender();
        BackgroundToolRouteController controller =
                new BackgroundToolRouteController("model", sender, host);

        controller.route("旧新闻");
        AgentApiClient.ReplyCallback oldCallback = sender.callback;
        controller.route("新新闻");

        oldCallback.onSuccess("{\"tool\":\"daily_news\",\"confidence\":1.0}");

        assertEquals(0, host.events.size());
    }

    @Test
    public void staleFailureIsIgnoredAfterNewRequest() {
        Host host = new Host();
        Sender sender = new Sender();
        BackgroundToolRouteController controller =
                new BackgroundToolRouteController("model", sender, host);

        controller.route("旧新闻");
        AgentApiClient.ReplyCallback oldCallback = sender.callback;
        controller.route("新新闻");

        oldCallback.onError("old network");

        assertEquals(0, host.events.size());
    }

    @Test
    public void explicitInvalidateDropsInflightDecision() {
        Host host = new Host();
        Sender sender = new Sender();
        BackgroundToolRouteController controller =
                new BackgroundToolRouteController("model", sender, host);

        controller.route("看看新闻");
        controller.invalidate();
        sender.callback.onSuccess("{\"tool\":\"daily_news\",\"confidence\":1.0}");

        assertEquals(0, host.events.size());
        assertEquals(2, controller.sequence());
    }

    @Test
    public void explicitInvalidateDropsInflightFailure() {
        Host host = new Host();
        Sender sender = new Sender();
        BackgroundToolRouteController controller =
                new BackgroundToolRouteController("model", sender, host);

        controller.route("看看新闻");
        controller.invalidate();
        sender.callback.onError("network");

        assertEquals(0, host.events.size());
        assertEquals(2, controller.sequence());
    }

    @Test
    public void parseFailureAndSendFailureAreLogged() {
        Host host = new Host();
        Sender sender = new Sender();
        BackgroundToolRouteController controller =
                new BackgroundToolRouteController("model", sender, host);

        controller.route("看看新闻");
        sender.callback.onSuccess("not json");
        sender.callback.onError("network");

        assertEquals("log:tool route parse failed content=not json", host.events.get(0));
        assertEquals("log:tool route failed network", host.events.get(1));
    }

    private static final class Sender implements BackgroundToolRouteController.Sender {
        int sendCount;
        JSONObject body;
        AgentApiClient.ReplyCallback callback;

        @Override public void send(JSONObject body, AgentApiClient.ReplyCallback callback) {
            sendCount++;
            this.body = body;
            this.callback = callback;
        }
    }

    private static final class Host implements BackgroundToolRouteController.Host {
        final List<String> events = new ArrayList<>();
        boolean apiKey = true;
        String agentName = "Doris";

        @Override public boolean hasApiKey() { return apiKey; }
        @Override public String effectiveAgentName() { return agentName; }
        @Override public void routeBackgroundDecision(String toolId, double confidence, String text) {
            events.add("route:" + toolId + ":" + confidence + ":" + text);
        }
        @Override public void logToolRoute(String message) { events.add("log:" + message); }
    }
}
