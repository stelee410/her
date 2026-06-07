package com.linkyun.her;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public final class RealtimeClosedHandlerTest {
    @Test
    public void pendingToolTtsStartsAfterReadyAndStopsFurtherHandlingWhenStarted() {
        Host host = new Host();
        host.pendingToolTts = true;
        host.startToolTts = true;
        host.activeToolTts = true;
        host.summary = true;
        RealtimeClosedHandler handler = new RealtimeClosedHandler(host);

        handler.onClosed();

        assertEquals("reset", host.events.get(0));
        assertEquals("state:ready", host.events.get(1));
        assertEquals("startToolTts", host.events.get(2));
        assertEquals(3, host.events.size());
    }

    @Test
    public void activeToolTtsKeepsSpeakingWhenNoPendingStartWins() {
        Host host = new Host();
        host.pendingToolTts = true;
        host.startToolTts = false;
        host.activeToolTts = true;
        RealtimeClosedHandler handler = new RealtimeClosedHandler(host);

        handler.onClosed();

        assertEquals("state:ready", host.events.get(1));
        assertEquals("startToolTts", host.events.get(2));
        assertEquals("state:speaking", host.events.get(3));
    }

    @Test
    public void gatewayTtsKeepsSpeakingWhenRealtimeCloses() {
        Host host = new Host();
        host.gatewayTts = true;
        host.initializing = true;
        RealtimeClosedHandler handler = new RealtimeClosedHandler(host);

        handler.onClosed();

        assertEquals("reset", host.events.get(0));
        assertEquals("state:speaking", host.events.get(1));
        assertEquals(2, host.events.size());
    }

    @Test
    public void textModeStaysTextOnlyWhenRealtimeCloses() {
        Host host = new Host();
        host.textMode = true;
        RealtimeClosedHandler handler = new RealtimeClosedHandler(host);

        handler.onClosed();

        assertEquals("reset", host.events.get(0));
        assertEquals("state:text_only", host.events.get(1));
        assertEquals(2, host.events.size());
    }

    @Test
    public void textModeDoesNotStartPendingToolTtsWhenRealtimeCloses() {
        Host host = new Host();
        host.textMode = true;
        host.pendingToolTts = true;
        host.startToolTts = true;
        RealtimeClosedHandler handler = new RealtimeClosedHandler(host);

        handler.onClosed();

        assertEquals("reset", host.events.get(0));
        assertEquals("state:text_only", host.events.get(1));
        assertEquals(2, host.events.size());
    }

    @Test
    public void textModeDoesNotShowSpeakingForActiveTtsWhenRealtimeCloses() {
        Host host = new Host();
        host.textMode = true;
        host.activeToolTts = true;
        host.gatewayTts = true;
        RealtimeClosedHandler handler = new RealtimeClosedHandler(host);

        handler.onClosed();

        assertEquals("reset", host.events.get(0));
        assertEquals("state:text_only", host.events.get(1));
        assertEquals(2, host.events.size());
    }

    @Test
    public void initializingWithoutSummaryFallsBackToTextOnly() {
        Host host = new Host();
        host.initializing = true;
        RealtimeClosedHandler handler = new RealtimeClosedHandler(host);

        handler.onClosed();

        assertEquals("reset", host.events.get(0));
        assertEquals("state:text_only", host.events.get(1));
    }

    @Test
    public void summaryStateWinsOverInitializingFallback() {
        Host host = new Host();
        host.initializing = true;
        host.summary = true;
        RealtimeClosedHandler handler = new RealtimeClosedHandler(host);

        handler.onClosed();

        assertEquals("state:summarizing", host.events.get(1));
    }

    @Test
    public void idleIsDefaultClosedState() {
        Host host = new Host();
        RealtimeClosedHandler handler = new RealtimeClosedHandler(host);

        handler.onClosed();

        assertEquals("reset", host.events.get(0));
        assertEquals("state:idle", host.events.get(1));
    }

    private static final class Host implements RealtimeClosedHandler.Host {
        final List<String> events = new ArrayList<>();
        boolean pendingToolTts;
        boolean startToolTts;
        boolean activeToolTts;
        boolean gatewayTts;
        boolean textMode;
        boolean initializing;
        boolean summary;

        @Override public void resetRealtimeOutput() { events.add("reset"); }
        @Override public boolean hasPendingToolTtsPlayback() { return pendingToolTts; }
        @Override public boolean maybeStartToolTtsAfterRealtimeStopped() {
            events.add("startToolTts");
            return startToolTts;
        }
        @Override public boolean hasActiveToolTtsPlayback() { return activeToolTts; }
        @Override public boolean isGatewayTtsPlaying() { return gatewayTts; }
        @Override public boolean isTextModeActive() { return textMode; }
        @Override public boolean isInitializing() { return initializing; }
        @Override public boolean isSummaryInProgress() { return summary; }
        @Override public void setState(String nextState) { events.add("state:" + nextState); }
    }
}
