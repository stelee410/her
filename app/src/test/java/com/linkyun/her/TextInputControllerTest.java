package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public final class TextInputControllerTest {
    @Test
    public void ignoresEmptyOrSummaryInput() {
        Host host = new Host();
        TextInputController controller = new TextInputController(host);

        assertFalse(controller.sendText(""));
        assertFalse(controller.sendText(" \t "));
        host.summary = true;
        assertFalse(controller.sendText("hello"));

        assertEquals(0, host.events.size());
    }

    @Test
    public void trimsTextBeforePersistingRoutingAndSending() {
        Host host = new Host();
        TextInputController controller = new TextInputController(host);

        assertTrue(controller.sendText("  hello  "));

        assertEquals("user:hello", host.events.get(1));
        assertEquals("routeOrSend:hello", host.events.get(4));
    }

    @Test
    public void normalTextAddsUserRendersAndSendsChat() {
        Host host = new Host();
        TextInputController controller = new TextInputController(host);

        assertTrue(controller.sendText("hello"));

        assertEquals("mark", host.events.get(0));
        assertEquals("user:hello", host.events.get(1));
        assertEquals("clearAssistant", host.events.get(2));
        assertEquals("render", host.events.get(3));
        assertEquals("routeOrSend:hello", host.events.get(4));
    }

    @Test
    public void routedToolStopsBeforeTextChat() {
        Host host = new Host();
        TextInputController controller = new TextInputController(host);

        assertTrue(controller.sendText("查一下新闻"));

        assertEquals("routeOrSend:查一下新闻", host.events.get(4));
        assertEquals(5, host.events.size());
    }

    @Test
    public void initializingAnswerSchedulesContextAndStillSendsToChatModel() {
        Host host = new Host();
        host.initializing = true;
        host.recorded = true;
        TextInputController controller = new TextInputController(host);

        assertTrue(controller.sendText("我叫史蒂芬"));

        assertEquals("record:我叫史蒂芬", host.events.get(2));
        assertEquals("scheduleInitContext", host.events.get(3));
        assertEquals("clearAssistant", host.events.get(4));
        assertEquals("render", host.events.get(5));
        assertEquals("chat:我叫史蒂芬", host.events.get(6));
    }

    @Test
    public void completingInitializationRendersAndStartsSummaryWithoutChat() {
        Host host = new Host();
        host.initializing = true;
        host.recorded = true;
        host.reachedTarget = true;
        TextInputController controller = new TextInputController(host);

        assertTrue(controller.sendText("这是我的故事"));

        assertEquals("record:这是我的故事", host.events.get(2));
        assertEquals("render", host.events.get(3));
        assertEquals("finishSummary", host.events.get(4));
        assertEquals(5, host.events.size());
    }

    private static final class Host implements TextInputController.Host {
        final List<String> events = new ArrayList<>();
        boolean summary;
        boolean initializing;
        boolean recorded;
        boolean reachedTarget;
        boolean routeTool;

        @Override public boolean isSummaryInProgress() { return summary; }
        @Override public boolean isInitializing() { return initializing; }
        @Override public void markConversationInteraction() { events.add("mark"); }
        @Override public void addUserMessage(String text) { events.add("user:" + text); }
        @Override public boolean recordInitializationAnswer(String text) {
            events.add("record:" + text);
            return recorded;
        }
        @Override public boolean hasReachedInitializationTarget() { return reachedTarget; }
        @Override public void scheduleInitializationContextUpdate() { events.add("scheduleInitContext"); }
        @Override public void clearActiveAssistant() { events.add("clearAssistant"); }
        @Override public void renderMessages() { events.add("render"); }
        @Override public void finishInitializationWithSummary() { events.add("finishSummary"); }
        @Override public void routeOrSendText(String text) { events.add("routeOrSend:" + text); }
        @Override public void sendTextWithAgentLLM(String text) { events.add("chat:" + text); }
    }
}
