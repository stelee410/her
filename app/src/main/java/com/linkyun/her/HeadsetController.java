package com.linkyun.her;

final class HeadsetController {
    static final long DOUBLE_TAP_MS = 520;

    enum MediaButton {
        NEXT,
        HOOK,
        PLAY_PAUSE,
        PLAY,
        PAUSE,
        OTHER
    }

    interface Host {
        void markConversationInteraction();
        void interruptRealtimePlayback(String reason);
        void stopToolTtsPlayback(boolean resumeListening);
        void persistAndClearActiveAssistantMessage();
        void clearNewsInteraction();
        void clearWeatherInteraction();
        boolean isTextModeActive();
        boolean isInputActive();
        void stopInputAudio(String nextState);
        void setState(String nextState);
        void toast(String message);
        void refreshVoiceControls();
        void connectRealtime();
        void wakeVoiceFromHeadsetLongPress();
    }

    private final Host host;
    private boolean hasPendingClick;
    private long lastClickAt;
    private boolean longPressHandled;

    HeadsetController(Host host) {
        this.host = host;
    }

    boolean onMediaButton(MediaButton button, boolean actionDown, int repeatCount, long nowMs) {
        if (!actionDown) {
            if (isClickButton(button)) longPressHandled = false;
            return false;
        }
        if (repeatCount > 0) {
            if (!isClickButton(button) || longPressHandled) return false;
            longPressHandled = true;
            clearPendingClick();
            host.wakeVoiceFromHeadsetLongPress();
            return true;
        }
        if (button == MediaButton.NEXT) {
            interruptCurrentConversation();
            return true;
        }
        if (!isClickButton(button)) return false;
        onTransportClick(nowMs);
        return true;
    }

    void onTransportClick(long nowMs) {
        if (hasPendingClick && nowMs >= lastClickAt && nowMs - lastClickAt <= DOUBLE_TAP_MS) {
            clearPendingClick();
            interruptCurrentConversation();
        } else {
            hasPendingClick = true;
            lastClickAt = nowMs;
        }
    }

    void interruptCurrentConversation() {
        clearPendingClick();
        boolean textModeActive = host.isTextModeActive();
        String nextState = textModeActive ? "text_only" : "ready";
        host.markConversationInteraction();
        host.interruptRealtimePlayback("headset_double_tap");
        host.stopToolTtsPlayback(!textModeActive);
        host.persistAndClearActiveAssistantMessage();
        host.clearNewsInteraction();
        host.clearWeatherInteraction();
        if (host.isInputActive()) {
            host.stopInputAudio(nextState);
        } else {
            host.setState(nextState);
        }
    }

    void onHeadsetDevicesChanged(boolean boundHeadsetConnected) {
        if (!boundHeadsetConnected && host.isInputActive()) {
            clearPendingClick();
            host.stopInputAudio("text_only");
            host.toast("耳机已断开，语音已暂停。");
        }
        host.refreshVoiceControls();
    }

    void onDemoModeChanged(boolean demoModeEnabled,
            boolean initializedOrInitializing,
            boolean realtimeOpen,
            boolean boundHeadsetConnected) {
        if (!demoModeEnabled && !boundHeadsetConnected && host.isInputActive()) {
            clearPendingClick();
            host.stopInputAudio("text_only");
            host.toast("演示模式已关闭，语音需要已绑定耳机在线。");
            return;
        }
        host.refreshVoiceControls();
        if (demoModeEnabled && initializedOrInitializing && !realtimeOpen
                && !host.isTextModeActive()) {
            host.connectRealtime();
        }
    }

    private static boolean isClickButton(MediaButton button) {
        return button == MediaButton.HOOK ||
                button == MediaButton.PLAY_PAUSE ||
                button == MediaButton.PLAY ||
                button == MediaButton.PAUSE;
    }

    private void clearPendingClick() {
        hasPendingClick = false;
        lastClickAt = 0;
    }
}
