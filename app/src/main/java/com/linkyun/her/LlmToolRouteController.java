package com.linkyun.her;

import org.json.JSONException;
import org.json.JSONObject;

final class LlmToolRouteController {
    interface Sender {
        void send(JSONObject body, AgentApiClient.ReplyCallback callback);
    }

    interface Host {
        boolean hasApiKey();
        String effectiveAgentName();
        boolean routeToolDecision(String toolId, double confidence, String text, boolean realtimeMode);
        void sendTextWithAgentLLM(String text);
        void logToolRoute(String message);
    }

    private final String model;
    private final Sender sender;
    private final Host host;
    private int sequence;

    LlmToolRouteController(String model, Sender sender, Host host) {
        this.model = model;
        this.sender = sender;
        this.host = host;
    }

    void invalidate() {
        sequence++;
    }

    boolean routeOrSend(String text, boolean realtimeMode) {
        String question = text == null ? "" : text.trim();
        if (question.isEmpty()) return false;
        if (!host.hasApiKey()) {
            host.logToolRoute("llm tool route skipped: missing api key");
            host.sendTextWithAgentLLM(question);
            return true;
        }
        int token = ++sequence;
        JSONObject body;
        try {
            body = BackgroundToolRouteDecider.requestBody(model, host.effectiveAgentName(), question);
        } catch (JSONException error) {
            host.logToolRoute("llm tool route build failed: " + error.getMessage());
            host.sendTextWithAgentLLM(question);
            return true;
        }
        sender.send(body, new AgentApiClient.ReplyCallback() {
            @Override public void onSuccess(String content) {
                if (token != sequence) return;
                try {
                    BackgroundToolRouteDecider.Decision decision =
                            BackgroundToolRouteDecider.parse(content);
                    host.logToolRoute("llm tool route tool=" + decision.tool +
                            " confidence=" + decision.confidence + " text=" + question);
                    if (!host.routeToolDecision(decision.tool, decision.confidence, question, realtimeMode)) {
                        host.sendTextWithAgentLLM(question);
                    }
                } catch (JSONException error) {
                    host.logToolRoute("llm tool route parse failed content=" + content);
                    host.sendTextWithAgentLLM(question);
                }
            }

            @Override public void onError(String message) {
                if (token != sequence) return;
                host.logToolRoute("llm tool route failed " + message);
                host.sendTextWithAgentLLM(question);
            }
        });
        return true;
    }
}
