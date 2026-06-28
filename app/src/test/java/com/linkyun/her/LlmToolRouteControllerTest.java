package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public final class LlmToolRouteControllerTest {
    @Test
    public void routesModelToolDecision() {
        Sender sender = new Sender();
        Host host = new Host();
        LlmToolRouteController controller = new LlmToolRouteController("c-her", sender, host);

        assertTrue(controller.routeOrSend("找个视频看看", false));
        sender.callback.onSuccess("{\"tool\":\"open_tv\",\"confidence\":0.91}");

        assertEquals("route:open_tv:0.91:找个视频看看:false", host.events.get(1));
    }

    @Test
    public void fallsBackToChatWhenModelChoosesNone() {
        Sender sender = new Sender();
        Host host = new Host();
        LlmToolRouteController controller = new LlmToolRouteController("c-her", sender, host);

        assertTrue(controller.routeOrSend("今天聊点轻松的", false));
        sender.callback.onSuccess("{\"tool\":\"none\",\"confidence\":0.2}");

        assertEquals("chat:今天聊点轻松的", host.events.get(2));
    }

    private static final class Sender implements LlmToolRouteController.Sender {
        AgentApiClient.ReplyCallback callback;

        @Override public void send(JSONObject body, AgentApiClient.ReplyCallback callback) {
            this.callback = callback;
        }
    }

    private static final class Host implements LlmToolRouteController.Host {
        final List<String> events = new ArrayList<>();

        @Override public boolean hasApiKey() {
            return true;
        }

        @Override public String effectiveAgentName() {
            return "Ava";
        }

        @Override public boolean routeToolDecision(String toolId, double confidence,
                String text, boolean realtimeMode) {
            events.add("route:" + toolId + ":" + confidence + ":" + text + ":" + realtimeMode);
            return !"none".equals(toolId);
        }

        @Override public void sendTextWithAgentLLM(String text) {
            events.add("chat:" + text);
        }

        @Override public void logToolRoute(String message) {
            events.add("log:" + message);
        }
    }
}
