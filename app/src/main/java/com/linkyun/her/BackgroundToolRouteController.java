package com.linkyun.her;

import org.json.JSONException;
import org.json.JSONObject;

final class BackgroundToolRouteController {
    interface Sender {
        void send(JSONObject body, AgentApiClient.ReplyCallback callback);
    }

    interface Host {
        boolean hasApiKey();
        String effectiveAgentName();
        void routeBackgroundDecision(String toolId, double confidence, String text);
        void logToolRoute(String message);
    }

    private final String model;
    private final Sender sender;
    private final Host host;
    private int sequence;

    BackgroundToolRouteController(String model, Sender sender, Host host) {
        this.model = model;
        this.sender = sender;
        this.host = host;
    }

    void invalidate() {
        sequence++;
    }

    int sequence() {
        return sequence;
    }

    boolean route(String text) {
        String question = text == null ? "" : text.trim();
        if (question.isEmpty()) return false;
        if (!host.hasApiKey()) return false;
        int token = ++sequence;
        JSONObject body;
        try {
            body = BackgroundToolRouteDecider.requestBody(model, host.effectiveAgentName(), question);
        } catch (JSONException error) {
            return false;
        }
        sender.send(body, new AgentApiClient.ReplyCallback() {
            @Override public void onSuccess(String content) {
                if (token != sequence) return;
                try {
                    BackgroundToolRouteDecider.Decision decision =
                            BackgroundToolRouteDecider.parse(content);
                    host.logToolRoute("tool route tool=" + decision.tool +
                            " confidence=" + decision.confidence + " text=" + question);
                    host.routeBackgroundDecision(decision.tool, decision.confidence, question);
                } catch (JSONException error) {
                    host.logToolRoute("tool route parse failed content=" + content);
                }
            }

            @Override public void onError(String message) {
                if (token != sequence) return;
                host.logToolRoute("tool route failed " + message);
            }
        });
        return true;
    }
}
