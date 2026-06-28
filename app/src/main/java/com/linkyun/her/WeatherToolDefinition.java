package com.linkyun.her;

final class WeatherToolDefinition implements ToolDefinition {
    static final String ID = "weather";
    static final String LLM_TOOL_ID = "weather";

    @Override public String id() {
        return ID;
    }

    @Override public boolean matches(String text) {
        return WeatherSkill.isWeatherQuestion(text);
    }

    @Override public boolean matchesBackgroundTool(String toolId) {
        return LLM_TOOL_ID.equals(toolId);
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
        host.logToolRoute("weather background intent hit text=" + question);
        host.startWeather(question, true);
        return true;
    }
}
