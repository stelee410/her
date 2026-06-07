package com.linkyun.her;

final class VoiceSessionOrchestrator {
    interface Host {
        boolean isTextModeActive();
        boolean isVoiceSurfaceActive();
        boolean isSummaryInProgress();
        boolean hasActiveToolTtsPlayback();
        boolean isGatewayTtsPlaying();
        boolean hasNewsInterruptionAvailable();
        boolean hasWeatherInterruptionAvailable();
        boolean isInputActive();
        void markConversationInteraction();
        void stopToolTtsPlayback(boolean interrupt);
        void setState(String nextState);
        void clearPendingNewsBroadcast();
        void clearPendingWeatherBroadcast();
        void interruptNewsInteraction();
        void interruptWeatherInteraction();
        void invalidateBackgroundToolRoute();
        void invalidateWeatherIntentAndPendingRequest();
        boolean isRealtimeOpen();
        void interruptRealtimePlayback(String reason);
        void closeRealtime();
        void connectRealtime();
        void resetRealtimeOutput();
        void stopRealtimePlayback();
        void clearVoiceNewsCard(boolean refreshVoice);
        void clearVoiceWeatherCard(boolean refreshVoice);
        void stopInputAudio(String nextState);
        void requestVoiceInputStart(boolean requestPermission, String interruptReason, boolean showHeadsetPrompt);
        boolean consumeInitPromptPending();
        void updateInitializationContext();
        boolean hasPendingWeatherBroadcast();
        void schedulePendingWeatherBroadcast(long delayMs);
        boolean onToolRealtimeReady();
        String consumePendingText();
        void sendRealtimeText(String text);
        void onVoiceInputRealtimeReady();
        boolean isInitializing();
        boolean hasInitSummaryPending();
        void persistAndClearActiveAssistantMessage();
        boolean onToolRealtimeOutputFinished();
        boolean maybeStartToolTtsAfterRealtimeStopped();
        boolean hasPendingNewsBroadcast();
        void schedulePendingNewsBroadcast(long delayMs);
        void finishInitializationWithSummary();
        void scheduleContinuousListening(long delayMs);
    }

    private final Host host;

    VoiceSessionOrchestrator(Host host) {
        this.host = host;
    }

    void onMicToggle() {
        if (host.isTextModeActive()) return;
        if (!host.isVoiceSurfaceActive()) return;
        if (host.isSummaryInProgress()) return;
        host.markConversationInteraction();
        if (host.hasActiveToolTtsPlayback() || host.isGatewayTtsPlaying()) {
            host.stopToolTtsPlayback(true);
            host.setState("ready");
        }
        if (host.hasNewsInterruptionAvailable()) {
            interruptNewsPlayback();
            return;
        }
        if (host.hasWeatherInterruptionAvailable()) {
            interruptWeatherPlayback();
            return;
        }
        if (host.isInputActive()) {
            host.stopInputAudio("processing");
            return;
        }
        host.requestVoiceInputStart(true, "user_speech_detected", true);
    }

    void interruptNewsPlayback() {
        host.clearPendingNewsBroadcast();
        host.stopToolTtsPlayback(true);
        host.interruptNewsInteraction();
        host.invalidateBackgroundToolRoute();
        if (host.isRealtimeOpen()) {
            host.interruptRealtimePlayback("news_interrupt");
            host.closeRealtime();
        }
        host.resetRealtimeOutput();
        host.stopRealtimePlayback();
        host.clearVoiceNewsCard(true);
        host.setState("ready");
        if (!host.isRealtimeOpen()) host.connectRealtime();
        host.scheduleContinuousListening(300);
    }

    void interruptWeatherPlayback() {
        host.clearPendingWeatherBroadcast();
        host.stopToolTtsPlayback(true);
        host.interruptWeatherInteraction();
        host.invalidateWeatherIntentAndPendingRequest();
        if (host.isRealtimeOpen()) {
            host.interruptRealtimePlayback("weather_interrupt");
            host.closeRealtime();
        }
        host.resetRealtimeOutput();
        host.stopRealtimePlayback();
        host.clearVoiceWeatherCard(true);
        host.setState("ready");
        if (!host.isRealtimeOpen()) host.connectRealtime();
        host.scheduleContinuousListening(300);
    }

    void onRealtimeReady() {
        if (host.isTextModeActive()) {
            host.setState("text_only");
            return;
        }
        if (!host.isVoiceSurfaceActive()) return;
        if (host.hasActiveToolTtsPlayback()) {
            host.setState("speaking");
            return;
        }
        if (host.consumeInitPromptPending()) {
            host.updateInitializationContext();
            host.setState("ready");
            host.onVoiceInputRealtimeReady();
            return;
        }
        if (host.hasPendingWeatherBroadcast()) {
            host.schedulePendingWeatherBroadcast(400);
            return;
        }
        if (host.onToolRealtimeReady()) {
            return;
        }
        String pendingText = host.consumePendingText();
        if (pendingText != null) {
            host.sendRealtimeText(pendingText);
            host.setState("processing");
        }
        host.onVoiceInputRealtimeReady();
    }

    void onRealtimeOutputFinished(boolean stopped) {
        long resumeDelayMs = stopped ? 180 : 650;
        boolean canListenImmediately = stopped && host.isInputActive();
        boolean shouldScheduleListening = !stopped || !host.isInputActive();

        host.persistAndClearActiveAssistantMessage();
        if (host.isTextModeActive()) {
            host.setState("text_only");
            return;
        }
        if (!host.isVoiceSurfaceActive()) return;
        if (host.onToolRealtimeOutputFinished()) {
            return;
        }
        host.setState("ready");
        if (host.maybeStartToolTtsAfterRealtimeStopped()) {
            return;
        }
        if (host.hasPendingWeatherBroadcast()) {
            host.schedulePendingWeatherBroadcast(900);
            return;
        }
        if (host.hasPendingNewsBroadcast()) {
            host.schedulePendingNewsBroadcast(250);
            return;
        }
        if (!stopped && host.isInitializing() && host.hasInitSummaryPending()) {
            host.finishInitializationWithSummary();
            return;
        }
        if (canListenImmediately) host.setState("listening");
        if (shouldScheduleListening) host.scheduleContinuousListening(resumeDelayMs);
    }
}
