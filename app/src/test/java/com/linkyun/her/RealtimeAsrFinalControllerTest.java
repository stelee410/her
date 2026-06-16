package com.linkyun.her;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public final class RealtimeAsrFinalControllerTest {
    @Test
    public void hiddenSystemEventOnlyCancelsTimeoutAndClearsInitTrigger() {
        Host host = new Host();
        RealtimeAsrFinalController controller = new RealtimeAsrFinalController(host);

        controller.onFinalText("【系统事件】用户刚打开应用");

        assertEquals("cancelTimeout", host.events.get(0));
        assertEquals("clearIgnoreInit", host.events.get(1));
        assertEquals(2, host.events.size());
    }

    @Test
    public void normalRealtimeTextAddsUserMessageRendersAndRoutesForegroundToolFirst() {
        Host host = new Host();
        host.routeTool = true;
        RealtimeAsrFinalController controller = new RealtimeAsrFinalController(host);

        controller.onFinalText("  今天天气怎么样  ");

        assertEquals("cancelTimeout", host.events.get(0));
        assertEquals("markInteraction", host.events.get(1));
        assertEquals("user:今天天气怎么样", host.events.get(2));
        assertEquals("clearActive", host.events.get(3));
        assertEquals("render", host.events.get(4));
        assertEquals("route:今天天气怎么样:true", host.events.get(5));
        assertEquals(6, host.events.size());
    }

    @Test
    public void textModeIgnoresLateAsrFinalAndKeepsTextOnly() {
        Host host = new Host();
        host.textMode = true;
        host.initializing = true;
        host.routeTool = true;
        RealtimeAsrFinalController controller = new RealtimeAsrFinalController(host);

        controller.onFinalText("今天天气怎么样");

        assertEquals("cancelTimeout", host.events.get(0));
        assertEquals("clearActive", host.events.get(1));
        assertEquals("render", host.events.get(2));
        assertEquals("state:text_only", host.events.get(3));
        assertEquals(4, host.events.size());
    }

    @Test
    public void normalRealtimeTextFallsThroughToBackgroundRouteWhenNoForegroundToolMatches() {
        Host host = new Host();
        RealtimeAsrFinalController controller = new RealtimeAsrFinalController(host);

        controller.onFinalText("聊聊天");

        assertEquals("clearActive", host.events.get(3));
        assertEquals("route:聊聊天:true", host.events.get(5));
        assertEquals("background:聊聊天", host.events.get(6));
        assertEquals(7, host.events.size());
    }

    @Test
    public void foregroundToolDoesNotRunBackgroundRoute() {
        Host host = new Host();
        host.routeTool = true;
        RealtimeAsrFinalController controller = new RealtimeAsrFinalController(host);

        controller.onFinalText("换一个人");

        assertEquals("clearActive", host.events.get(3));
        assertEquals("route:换一个人:true", host.events.get(5));
        assertEquals(6, host.events.size());
    }

    @Test
    public void duplicateRealtimeFinalInsideWindowIsIgnored() {
        Host host = new Host();
        FakeClock clock = new FakeClock();
        RealtimeAsrFinalController controller = new RealtimeAsrFinalController(host, clock);

        controller.onFinalText("精致的，是吧？");
        controller.onFinalText("精致的，是吧？");

        assertEquals(8, host.events.size());
        assertEquals("background:精致的，是吧？", host.events.get(6));
        assertEquals("cancelTimeout", host.events.get(7));
    }

    @Test
    public void initializationAnswerSchedulesContextBeforeRenderWhenTargetNotReached() {
        Host host = new Host();
        host.initializing = true;
        host.recorded = true;
        RealtimeAsrFinalController controller = new RealtimeAsrFinalController(host);

        controller.onFinalText("我叫 Stephen");

        assertEquals("record:我叫 Stephen", host.events.get(3));
        assertEquals("scheduleInitContext", host.events.get(4));
        assertEquals("clearActive", host.events.get(5));
        assertEquals("render", host.events.get(6));
        assertEquals(7, host.events.size());
    }

    @Test
    public void initializationTargetReachedMarksSummaryAndFinishesWithoutRoutingTools() {
        Host host = new Host();
        host.initializing = true;
        host.recorded = true;
        host.reachedTarget = true;
        RealtimeAsrFinalController controller = new RealtimeAsrFinalController(host);

        controller.onFinalText("这是我的故事");

        assertEquals("markSummary", host.events.get(4));
        assertEquals("clearActive", host.events.get(5));
        assertEquals("render", host.events.get(6));
        assertEquals("finishSummary", host.events.get(7));
        assertEquals(8, host.events.size());
    }

    @Test
    public void emptyTextClearsActiveAndRendersWithoutRoutingTools() {
        Host host = new Host();
        RealtimeAsrFinalController controller = new RealtimeAsrFinalController(host);

        controller.onFinalText("   ");

        assertEquals("cancelTimeout", host.events.get(0));
        assertEquals("clearActive", host.events.get(1));
        assertEquals("render", host.events.get(2));
        assertEquals(3, host.events.size());
    }

    private static final class Host implements RealtimeAsrFinalController.Host {
        final List<String> events = new ArrayList<>();
        boolean initializing;
        boolean textMode;
        boolean recorded;
        boolean reachedTarget;
        boolean routeTool;

        @Override public boolean isInitializing() { return initializing; }
        @Override public boolean isTextModeActive() { return textMode; }
        @Override public void cancelAsrFinalTimeout() { events.add("cancelTimeout"); }
        @Override public void clearIgnoreNextInitTrigger() { events.add("clearIgnoreInit"); }
        @Override public void markConversationInteraction() { events.add("markInteraction"); }
        @Override public void addUserMessage(String text) { events.add("user:" + text); }
        @Override public boolean recordInitializationAnswer(String text) {
            events.add("record:" + text);
            return recorded;
        }
        @Override public boolean hasReachedInitializationTarget() { return reachedTarget; }
        @Override public void markInitSummaryPending() { events.add("markSummary"); }
        @Override public void scheduleInitializationContextUpdate() { events.add("scheduleInitContext"); }
        @Override public void clearActiveAssistant() { events.add("clearActive"); }
        @Override public void renderMessages() { events.add("render"); }
        @Override public void finishInitializationWithSummary() { events.add("finishSummary"); }
        @Override public boolean routeToolQuestion(String text, boolean realtimeMode) {
            events.add("route:" + text + ":" + realtimeMode);
            return routeTool;
        }
        @Override public void routeToolsInBackground(String text) { events.add("background:" + text); }
        @Override public void setState(String nextState) { events.add("state:" + nextState); }
    }

    private static final class FakeClock implements RealtimeAsrFinalController.Clock {
        long now;
        @Override public long nowMs() { return now; }
    }
}
