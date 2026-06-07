package com.linkyun.her;

final class ToolResultPresenter {
    interface Host {
        void cacheToolFact(String toolId, String fact);
        void showNewsCard(NewsTool.NewsResult result);
        void showWeatherCard(WeatherTool.WeatherResult result);
        void addAssistantMessage(String text);
        void renderMessages();
        boolean isVoiceSurfaceActive();
        void queueToolTtsPlayback(String source, String text);
        void setState(String nextState);
        void logToolResult(String message);
    }

    private final Host host;

    ToolResultPresenter(Host host) {
        this.host = host;
    }

    void presentNews(ToolInteractionResult<NewsTool.NewsResult> result, boolean realtimeMode) {
        if (shouldCacheFact(result, realtimeMode)) host.cacheToolFact(result.tool, result.fact.trim());
        if (result.success && result.payload != null) {
            host.logToolResult("news tool success items=" + result.payload.items.size() + " realtime=" + realtimeMode);
            host.showNewsCard(result.payload);
        } else if (result.success) {
            host.logToolResult("news tool success missing payload realtime=" + realtimeMode);
        } else {
            host.logToolResult("news tool error realtime=" + realtimeMode + " message=" + result.errorMessage);
        }
        presentAnswer(result, realtimeMode, "news");
    }

    void presentWeather(ToolInteractionResult<WeatherTool.WeatherResult> result, boolean realtimeMode) {
        if (shouldCacheFact(result, realtimeMode)) host.cacheToolFact(result.tool, result.fact.trim());
        if (result.success && result.payload != null) {
            host.logToolResult("weather success place=" + result.payload.placeName + " realtime=" + realtimeMode);
            host.showWeatherCard(result.payload);
        } else if (result.success) {
            host.logToolResult("weather success missing payload realtime=" + realtimeMode);
        } else {
            host.logToolResult("weather error realtime=" + realtimeMode + " message=" + result.errorMessage);
        }
        presentAnswer(result, realtimeMode, "weather");
    }

    private void presentAnswer(ToolInteractionResult<?> result, boolean realtimeMode, String source) {
        String answer = answerText(result, realtimeMode);
        if (realtimeMode) {
            host.addAssistantMessage(answer);
            host.renderMessages();
            if (host.isVoiceSurfaceActive()) {
                host.queueToolTtsPlayback(source, answer);
            }
        } else {
            host.addAssistantMessage(answer);
            host.renderMessages();
            host.setState("ready");
        }
    }

    private static String answerText(ToolInteractionResult<?> result, boolean realtimeMode) {
        if (result.success || realtimeMode) {
            if (!result.answer.trim().isEmpty()) return result.answer.trim();
        }
        if (!result.success && !result.errorMessage.trim().isEmpty()) {
            return result.errorMessage.trim();
        }
        if (!result.answer.trim().isEmpty()) return result.answer.trim();
        return result.success ? "我已经拿到结果了。" : "工具暂时不可用，请稍后再试。";
    }

    private static boolean shouldCacheFact(ToolInteractionResult<?> result, boolean realtimeMode) {
        return (result.success || realtimeMode) && !result.fact.trim().isEmpty();
    }
}
