package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public final class HeadsetControllerTest {
    @Test
    public void ignoresNonDownAndRepeatedNonClickMediaButtonEvents() {
        Host host = new Host();
        HeadsetController controller = new HeadsetController(host);

        assertFalse(controller.onMediaButton(HeadsetController.MediaButton.PLAY_PAUSE, false, 0, 1000));
        assertFalse(controller.onMediaButton(HeadsetController.MediaButton.NEXT, true, 1, 1000));

        assertEquals(0, host.events.size());
    }

    @Test
    public void longPressWakesVoiceOnceUntilReleased() {
        Host host = new Host();
        HeadsetController controller = new HeadsetController(host);

        assertTrue(controller.onMediaButton(HeadsetController.MediaButton.HOOK, true, 1, 1000));
        assertFalse(controller.onMediaButton(HeadsetController.MediaButton.HOOK, true, 2, 1100));
        assertFalse(controller.onMediaButton(HeadsetController.MediaButton.HOOK, false, 0, 1200));
        assertTrue(controller.onMediaButton(HeadsetController.MediaButton.HOOK, true, 1, 1300));

        assertEquals("wakeVoice", host.events.get(0));
        assertEquals("wakeVoice", host.events.get(1));
        assertEquals(2, host.events.size());
    }

    @Test
    public void doubleTapInterruptsCurrentConversation() {
        Host host = new Host();
        HeadsetController controller = new HeadsetController(host);

        assertTrue(controller.onMediaButton(HeadsetController.MediaButton.PLAY_PAUSE, true, 0, 1000));
        assertTrue(controller.onMediaButton(HeadsetController.MediaButton.PLAY_PAUSE, true, 0, 1400));

        assertEquals("mark", host.events.get(0));
        assertEquals("interrupt:headset_double_tap", host.events.get(1));
        assertEquals("stopTool:true", host.events.get(2));
        assertEquals("persistClear", host.events.get(3));
        assertEquals("clearNews", host.events.get(4));
        assertEquals("clearWeather", host.events.get(5));
        assertEquals("state:ready", host.events.get(6));
    }

    @Test
    public void doubleTapInTextModeInterruptsWithoutResumingListening() {
        Host host = new Host();
        host.textMode = true;
        HeadsetController controller = new HeadsetController(host);

        controller.onMediaButton(HeadsetController.MediaButton.PLAY_PAUSE, true, 0, 1000);
        controller.onMediaButton(HeadsetController.MediaButton.PLAY_PAUSE, true, 0, 1400);

        assertEquals("stopTool:false", host.events.get(2));
        assertEquals("state:text_only", host.events.get(6));
    }

    @Test
    public void firstClickInsideWindowDoesNotInterrupt() {
        Host host = new Host();
        HeadsetController controller = new HeadsetController(host);

        assertTrue(controller.onMediaButton(HeadsetController.MediaButton.PLAY_PAUSE, true, 0, 100));

        assertEquals(0, host.events.size());
    }

    @Test
    public void nextButtonInterruptsImmediatelyAndStopsInputIfActive() {
        Host host = new Host();
        host.inputActive = true;
        HeadsetController controller = new HeadsetController(host);

        assertTrue(controller.onMediaButton(HeadsetController.MediaButton.NEXT, true, 0, 1000));

        assertEquals("stopInput:ready", host.events.get(6));
    }

    @Test
    public void immediateInterruptInTextModeStopsInputAsTextOnly() {
        Host host = new Host();
        host.inputActive = true;
        host.textMode = true;
        HeadsetController controller = new HeadsetController(host);

        assertTrue(controller.onMediaButton(HeadsetController.MediaButton.NEXT, true, 0, 1000));

        assertEquals("stopTool:false", host.events.get(2));
        assertEquals("stopInput:text_only", host.events.get(6));
    }

    @Test
    public void immediateInterruptClearsPendingTransportClick() {
        Host host = new Host();
        HeadsetController controller = new HeadsetController(host);

        assertTrue(controller.onMediaButton(HeadsetController.MediaButton.PLAY_PAUSE, true, 0, 1000));
        assertTrue(controller.onMediaButton(HeadsetController.MediaButton.NEXT, true, 0, 1200));
        assertTrue(controller.onMediaButton(HeadsetController.MediaButton.PLAY_PAUSE, true, 0, 1300));

        assertEquals(7, host.events.size());
        assertEquals("state:ready", host.events.get(6));
    }

    @Test
    public void headsetDisconnectStopsActiveInputAndRefreshesControls() {
        Host host = new Host();
        host.inputActive = true;
        HeadsetController controller = new HeadsetController(host);

        controller.onHeadsetDevicesChanged(false);

        assertEquals("stopInput:text_only", host.events.get(0));
        assertEquals("toast:耳机已断开，语音已暂停。", host.events.get(1));
        assertEquals("refresh", host.events.get(2));
    }

    @Test
    public void headsetDisconnectClearsPendingTransportClick() {
        Host host = new Host();
        host.inputActive = true;
        HeadsetController controller = new HeadsetController(host);

        controller.onTransportClick(1000);
        controller.onHeadsetDevicesChanged(false);
        controller.onTransportClick(1200);

        assertEquals(3, host.events.size());
        assertEquals("refresh", host.events.get(2));
    }

    @Test
    public void enablingDemoModeReconnectsRealtimeWhenNeeded() {
        Host host = new Host();
        HeadsetController controller = new HeadsetController(host);

        controller.onDemoModeChanged(true, true, false, false);

        assertEquals("refresh", host.events.get(0));
        assertEquals("connectRealtime", host.events.get(1));
    }

    @Test
    public void enablingDemoModeInTextModeDoesNotReconnectRealtime() {
        Host host = new Host();
        host.textMode = true;
        HeadsetController controller = new HeadsetController(host);

        controller.onDemoModeChanged(true, true, false, false);

        assertEquals(1, host.events.size());
        assertEquals("refresh", host.events.get(0));
    }

    @Test
    public void disablingDemoModeWithoutBoundHeadsetStopsInput() {
        Host host = new Host();
        host.inputActive = true;
        HeadsetController controller = new HeadsetController(host);

        controller.onDemoModeChanged(false, true, true, false);

        assertEquals("stopInput:text_only", host.events.get(0));
        assertEquals("toast:演示模式已关闭，语音需要已绑定耳机在线。", host.events.get(1));
    }

    @Test
    public void disablingDemoModeWithoutHeadsetClearsPendingTransportClick() {
        Host host = new Host();
        host.inputActive = true;
        HeadsetController controller = new HeadsetController(host);

        controller.onTransportClick(1000);
        controller.onDemoModeChanged(false, true, true, false);
        controller.onTransportClick(1200);

        assertEquals(2, host.events.size());
        assertEquals("toast:演示模式已关闭，语音需要已绑定耳机在线。", host.events.get(1));
    }

    private static final class Host implements HeadsetController.Host {
        final List<String> events = new ArrayList<>();
        boolean inputActive;
        boolean textMode;

        @Override public void markConversationInteraction() { events.add("mark"); }
        @Override public void interruptRealtimePlayback(String reason) { events.add("interrupt:" + reason); }
        @Override public void stopToolTtsPlayback(boolean resumeListening) { events.add("stopTool:" + resumeListening); }
        @Override public void persistAndClearActiveAssistantMessage() { events.add("persistClear"); }
        @Override public void clearNewsInteraction() { events.add("clearNews"); }
        @Override public void clearWeatherInteraction() { events.add("clearWeather"); }
        @Override public boolean isTextModeActive() { return textMode; }
        @Override public boolean isInputActive() { return inputActive; }
        @Override public void stopInputAudio(String nextState) { events.add("stopInput:" + nextState); inputActive = false; }
        @Override public void setState(String nextState) { events.add("state:" + nextState); }
        @Override public void toast(String message) { events.add("toast:" + message); }
        @Override public void refreshVoiceControls() { events.add("refresh"); }
        @Override public void connectRealtime() { events.add("connectRealtime"); }
        @Override public void wakeVoiceFromHeadsetLongPress() { events.add("wakeVoice"); }
    }
}
