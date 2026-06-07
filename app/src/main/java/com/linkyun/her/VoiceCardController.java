package com.linkyun.her;

final class VoiceCardController {
    interface Scheduler {
        void postDelayed(Runnable runnable, long delayMs);
        void removeCallbacks(Runnable runnable);
    }

    interface Host {
        boolean isVoiceSurfaceActive();
        void refreshVoiceHome();
    }

    private final Scheduler scheduler;
    private final Host host;
    private WeatherTool.WeatherResult latestWeather;
    private NewsTool.NewsResult latestNews;
    private Runnable weatherTimeout;
    private Runnable newsTimeout;

    VoiceCardController(Scheduler scheduler, Host host) {
        this.scheduler = scheduler;
        this.host = host;
    }

    WeatherTool.WeatherResult latestWeather() {
        return latestWeather;
    }

    NewsTool.NewsResult latestNews() {
        return latestNews;
    }

    boolean hasWeatherCard() {
        return latestWeather != null;
    }

    boolean hasNewsCard() {
        return latestNews != null;
    }

    void showWeather(WeatherTool.WeatherResult result) {
        latestWeather = result;
        latestNews = null;
        cancelNewsTimeout();
        scheduleWeatherTimeout(result);
        refreshIfVoiceSurface();
    }

    void showNews(NewsTool.NewsResult result, boolean scheduleTimeout) {
        latestNews = result;
        latestWeather = null;
        cancelWeatherTimeout();
        if (scheduleTimeout) {
            scheduleNewsTimeout(result);
        } else {
            cancelNewsTimeout();
        }
        refreshIfVoiceSurface();
    }

    void clearWeather(boolean refreshVoice) {
        cancelWeatherTimeout();
        if (latestWeather == null) return;
        latestWeather = null;
        if (refreshVoice) refreshIfVoiceSurface();
    }

    void clearNews(boolean refreshVoice) {
        cancelNewsTimeout();
        if (latestNews == null) return;
        latestNews = null;
        if (refreshVoice) refreshIfVoiceSurface();
    }

    void clearAll(boolean refreshVoice) {
        cancelWeatherTimeout();
        cancelNewsTimeout();
        boolean hadCard = latestWeather != null || latestNews != null;
        latestWeather = null;
        latestNews = null;
        if (refreshVoice && hadCard) refreshIfVoiceSurface();
    }

    void cancelTimeouts() {
        cancelWeatherTimeout();
        cancelNewsTimeout();
    }

    private void scheduleWeatherTimeout(WeatherTool.WeatherResult result) {
        cancelWeatherTimeout();
        weatherTimeout = () -> {
            if (latestWeather != result) return;
            weatherTimeout = null;
            latestWeather = null;
            refreshIfVoiceSurface();
        };
        scheduler.postDelayed(weatherTimeout, WeatherSkill.VOICE_CARD_TIMEOUT_MS);
    }

    private void scheduleNewsTimeout(NewsTool.NewsResult result) {
        cancelNewsTimeout();
        newsTimeout = () -> {
            if (latestNews != result) return;
            newsTimeout = null;
            latestNews = null;
            refreshIfVoiceSurface();
        };
        scheduler.postDelayed(newsTimeout, NewsSkill.VOICE_CARD_TIMEOUT_MS);
    }

    private void cancelWeatherTimeout() {
        if (weatherTimeout != null) {
            scheduler.removeCallbacks(weatherTimeout);
            weatherTimeout = null;
        }
    }

    private void cancelNewsTimeout() {
        if (newsTimeout != null) {
            scheduler.removeCallbacks(newsTimeout);
            newsTimeout = null;
        }
    }

    private void refreshIfVoiceSurface() {
        if (host.isVoiceSurfaceActive()) host.refreshVoiceHome();
    }
}
