package com.linkyun.her;

final class WeatherToolDefinition implements ToolDefinition {
    static final String ID = "weather";

    @Override public String id() {
        return ID;
    }

    @Override public boolean matches(String text) {
        return WeatherSkill.isWeatherQuestion(text);
    }

    @Override public boolean matchesBackgroundTool(String toolId) {
        return false;
    }

    @Override public boolean start(String question, boolean realtimeMode, ToolRouter.Host host) {
        host.logToolRoute("weather intent hit realtime=" + realtimeMode + " text=" + question);
        if (realtimeMode && WeatherSkill.shouldReuseLatestFact(question, host.hasLatestWeatherFact())) {
            host.logToolRoute("weather reuse latest fact text=" + question);
            host.reuseLatestWeatherFact(question);
            return true;
        }
        host.startWeather(question, realtimeMode);
        return true;
    }

    @Override public boolean startFromBackground(String question, ToolRouter.Host host) {
        return false;
    }
}
