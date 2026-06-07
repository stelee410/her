package com.linkyun.her;

final class ToolInteractionCoordinator {
    enum State {
        IDLE,
        NEWS_ACKING,
        NEWS_FETCHING,
        WEATHER_FETCHING
    }

    interface Host {
        void onNewsStarted(String question, boolean realtimeMode);
        void invalidateBackgroundToolRoute();
        boolean startNewsAck(String question);
        void startNewsFetch(String question, boolean realtimeMode, int token);
        void onNewsCompleted(String question, boolean realtimeMode);
        void onNewsInterrupted(String question, boolean realtimeMode);
        void onWeatherStarted(String question, boolean realtimeMode);
        void startWeatherFetch(String question, boolean realtimeMode, int token);
        void onWeatherCompleted(String question, boolean realtimeMode);
        void onWeatherInterrupted(String question, boolean realtimeMode);
        void logToolInteraction(String message);
    }

    private final Host host;
    private State state = State.IDLE;
    private ToolSession activeSession = ToolSession.none();
    private int token;
    private boolean newsAckSent;

    ToolInteractionCoordinator(Host host) {
        this.host = host;
    }

    State state() {
        return state;
    }

    boolean isNewsActive() {
        return state == State.NEWS_ACKING || state == State.NEWS_FETCHING;
    }

    boolean isAwaitingRealtimeNewsAnswer() {
        return isNewsActive() && activeSession.isRealtimeTool(NewsToolDefinition.ID);
    }

    boolean isNewsAckPending() {
        return state == State.NEWS_ACKING;
    }

    boolean isWeatherActive() {
        return state == State.WEATHER_FETCHING;
    }

    boolean isAwaitingRealtimeWeatherAnswer() {
        return isWeatherActive() && activeSession.isRealtimeTool(WeatherToolDefinition.ID);
    }

    void startNews(String question, boolean realtimeMode) {
        String normalizedQuestion = normalizeQuestion(question, "每日新闻热点");
        interruptActiveSessionForReplacement();
        token++;
        activeSession = ToolSession.start(NewsToolDefinition.ID, normalizedQuestion, realtimeMode, token);
        newsAckSent = false;
        host.invalidateBackgroundToolRoute();
        if (realtimeMode) {
            state = State.NEWS_ACKING;
            host.onNewsStarted(normalizedQuestion, true);
            host.logToolInteraction("news ack start question=" + normalizedQuestion);
            newsAckSent = host.startNewsAck(normalizedQuestion);
        } else {
            state = State.NEWS_FETCHING;
            host.onNewsStarted(normalizedQuestion, false);
            host.logToolInteraction("news fetch start question=" + normalizedQuestion);
            host.startNewsFetch(normalizedQuestion, false, token);
        }
    }

    void startNewsFromBackground(String question) {
        String normalizedQuestion = normalizeQuestion(question, "每日新闻热点");
        interruptActiveSessionForReplacement();
        token++;
        activeSession = ToolSession.start(NewsToolDefinition.ID, normalizedQuestion, true, token);
        newsAckSent = false;
        state = State.NEWS_FETCHING;
        host.onNewsStarted(normalizedQuestion, true);
        host.logToolInteraction("news background fetch start question=" + normalizedQuestion);
        host.startNewsFetch(normalizedQuestion, true, token);
    }

    void startWeather(String question, boolean realtimeMode) {
        String normalizedQuestion = normalizeQuestion(question, "天气");
        interruptActiveSessionForReplacement();
        token++;
        activeSession = ToolSession.start(WeatherToolDefinition.ID, normalizedQuestion, realtimeMode, token);
        newsAckSent = false;
        host.invalidateBackgroundToolRoute();
        state = State.WEATHER_FETCHING;
        host.onWeatherStarted(normalizedQuestion, realtimeMode);
        host.logToolInteraction("weather fetch start question=" + normalizedQuestion);
        host.startWeatherFetch(normalizedQuestion, realtimeMode, token);
    }

    boolean onRealtimeReady() {
        if (state != State.NEWS_ACKING) return false;
        if (!newsAckSent) {
            newsAckSent = host.startNewsAck(activeSession.question());
        }
        return true;
    }

    boolean onRealtimeOutputFinished() {
        if (state != State.NEWS_ACKING) return false;
        if (!newsAckSent) return false;
        state = State.NEWS_FETCHING;
        host.logToolInteraction("news ack done, fetch question=" + activeSession.question());
        host.startNewsFetch(activeSession.question(), true, activeSession.token());
        return true;
    }

    boolean completeNewsFetch(int callbackToken) {
        if (state != State.NEWS_FETCHING ||
                !activeSession.matches(NewsToolDefinition.ID, callbackToken)) return false;
        String question = activeSession.question();
        boolean realtimeMode = activeSession.realtimeMode();
        clear();
        host.onNewsCompleted(question, realtimeMode);
        return true;
    }

    boolean completeWeatherFetch(int callbackToken) {
        if (state != State.WEATHER_FETCHING ||
                !activeSession.matches(WeatherToolDefinition.ID, callbackToken)) return false;
        String question = activeSession.question();
        boolean realtimeMode = activeSession.realtimeMode();
        clear();
        host.onWeatherCompleted(question, realtimeMode);
        return true;
    }

    void interruptNews() {
        if (!isNewsActive()) {
            token++;
            return;
        }
        String question = activeSession.question();
        boolean realtimeMode = activeSession.realtimeMode();
        token++;
        clear();
        host.onNewsInterrupted(question, realtimeMode);
    }

    void interruptWeather() {
        if (!isWeatherActive()) {
            token++;
            return;
        }
        String question = activeSession.question();
        boolean realtimeMode = activeSession.realtimeMode();
        token++;
        clear();
        host.onWeatherInterrupted(question, realtimeMode);
    }

    void clearNews() {
        if (isNewsActive()) {
            token++;
            clear();
        }
    }

    void clearWeather() {
        if (isWeatherActive()) {
            token++;
            clear();
        }
    }

    private void clear() {
        state = State.IDLE;
        activeSession = ToolSession.none();
        newsAckSent = false;
    }

    private void interruptActiveSessionForReplacement() {
        if (!isNewsActive() && !isWeatherActive()) return;
        String toolId = activeSession.toolId();
        String question = activeSession.question();
        boolean realtimeMode = activeSession.realtimeMode();
        clear();
        if (NewsToolDefinition.ID.equals(toolId)) {
            host.onNewsInterrupted(question, realtimeMode);
        } else if (WeatherToolDefinition.ID.equals(toolId)) {
            host.onWeatherInterrupted(question, realtimeMode);
        }
    }

    private static String normalizeQuestion(String question, String fallback) {
        if (question == null || question.trim().isEmpty()) return fallback;
        return question.trim();
    }
}
