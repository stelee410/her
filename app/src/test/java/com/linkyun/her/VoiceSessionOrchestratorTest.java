package com.linkyun.her;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public final class VoiceSessionOrchestratorTest {
    @Test
    public void textModeIgnoresMicToggle() {
        Host host = new Host();
        host.textMode = true;
        VoiceSessionOrchestrator orchestrator = new VoiceSessionOrchestrator(host);

        orchestrator.onMicToggle();

        assertEquals(0, host.events.size());
    }

    @Test
    public void inactiveVoiceSurfaceIgnoresMicToggle() {
        Host host = new Host();
        host.voiceSurfaceActive = false;
        VoiceSessionOrchestrator orchestrator = new VoiceSessionOrchestrator(host);

        orchestrator.onMicToggle();

        assertEquals(0, host.events.size());
    }

    @Test
    public void activeToolTtsStopsBeforeStartingInput() {
        Host host = new Host();
        host.toolTts = true;
        VoiceSessionOrchestrator orchestrator = new VoiceSessionOrchestrator(host);

        orchestrator.onMicToggle();

        assertEquals("mark", host.events.get(0));
        assertEquals("stopTool:true", host.events.get(1));
        assertEquals("state:ready", host.events.get(2));
        assertEquals("request:true:user_speech_detected:true", host.events.get(3));
    }

    @Test
    public void newsInterruptionWinsBeforeWeatherAndInput() {
        Host host = new Host();
        host.news = true;
        host.weather = true;
        host.input = true;
        VoiceSessionOrchestrator orchestrator = new VoiceSessionOrchestrator(host);

        orchestrator.onMicToggle();

        assertEquals("mark", host.events.get(0));
        assertEquals("clearNewsBroadcast", host.events.get(1));
        assertEquals("stopTool:true", host.events.get(2));
        assertEquals("interruptNews", host.events.get(3));
        assertEquals("invalidateToolRoute", host.events.get(4));
    }

    @Test
    public void weatherInterruptionRunsBeforeStoppingInput() {
        Host host = new Host();
        host.weather = true;
        host.input = true;
        VoiceSessionOrchestrator orchestrator = new VoiceSessionOrchestrator(host);

        orchestrator.onMicToggle();

        assertEquals("mark", host.events.get(0));
        assertEquals("clearWeatherBroadcast", host.events.get(1));
        assertEquals("stopTool:true", host.events.get(2));
        assertEquals("interruptWeather", host.events.get(3));
        assertEquals("invalidateWeather", host.events.get(4));
    }

    @Test
    public void activeInputStopsIntoProcessing() {
        Host host = new Host();
        host.input = true;
        VoiceSessionOrchestrator orchestrator = new VoiceSessionOrchestrator(host);

        orchestrator.onMicToggle();

        assertEquals("mark", host.events.get(0));
        assertEquals("stopInput:processing", host.events.get(1));
        assertEquals(2, host.events.size());
    }

    @Test
    public void realtimeReadyKeepsToolTtsSpeaking() {
        Host host = new Host();
        host.toolTts = true;
        VoiceSessionOrchestrator orchestrator = new VoiceSessionOrchestrator(host);

        orchestrator.onRealtimeReady();

        assertEquals("state:speaking", host.events.get(0));
        assertEquals(1, host.events.size());
    }

    @Test
    public void realtimeReadyInTextModePreservesTextOnlyAndSkipsVoiceWork() {
        Host host = new Host();
        host.textMode = true;
        host.toolTts = true;
        host.initPromptPending = true;
        host.pendingWeather = true;
        host.toolRealtimeReady = true;
        host.pendingText = "hello";
        VoiceSessionOrchestrator orchestrator = new VoiceSessionOrchestrator(host);

        orchestrator.onRealtimeReady();

        assertEquals("state:text_only", host.events.get(0));
        assertEquals(1, host.events.size());
    }

    @Test
    public void realtimeReadyOutsideVoiceSurfaceSkipsPendingVoiceWork() {
        Host host = new Host();
        host.voiceSurfaceActive = false;
        host.toolTts = true;
        host.initPromptPending = true;
        host.pendingWeather = true;
        host.toolRealtimeReady = true;
        host.pendingText = "hello";
        VoiceSessionOrchestrator orchestrator = new VoiceSessionOrchestrator(host);

        orchestrator.onRealtimeReady();

        assertEquals(0, host.events.size());
    }

    @Test
    public void realtimeReadyConsumesInitializationPromptBeforeVoiceInput() {
        Host host = new Host();
        host.initPromptPending = true;
        VoiceSessionOrchestrator orchestrator = new VoiceSessionOrchestrator(host);

        orchestrator.onRealtimeReady();

        assertEquals("updateInitContext", host.events.get(0));
        assertEquals("state:ready", host.events.get(1));
        assertEquals("voiceInputReady", host.events.get(2));
        assertEquals(3, host.events.size());
    }

    @Test
    public void realtimeReadySendsPendingTextThenStartsPendingVoiceInput() {
        Host host = new Host();
        host.pendingText = "hello";
        VoiceSessionOrchestrator orchestrator = new VoiceSessionOrchestrator(host);

        orchestrator.onRealtimeReady();

        assertEquals("sendText:hello", host.events.get(0));
        assertEquals("state:processing", host.events.get(1));
        assertEquals("voiceInputReady", host.events.get(2));
        assertEquals(3, host.events.size());
    }

    @Test
    public void realtimeReadySchedulesWeatherBroadcastBeforeToolOrText() {
        Host host = new Host();
        host.pendingWeather = true;
        host.toolRealtimeReady = true;
        host.pendingText = "hello";
        VoiceSessionOrchestrator orchestrator = new VoiceSessionOrchestrator(host);

        orchestrator.onRealtimeReady();

        assertEquals("scheduleWeather:400", host.events.get(0));
        assertEquals(1, host.events.size());
    }

    @Test
    public void outputFinishedLetsToolCoordinatorContinueFirst() {
        Host host = new Host();
        host.toolOutputFinished = true;
        VoiceSessionOrchestrator orchestrator = new VoiceSessionOrchestrator(host);

        orchestrator.onRealtimeOutputFinished(false);

        assertEquals("persistClear", host.events.get(0));
        assertEquals(1, host.events.size());
    }

    @Test
    public void outputFinishedInTextModePreservesTextOnlyAndSkipsVoiceFollowups() {
        Host host = new Host();
        host.textMode = true;
        host.input = true;
        host.toolOutputFinished = true;
        host.startToolTts = true;
        host.pendingWeather = true;
        host.pendingNews = true;
        host.initializing = true;
        host.initSummaryPending = true;
        VoiceSessionOrchestrator orchestrator = new VoiceSessionOrchestrator(host);

        orchestrator.onRealtimeOutputFinished(false);

        assertEquals("persistClear", host.events.get(0));
        assertEquals("state:text_only", host.events.get(1));
        assertEquals(2, host.events.size());
    }

    @Test
    public void outputFinishedOutsideVoiceSurfaceOnlyPersistsActiveAssistant() {
        Host host = new Host();
        host.voiceSurfaceActive = false;
        host.input = true;
        host.toolOutputFinished = true;
        host.startToolTts = true;
        host.pendingWeather = true;
        host.pendingNews = true;
        host.initializing = true;
        host.initSummaryPending = true;
        VoiceSessionOrchestrator orchestrator = new VoiceSessionOrchestrator(host);

        orchestrator.onRealtimeOutputFinished(false);

        assertEquals("persistClear", host.events.get(0));
        assertEquals(1, host.events.size());
    }

    @Test
    public void outputFinishedStartsToolTtsBeforeBroadcastOrListening() {
        Host host = new Host();
        host.startToolTts = true;
        host.pendingWeather = true;
        VoiceSessionOrchestrator orchestrator = new VoiceSessionOrchestrator(host);

        orchestrator.onRealtimeOutputFinished(false);

        assertEquals("persistClear", host.events.get(0));
        assertEquals("state:ready", host.events.get(1));
        assertEquals(2, host.events.size());
    }

    @Test
    public void outputFinishedSchedulesBroadcastsBeforeContinuousListening() {
        Host host = new Host();
        host.pendingNews = true;
        VoiceSessionOrchestrator orchestrator = new VoiceSessionOrchestrator(host);

        orchestrator.onRealtimeOutputFinished(false);

        assertEquals("persistClear", host.events.get(0));
        assertEquals("state:ready", host.events.get(1));
        assertEquals("scheduleNews:250", host.events.get(2));
        assertEquals(3, host.events.size());
    }

    @Test
    public void stoppedOutputWithActiveInputReturnsToListeningWithoutScheduling() {
        Host host = new Host();
        host.input = true;
        VoiceSessionOrchestrator orchestrator = new VoiceSessionOrchestrator(host);

        orchestrator.onRealtimeOutputFinished(true);

        assertEquals("persistClear", host.events.get(0));
        assertEquals("state:ready", host.events.get(1));
        assertEquals("state:listening", host.events.get(2));
        assertEquals(3, host.events.size());
    }

    @Test
    public void naturalOutputSchedulesContinuousListeningAfterDelay() {
        Host host = new Host();
        VoiceSessionOrchestrator orchestrator = new VoiceSessionOrchestrator(host);

        orchestrator.onRealtimeOutputFinished(false);

        assertEquals("persistClear", host.events.get(0));
        assertEquals("state:ready", host.events.get(1));
        assertEquals("state:listening", host.events.get(2));
        assertEquals("scheduleListen:650", host.events.get(3));
        assertEquals(4, host.events.size());
    }

    @Test
    public void newsInterruptClosesOpenRealtimeReconnectsAndResumesListening() {
        Host host = new Host();
        host.realtimeOpen = true;
        VoiceSessionOrchestrator orchestrator = new VoiceSessionOrchestrator(host);

        orchestrator.interruptNewsPlayback();

        assertEquals("clearNewsBroadcast", host.events.get(0));
        assertEquals("stopTool:true", host.events.get(1));
        assertEquals("interruptNews", host.events.get(2));
        assertEquals("invalidateToolRoute", host.events.get(3));
        assertEquals("interruptRealtime:news_interrupt", host.events.get(4));
        assertEquals("closeRealtime", host.events.get(5));
        assertEquals("resetRealtime", host.events.get(6));
        assertEquals("stopRealtime", host.events.get(7));
        assertEquals("clearNewsCard:true", host.events.get(8));
        assertEquals("state:ready", host.events.get(9));
        assertEquals("connectRealtime", host.events.get(10));
        assertEquals("scheduleListen:300", host.events.get(11));
    }

    @Test
    public void weatherInterruptInvalidatesPendingWeatherBeforeRealtimeReset() {
        Host host = new Host();
        host.realtimeOpen = true;
        VoiceSessionOrchestrator orchestrator = new VoiceSessionOrchestrator(host);

        orchestrator.interruptWeatherPlayback();

        assertEquals("clearWeatherBroadcast", host.events.get(0));
        assertEquals("stopTool:true", host.events.get(1));
        assertEquals("interruptWeather", host.events.get(2));
        assertEquals("invalidateWeather", host.events.get(3));
        assertEquals("interruptRealtime:weather_interrupt", host.events.get(4));
        assertEquals("closeRealtime", host.events.get(5));
        assertEquals("resetRealtime", host.events.get(6));
        assertEquals("stopRealtime", host.events.get(7));
        assertEquals("clearWeatherCard:true", host.events.get(8));
        assertEquals("state:ready", host.events.get(9));
        assertEquals("connectRealtime", host.events.get(10));
        assertEquals("scheduleListen:300", host.events.get(11));
    }

    private static final class Host implements VoiceSessionOrchestrator.Host {
        final List<String> events = new ArrayList<>();
        boolean textMode;
        boolean voiceSurfaceActive = true;
        boolean summary;
        boolean toolTts;
        boolean gatewayTts;
        boolean news;
        boolean weather;
        boolean input;
        boolean initPromptPending;
        boolean pendingWeather;
        boolean toolRealtimeReady;
        boolean initializing;
        boolean initSummaryPending;
        boolean toolOutputFinished;
        boolean startToolTts;
        boolean pendingNews;
        boolean realtimeOpen;
        String pendingText;

        @Override public boolean isTextModeActive() { return textMode; }
        @Override public boolean isVoiceSurfaceActive() { return voiceSurfaceActive; }
        @Override public boolean isSummaryInProgress() { return summary; }
        @Override public boolean hasActiveToolTtsPlayback() { return toolTts; }
        @Override public boolean isGatewayTtsPlaying() { return gatewayTts; }
        @Override public boolean hasNewsInterruptionAvailable() { return news; }
        @Override public boolean hasWeatherInterruptionAvailable() { return weather; }
        @Override public boolean isInputActive() { return input; }
        @Override public void markConversationInteraction() { events.add("mark"); }
        @Override public void stopToolTtsPlayback(boolean interrupt) { events.add("stopTool:" + interrupt); }
        @Override public void setState(String nextState) { events.add("state:" + nextState); }
        @Override public void clearPendingNewsBroadcast() { events.add("clearNewsBroadcast"); }
        @Override public void clearPendingWeatherBroadcast() { events.add("clearWeatherBroadcast"); }
        @Override public void interruptNewsInteraction() { events.add("interruptNews"); }
        @Override public void interruptWeatherInteraction() { events.add("interruptWeather"); }
        @Override public void invalidateBackgroundToolRoute() { events.add("invalidateToolRoute"); }
        @Override public void invalidateWeatherIntentAndPendingRequest() { events.add("invalidateWeather"); }
        @Override public boolean isRealtimeOpen() { return realtimeOpen; }
        @Override public void interruptRealtimePlayback(String reason) { events.add("interruptRealtime:" + reason); }
        @Override public void closeRealtime() { events.add("closeRealtime"); realtimeOpen = false; }
        @Override public void connectRealtime() { events.add("connectRealtime"); realtimeOpen = true; }
        @Override public void resetRealtimeOutput() { events.add("resetRealtime"); }
        @Override public void stopRealtimePlayback() { events.add("stopRealtime"); }
        @Override public void clearVoiceNewsCard(boolean refreshVoice) { events.add("clearNewsCard:" + refreshVoice); }
        @Override public void clearVoiceWeatherCard(boolean refreshVoice) { events.add("clearWeatherCard:" + refreshVoice); }
        @Override public void stopInputAudio(String nextState) { events.add("stopInput:" + nextState); }
        @Override public void requestVoiceInputStart(boolean requestPermission, String interruptReason, boolean showHeadsetPrompt) {
            events.add("request:" + requestPermission + ":" + interruptReason + ":" + showHeadsetPrompt);
        }
        @Override public boolean consumeInitPromptPending() {
            boolean value = initPromptPending;
            initPromptPending = false;
            return value;
        }
        @Override public void updateInitializationContext() { events.add("updateInitContext"); }
        @Override public boolean hasPendingWeatherBroadcast() { return pendingWeather; }
        @Override public void schedulePendingWeatherBroadcast(long delayMs) { events.add("scheduleWeather:" + delayMs); }
        @Override public boolean onToolRealtimeReady() { return toolRealtimeReady; }
        @Override public String consumePendingText() {
            String value = pendingText;
            pendingText = null;
            return value;
        }
        @Override public void sendRealtimeText(String text) { events.add("sendText:" + text); }
        @Override public void onVoiceInputRealtimeReady() { events.add("voiceInputReady"); }
        @Override public boolean isInitializing() { return initializing; }
        @Override public boolean hasInitSummaryPending() { return initSummaryPending; }
        @Override public void persistAndClearActiveAssistantMessage() { events.add("persistClear"); }
        @Override public boolean onToolRealtimeOutputFinished() { return toolOutputFinished; }
        @Override public boolean maybeStartToolTtsAfterRealtimeStopped() { return startToolTts; }
        @Override public boolean hasPendingNewsBroadcast() { return pendingNews; }
        @Override public void schedulePendingNewsBroadcast(long delayMs) { events.add("scheduleNews:" + delayMs); }
        @Override public void finishInitializationWithSummary() { events.add("finishSummary"); }
        @Override public void scheduleContinuousListening(long delayMs) { events.add("scheduleListen:" + delayMs); }
    }
}
