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
        void startNewsAck(String question);
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
    private String activeQuestion;
    private boolean activeRealtimeMode;
    private int token;

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
        return activeRealtimeMode && isNewsActive();
    }

    boolean isNewsAckPending() {
        return state == State.NEWS_ACKING;
    }

    boolean isWeatherActive() {
        return state == State.WEATHER_FETCHING;
    }

    boolean isAwaitingRealtimeWeatherAnswer() {
        return activeRealtimeMode && isWeatherActive();
    }

    void startNews(String question, boolean realtimeMode) {
        String normalizedQuestion = normalizeQuestion(question, "每日新闻热点");
        token++;
        activeQuestion = normalizedQuestion;
        activeRealtimeMode = realtimeMode;
        if (realtimeMode) {
            state = State.NEWS_ACKING;
            host.onNewsStarted(normalizedQuestion, true);
            host.logToolInteraction("news ack start question=" + normalizedQuestion);
            host.startNewsAck(normalizedQuestion);
        } else {
            state = State.NEWS_FETCHING;
            host.onNewsStarted(normalizedQuestion, false);
            host.logToolInteraction("news fetch start question=" + normalizedQuestion);
            host.startNewsFetch(normalizedQuestion, false, token);
        }
    }

    void startNewsFromBackground(String question) {
        String normalizedQuestion = normalizeQuestion(question, "每日新闻热点");
        token++;
        activeQuestion = normalizedQuestion;
        activeRealtimeMode = true;
        state = State.NEWS_FETCHING;
        host.onNewsStarted(normalizedQuestion, true);
        host.logToolInteraction("news background fetch start question=" + normalizedQuestion);
        host.startNewsFetch(normalizedQuestion, true, token);
    }

    void startWeather(String question, boolean realtimeMode) {
        String normalizedQuestion = normalizeQuestion(question, "天气");
        token++;
        activeQuestion = normalizedQuestion;
        activeRealtimeMode = realtimeMode;
        state = State.WEATHER_FETCHING;
        host.onWeatherStarted(normalizedQuestion, realtimeMode);
        host.logToolInteraction("weather fetch start question=" + normalizedQuestion);
        host.startWeatherFetch(normalizedQuestion, realtimeMode, token);
    }

    boolean onRealtimeReady() {
        if (state != State.NEWS_ACKING) return false;
        host.startNewsAck(activeQuestion);
        return true;
    }

    boolean onRealtimeOutputFinished() {
        if (state != State.NEWS_ACKING) return false;
        state = State.NEWS_FETCHING;
        host.logToolInteraction("news ack done, fetch question=" + activeQuestion);
        host.startNewsFetch(activeQuestion, true, token);
        return true;
    }

    boolean completeNewsFetch(int callbackToken) {
        if (callbackToken != token || state != State.NEWS_FETCHING) return false;
        String question = activeQuestion;
        boolean realtimeMode = activeRealtimeMode;
        clear();
        host.onNewsCompleted(question, realtimeMode);
        return true;
    }

    boolean completeWeatherFetch(int callbackToken) {
        if (callbackToken != token || state != State.WEATHER_FETCHING) return false;
        String question = activeQuestion;
        boolean realtimeMode = activeRealtimeMode;
        clear();
        host.onWeatherCompleted(question, realtimeMode);
        return true;
    }

    void interruptNews() {
        if (!isNewsActive()) {
            token++;
            return;
        }
        String question = activeQuestion;
        boolean realtimeMode = activeRealtimeMode;
        token++;
        clear();
        host.onNewsInterrupted(question, realtimeMode);
    }

    void interruptWeather() {
        if (!isWeatherActive()) {
            token++;
            return;
        }
        String question = activeQuestion;
        boolean realtimeMode = activeRealtimeMode;
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
        activeQuestion = null;
        activeRealtimeMode = false;
    }

    private static String normalizeQuestion(String question, String fallback) {
        if (question == null || question.trim().isEmpty()) return fallback;
        return question.trim();
    }
}
