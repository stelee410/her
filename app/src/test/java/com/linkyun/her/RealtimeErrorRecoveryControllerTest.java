package com.linkyun.her;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public final class RealtimeErrorRecoveryControllerTest {
    @Test
    public void transportErrorOutsideInitializationMovesToError() {
        Host host = new Host();
        RealtimeErrorRecoveryController controller = new RealtimeErrorRecoveryController(host);

        controller.onTransportError("socket closed");

        assertEquals("state:error", host.events.get(0));
        assertEquals("toast:socket closed", host.events.get(1));
        assertEquals(2, host.events.size());
    }

    @Test
    public void transportErrorDuringInitializationDegradesToTextMode() {
        Host host = new Host();
        host.initializing = true;
        RealtimeErrorRecoveryController controller = new RealtimeErrorRecoveryController(host);

        controller.onTransportError("boom");

        assertEquals("log:boom", host.events.get(0));
        assertEquals("stopMic", host.events.get(1));
        assertEquals("inputClosed", host.events.get(2));
        assertEquals("clearVoiceInput", host.events.get(3));
        assertEquals("clearInitPrompt", host.events.get(4));
        assertEquals("stopAudio", host.events.get(5));
        assertEquals("closeRealtime", host.events.get(6));
        assertEquals("state:text_only", host.events.get(7));
        assertEquals("updateInit", host.events.get(8));
        assertEquals("toast:语音服务暂时不可用，我们先用文字继续初始化。", host.events.get(9));
        assertEquals(0, controller.retryCount());
    }

    @Test
    public void textModeTransportErrorPreservesTextOnlyWithoutToast() {
        Host host = new Host();
        host.textMode = true;
        RealtimeErrorRecoveryController controller = new RealtimeErrorRecoveryController(host);

        controller.onTransportError("stale socket");

        assertEquals("state:text_only", host.events.get(0));
        assertEquals(1, host.events.size());
        assertEquals(0, controller.retryCount());
    }

    @Test
    public void recoverableErrorSchedulesReconnectWhenRealtimeStaysClosed() {
        Host host = new Host();
        RealtimeErrorRecoveryController controller = new RealtimeErrorRecoveryController(host);

        controller.retry("timeout");

        assertEquals("toast:语音交互模型连接超时，正在重试 1/2...", host.events.get(0));
        assertEquals("state:connecting", host.events.get(1));
        assertEquals("closeRealtime", host.events.get(2));
        assertEquals("postDelayed:1200", host.events.get(3));
        assertEquals(1, controller.retryCount());

        host.scheduled.run();

        assertEquals("connectRealtime", host.events.get(4));
    }

    @Test
    public void scheduledReconnectDoesNothingWhenRealtimeAlreadyOpen() {
        Host host = new Host();
        RealtimeErrorRecoveryController controller = new RealtimeErrorRecoveryController(host);

        controller.retry("timeout");
        host.realtimeOpen = true;
        host.scheduled.run();

        assertEquals(4, host.events.size());
    }

    @Test
    public void textModeRecoverableErrorPreservesTextOnlyAndInvalidatesReconnects() {
        Host host = new Host();
        RealtimeErrorRecoveryController controller = new RealtimeErrorRecoveryController(host);
        controller.retry("first");
        Runnable staleReconnect = host.scheduled;

        host.textMode = true;
        controller.retry("stale");
        staleReconnect.run();

        assertEquals(0, controller.retryCount());
        assertEquals("state:text_only", host.events.get(4));
        assertEquals(5, host.events.size());
    }

    @Test
    public void thirdRecoverableErrorFallsBackToTextMode() {
        Host host = new Host();
        RealtimeErrorRecoveryController controller = new RealtimeErrorRecoveryController(host);

        controller.retry("one");
        controller.retry("two");
        controller.retry("three");

        assertEquals(3, controller.retryCount());
        assertEquals("toast:语音交互模型暂时不可用，已切到文字聊天：three",
                host.events.get(host.events.size() - 2));
        assertEquals("state:text_only", host.events.get(host.events.size() - 1));
    }

    @Test
    public void resetRetryCountAllowsFreshRetryWindow() {
        Host host = new Host();
        RealtimeErrorRecoveryController controller = new RealtimeErrorRecoveryController(host);
        controller.retry("one");
        controller.retry("two");

        controller.resetRetryCount();
        controller.retry("fresh");

        assertEquals(1, controller.retryCount());
        assertEquals("toast:语音交互模型连接超时，正在重试 1/2...",
                host.events.get(host.events.size() - 4));
    }

    @Test
    public void fallbackInvalidatesPreviouslyScheduledReconnects() {
        Host host = new Host();
        RealtimeErrorRecoveryController controller = new RealtimeErrorRecoveryController(host);

        controller.retry("one");
        Runnable firstReconnect = host.scheduled;
        controller.retry("two");
        Runnable secondReconnect = host.scheduled;
        controller.retry("three");

        firstReconnect.run();
        secondReconnect.run();

        assertEquals(3, controller.retryCount());
        assertEquals("state:text_only", host.events.get(host.events.size() - 1));
    }

    @Test
    public void resetInvalidatesPreviouslyScheduledReconnects() {
        Host host = new Host();
        RealtimeErrorRecoveryController controller = new RealtimeErrorRecoveryController(host);

        controller.retry("one");
        Runnable staleReconnect = host.scheduled;
        controller.resetRetryCount();
        staleReconnect.run();

        assertEquals(0, controller.retryCount());
        assertEquals("postDelayed:1200", host.events.get(host.events.size() - 1));
    }

    private static final class Host implements RealtimeErrorRecoveryController.Host {
        final List<String> events = new ArrayList<>();
        boolean initializing;
        boolean textMode;
        boolean realtimeOpen;
        Runnable scheduled;

        @Override public boolean isInitializing() { return initializing; }
        @Override public boolean isTextModeActive() { return textMode; }
        @Override public void logInitializationDegraded(String reason) { events.add("log:" + reason); }
        @Override public void stopMic() { events.add("stopMic"); }
        @Override public void markInputAudioClosed() { events.add("inputClosed"); }
        @Override public void clearVoiceInputRequests() { events.add("clearVoiceInput"); }
        @Override public void clearInitPromptPending() { events.add("clearInitPrompt"); }
        @Override public void stopRealtimeAudio() { events.add("stopAudio"); }
        @Override public void closeRealtime() {
            realtimeOpen = false;
            events.add("closeRealtime");
        }
        @Override public boolean isRealtimeOpen() { return realtimeOpen; }
        @Override public void connectRealtime() {
            realtimeOpen = true;
            events.add("connectRealtime");
        }
        @Override public void postDelayed(Runnable runnable, long delayMs) {
            scheduled = runnable;
            events.add("postDelayed:" + delayMs);
        }
        @Override public void setState(String nextState) { events.add("state:" + nextState); }
        @Override public void updateInitProgress() { events.add("updateInit"); }
        @Override public void toastError(String message) { events.add("toast:" + message); }
    }
}
