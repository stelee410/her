package com.linkyun.her;

final class PendingBroadcastCoordinator {
    interface Scheduler {
        void postDelayed(Runnable runnable, long delayMs);
    }

    interface Host {
        boolean canSendWeatherNow();
        boolean canSendNewsNow();
        boolean isRealtimeOpen();
        void connectRealtime();
        void pushWeatherFact();
        void pushNewsFact();
        void sendRealtimeText(String text);
        void onBroadcastSent();
        void logBroadcast(String message);
    }

    private static final long INITIAL_DELAY_MS = 1000;
    private static final long REALTIME_RECONNECT_DELAY_MS = 1400;
    private static final long NEWS_BUSY_RETRY_MS = 600;

    private final Scheduler scheduler;
    private final Host host;
    private int weatherSeq = 0;
    private int newsSeq = 0;
    private String weatherPrompt;
    private String newsPrompt;

    PendingBroadcastCoordinator(Scheduler scheduler, Host host) {
        this.scheduler = scheduler;
        this.host = host;
    }

    void queueWeather(String prompt) {
        if (isBlank(prompt)) return;
        weatherPrompt = prompt;
        host.pushWeatherFact();
        if (host.canSendWeatherNow()) {
            scheduleWeather(INITIAL_DELAY_MS);
        }
    }

    void scheduleWeather(long delayMs) {
        if (!hasPendingWeather()) return;
        int seq = ++weatherSeq;
        scheduler.postDelayed(() -> sendWeather(seq), delayMs);
    }

    void queueNews(String prompt) {
        if (isBlank(prompt)) return;
        newsPrompt = prompt;
        host.pushNewsFact();
        if (host.canSendNewsNow()) {
            scheduleNews(INITIAL_DELAY_MS);
        }
    }

    void scheduleNews(long delayMs) {
        if (!hasPendingNews()) return;
        int seq = ++newsSeq;
        scheduler.postDelayed(() -> sendNews(seq), delayMs);
    }

    boolean hasPendingWeather() {
        return !isBlank(weatherPrompt);
    }

    boolean hasPendingNews() {
        return !isBlank(newsPrompt);
    }

    void clearWeather() {
        weatherPrompt = null;
        weatherSeq++;
    }

    void clearNews() {
        newsPrompt = null;
        newsSeq++;
    }

    private void sendWeather(int seq) {
        if (seq != weatherSeq) return;
        String prompt = weatherPrompt;
        if (isBlank(prompt)) return;
        if (!host.isRealtimeOpen()) {
            host.connectRealtime();
            retryWeather(seq, REALTIME_RECONNECT_DELAY_MS);
            return;
        }
        weatherPrompt = null;
        weatherSeq++;
        host.sendRealtimeText(prompt);
        host.onBroadcastSent();
    }

    private void sendNews(int seq) {
        if (seq != newsSeq) return;
        String prompt = newsPrompt;
        if (isBlank(prompt)) return;
        host.logBroadcast("send pending news broadcast");
        if (!host.canSendNewsNow()) {
            retryNews(seq, NEWS_BUSY_RETRY_MS);
            return;
        }
        if (!host.isRealtimeOpen()) {
            host.connectRealtime();
            retryNews(seq, REALTIME_RECONNECT_DELAY_MS);
            return;
        }
        host.pushNewsFact();
        newsPrompt = null;
        newsSeq++;
        host.sendRealtimeText(prompt);
        host.onBroadcastSent();
    }

    private void retryWeather(int seq, long delayMs) {
        scheduler.postDelayed(() -> sendWeather(seq), delayMs);
    }

    private void retryNews(int seq, long delayMs) {
        scheduler.postDelayed(() -> sendNews(seq), delayMs);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
