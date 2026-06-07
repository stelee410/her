package com.linkyun.her;

import static org.junit.Assert.assertEquals;

import org.json.JSONException;
import org.junit.Test;

public final class ChatControllerTest {
    @Test
    public void missingApiKeyShowsErrorAndDoesNotSend() {
        Host host = new Host();
        host.apiKey = false;
        Sender sender = new Sender();
        ChatController controller = new ChatController("model", sender, host);

        controller.sendText("hello");

        assertEquals(0, sender.sendCount);
        assertEquals("toast:Missing AGENTLLM_API_KEY in local.properties", host.events.get(0));
        assertEquals("state:error", host.events.get(1));
    }

    @Test
    public void sendFailureMovesToError() {
        Host host = new Host();
        Sender sender = new Sender();
        ChatController controller = new ChatController("model", sender, host);

        controller.sendText("hello");
        sender.callback.onError("boom");

        assertEquals("state:processing", host.events.get(0));
        assertEquals("showPlaceholder", host.events.get(1));
        assertEquals("hidePlaceholder", host.events.get(2));
        assertEquals("toast:boom", host.events.get(3));
        assertEquals("state:error", host.events.get(4));
    }

    @Test
    public void successfulReplyAddsMessageRendersAndReturnsReady() {
        Host host = new Host();
        Sender sender = new Sender();
        ChatController controller = new ChatController("model", sender, host);

        controller.sendText("hello");
        sender.callback.onSuccess("hi");

        assertEquals("state:processing", host.events.get(0));
        assertEquals("showPlaceholder", host.events.get(1));
        assertEquals("hidePlaceholder", host.events.get(2));
        assertEquals("breathe", host.events.get(3));
        assertEquals("assistant:hi", host.events.get(4));
        assertEquals("render", host.events.get(5));
        assertEquals("state:ready", host.events.get(6));
    }

    @Test
    public void staleSuccessFromEarlierTextIsIgnored() {
        Host host = new Host();
        Sender sender = new Sender();
        ChatController controller = new ChatController("model", sender, host);

        controller.sendText("first");
        AgentApiClient.ReplyCallback first = sender.callback;
        controller.sendText("second");

        first.onSuccess("old reply");
        sender.callback.onSuccess("new reply");

        assertEquals("state:processing", host.events.get(0));
        assertEquals("showPlaceholder", host.events.get(1));
        assertEquals("state:processing", host.events.get(2));
        assertEquals("showPlaceholder", host.events.get(3));
        assertEquals("assistant:new reply", host.events.get(6));
        assertEquals("state:ready", host.events.get(8));
        assertEquals(9, host.events.size());
    }

    @Test
    public void staleErrorFromEarlierTextIsIgnored() {
        Host host = new Host();
        Sender sender = new Sender();
        ChatController controller = new ChatController("model", sender, host);

        controller.sendText("first");
        AgentApiClient.ReplyCallback first = sender.callback;
        controller.sendText("second");

        first.onError("old boom");
        sender.callback.onSuccess("new reply");

        assertEquals("assistant:new reply", host.events.get(6));
        assertEquals("state:ready", host.events.get(8));
        assertEquals(9, host.events.size());
    }

    @Test
    public void initializingReplyUpdatesInitProgressBeforeReady() {
        Host host = new Host();
        host.initializing = true;
        Sender sender = new Sender();
        ChatController controller = new ChatController("model", sender, host);

        controller.sendText("hello");
        sender.callback.onSuccess("hi");

        assertEquals("updateInit", host.events.get(6));
        assertEquals("state:ready", host.events.get(7));
    }

    @Test
    public void emptyReplyShowsError() {
        Host host = new Host();
        Sender sender = new Sender();
        ChatController controller = new ChatController("model", sender, host);

        controller.sendText("hello");
        sender.callback.onSuccess("");

        assertEquals("hidePlaceholder", host.events.get(2));
        assertEquals("toast:文本聊天返回为空", host.events.get(3));
        assertEquals("state:error", host.events.get(4));
    }

    @Test
    public void instructionBuildFailureShowsError() {
        Host host = new Host();
        host.failInstructions = true;
        Sender sender = new Sender();
        ChatController controller = new ChatController("model", sender, host);

        controller.sendText("hello");

        assertEquals(0, sender.sendCount);
        assertEquals("state:processing", host.events.get(0));
        assertEquals("showPlaceholder", host.events.get(1));
        assertEquals("hidePlaceholder", host.events.get(2));
        assertEquals("toast:构建文本聊天请求失败", host.events.get(3));
        assertEquals("state:error", host.events.get(4));
    }

    private static final class Sender implements ChatController.Sender {
        int sendCount;
        String model;
        String instructions;
        String text;
        AgentApiClient.ReplyCallback callback;

        @Override public void sendChat(String model, String instructions, String text,
                AgentApiClient.ReplyCallback callback) {
            sendCount++;
            this.model = model;
            this.instructions = instructions;
            this.text = text;
            this.callback = callback;
        }
    }

    private static final class Host implements ChatController.Host {
        final java.util.List<String> events = new java.util.ArrayList<>();
        boolean apiKey = true;
        boolean initializing;
        boolean failInstructions;

        @Override public boolean hasApiKey() { return apiKey; }

        @Override public String buildTextChatInstructions() throws JSONException {
            if (failInstructions) throw new JSONException("bad");
            return "instructions";
        }

        @Override public void setState(String nextState) { events.add("state:" + nextState); }
        @Override public void toastError(String message) { events.add("toast:" + message); }
        @Override public void showReplyPlaceholder() { events.add("showPlaceholder"); }
        @Override public void hideReplyPlaceholder() { events.add("hidePlaceholder"); }
        @Override public void breatheScreenForAssistantReply() { events.add("breathe"); }
        @Override public void addAssistantMessage(String text) { events.add("assistant:" + text); }
        @Override public void renderMessages() { events.add("render"); }
        @Override public boolean isInitializing() { return initializing; }
        @Override public void updateInitProgress() { events.add("updateInit"); }
    }
}
