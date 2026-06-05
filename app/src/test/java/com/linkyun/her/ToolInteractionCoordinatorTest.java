package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public final class ToolInteractionCoordinatorTest {
    @Test
    public void realtimeNewsRunsAckBeforeFetch() {
        Host host = new Host();
        ToolInteractionCoordinator coordinator = new ToolInteractionCoordinator(host);

        coordinator.startNews("查新闻", true);

        assertEquals(ToolInteractionCoordinator.State.NEWS_ACKING, coordinator.state());
        assertTrue(coordinator.isAwaitingRealtimeNewsAnswer());
        assertEquals("start:查新闻:true", host.events.get(0));
        assertEquals("ack:查新闻", host.events.get(2));

        assertTrue(coordinator.onRealtimeOutputFinished());

        assertEquals(ToolInteractionCoordinator.State.NEWS_FETCHING, coordinator.state());
        assertEquals("fetch:查新闻:true:1", host.events.get(4));

        assertTrue(coordinator.completeNewsFetch(1));
        assertEquals(ToolInteractionCoordinator.State.IDLE, coordinator.state());
        assertFalse(coordinator.isAwaitingRealtimeNewsAnswer());
        assertEquals("completed:查新闻:true", host.events.get(5));
    }

    @Test
    public void realtimeReadyResendsAckWhenConnectionWasNotOpen() {
        Host host = new Host();
        ToolInteractionCoordinator coordinator = new ToolInteractionCoordinator(host);

        coordinator.startNews("新闻", true);
        assertTrue(coordinator.onRealtimeReady());

        assertEquals("ack:新闻", host.events.get(2));
        assertEquals("ack:新闻", host.events.get(3));
    }

    @Test
    public void textNewsFetchesImmediately() {
        Host host = new Host();
        ToolInteractionCoordinator coordinator = new ToolInteractionCoordinator(host);

        coordinator.startNews("新闻", false);

        assertEquals(ToolInteractionCoordinator.State.NEWS_FETCHING, coordinator.state());
        assertFalse(coordinator.isAwaitingRealtimeNewsAnswer());
        assertEquals("fetch:新闻:false:1", host.events.get(2));
    }

    @Test
    public void backgroundNewsFetchesWithoutAck() {
        Host host = new Host();
        ToolInteractionCoordinator coordinator = new ToolInteractionCoordinator(host);

        coordinator.startNewsFromBackground("热点");

        assertEquals(ToolInteractionCoordinator.State.NEWS_FETCHING, coordinator.state());
        assertTrue(coordinator.isAwaitingRealtimeNewsAnswer());
        assertEquals("fetch:热点:true:1", host.events.get(2));
    }

    @Test
    public void interruptInvalidatesOldFetchToken() {
        Host host = new Host();
        ToolInteractionCoordinator coordinator = new ToolInteractionCoordinator(host);

        coordinator.startNews("新闻", false);
        coordinator.interruptNews();

        assertEquals(ToolInteractionCoordinator.State.IDLE, coordinator.state());
        assertFalse(coordinator.completeNewsFetch(1));
        assertEquals("interrupted:新闻:false", host.events.get(3));
    }

    @Test
    public void realtimeWeatherFetchesImmediatelyAndMarksAwaitingAnswer() {
        Host host = new Host();
        ToolInteractionCoordinator coordinator = new ToolInteractionCoordinator(host);

        coordinator.startWeather("深圳天气", true);

        assertEquals(ToolInteractionCoordinator.State.WEATHER_FETCHING, coordinator.state());
        assertTrue(coordinator.isWeatherActive());
        assertTrue(coordinator.isAwaitingRealtimeWeatherAnswer());
        assertEquals("weatherStart:深圳天气:true", host.events.get(0));
        assertEquals("weatherFetch:深圳天气:true:1", host.events.get(2));

        assertTrue(coordinator.completeWeatherFetch(1));
        assertEquals(ToolInteractionCoordinator.State.IDLE, coordinator.state());
        assertFalse(coordinator.isAwaitingRealtimeWeatherAnswer());
        assertEquals("weatherCompleted:深圳天气:true", host.events.get(3));
    }

    @Test
    public void weatherStartInvalidatesPendingNewsFetch() {
        Host host = new Host();
        ToolInteractionCoordinator coordinator = new ToolInteractionCoordinator(host);

        coordinator.startNews("新闻", false);
        coordinator.startWeather("天气", false);

        assertFalse(coordinator.completeNewsFetch(1));
        assertTrue(coordinator.completeWeatherFetch(2));
        assertEquals("weatherCompleted:天气:false", host.events.get(6));
    }

    @Test
    public void interruptWeatherInvalidatesOldFetchToken() {
        Host host = new Host();
        ToolInteractionCoordinator coordinator = new ToolInteractionCoordinator(host);

        coordinator.startWeather("天气", true);
        coordinator.interruptWeather();

        assertEquals(ToolInteractionCoordinator.State.IDLE, coordinator.state());
        assertFalse(coordinator.completeWeatherFetch(1));
        assertEquals("weatherInterrupted:天气:true", host.events.get(3));
    }

    private static final class Host implements ToolInteractionCoordinator.Host {
        final List<String> events = new ArrayList<>();

        @Override public void onNewsStarted(String question, boolean realtimeMode) {
            events.add("start:" + question + ":" + realtimeMode);
        }

        @Override public void startNewsAck(String question) {
            events.add("ack:" + question);
        }

        @Override public void startNewsFetch(String question, boolean realtimeMode, int token) {
            events.add("fetch:" + question + ":" + realtimeMode + ":" + token);
        }

        @Override public void onNewsCompleted(String question, boolean realtimeMode) {
            events.add("completed:" + question + ":" + realtimeMode);
        }

        @Override public void onNewsInterrupted(String question, boolean realtimeMode) {
            events.add("interrupted:" + question + ":" + realtimeMode);
        }

        @Override public void onWeatherStarted(String question, boolean realtimeMode) {
            events.add("weatherStart:" + question + ":" + realtimeMode);
        }

        @Override public void startWeatherFetch(String question, boolean realtimeMode, int token) {
            events.add("weatherFetch:" + question + ":" + realtimeMode + ":" + token);
        }

        @Override public void onWeatherCompleted(String question, boolean realtimeMode) {
            events.add("weatherCompleted:" + question + ":" + realtimeMode);
        }

        @Override public void onWeatherInterrupted(String question, boolean realtimeMode) {
            events.add("weatherInterrupted:" + question + ":" + realtimeMode);
        }

        @Override public void logToolInteraction(String message) {
            events.add("log:" + message);
        }
    }
}
