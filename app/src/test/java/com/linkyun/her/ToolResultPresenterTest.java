package com.linkyun.her;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public final class ToolResultPresenterTest {
    @Test
    public void realtimeNewsSuccessCachesFactShowsCardAndQueuesTts() {
        Host host = new Host();
        ToolResultPresenter presenter = new ToolResultPresenter(host);
        ToolInteractionResult<NewsTool.NewsResult> result = ToolInteractionResult.success(
                "news",
                "新闻",
                "fact",
                "answer",
                new NewsTool.NewsResult("source", Collections.emptyList()));

        presenter.presentNews(result, true);

        assertEquals("cache:news:fact", host.events.get(0));
        assertEquals("newsCard:source", host.events.get(2));
        assertEquals("message:answer", host.events.get(3));
        assertEquals("render", host.events.get(4));
        assertEquals("tts:news:answer", host.events.get(5));
    }

    @Test
    public void textNewsFailureShowsErrorAndReturnsReady() {
        Host host = new Host();
        ToolResultPresenter presenter = new ToolResultPresenter(host);
        ToolInteractionResult<NewsTool.NewsResult> result = ToolInteractionResult.failure(
                "news",
                "新闻",
                "fact",
                "answer",
                "failed");

        presenter.presentNews(result, false);

        assertEquals("log:news tool error realtime=false message=failed", host.events.get(0));
        assertEquals("message:failed", host.events.get(1));
        assertEquals("render", host.events.get(2));
        assertEquals("state:ready", host.events.get(3));
    }

    @Test
    public void textNewsSuccessShowsCardMessageAndReturnsReadyWithoutTts() {
        Host host = new Host();
        ToolResultPresenter presenter = new ToolResultPresenter(host);
        ToolInteractionResult<NewsTool.NewsResult> result = ToolInteractionResult.success(
                "news",
                "新闻",
                "fact",
                "answer",
                new NewsTool.NewsResult("source", Collections.emptyList()));

        presenter.presentNews(result, false);

        assertEquals("cache:news:fact", host.events.get(0));
        assertEquals("log:news tool success items=0 realtime=false", host.events.get(1));
        assertEquals("newsCard:source", host.events.get(2));
        assertEquals("message:answer", host.events.get(3));
        assertEquals("render", host.events.get(4));
        assertEquals("state:ready", host.events.get(5));
        assertEquals(6, host.events.size());
    }

    @Test
    public void textWeatherSuccessShowsCardMessageAndReturnsReadyWithoutTts() {
        Host host = new Host();
        ToolResultPresenter presenter = new ToolResultPresenter(host);
        ToolInteractionResult<WeatherTool.WeatherResult> result = ToolInteractionResult.success(
                "weather",
                "天气",
                "weatherFact",
                "weatherAnswer",
                sampleWeather());

        presenter.presentWeather(result, false);

        assertEquals("cache:weather:weatherFact", host.events.get(0));
        assertEquals("log:weather success place=深圳 realtime=false", host.events.get(1));
        assertEquals("weatherCard:深圳", host.events.get(2));
        assertEquals("message:weatherAnswer", host.events.get(3));
        assertEquals("render", host.events.get(4));
        assertEquals("state:ready", host.events.get(5));
        assertEquals(6, host.events.size());
    }

    @Test
    public void textNewsSuccessWithMissingPayloadDoesNotCrashOrShowCard() {
        Host host = new Host();
        ToolResultPresenter presenter = new ToolResultPresenter(host);
        ToolInteractionResult<NewsTool.NewsResult> result = ToolInteractionResult.success(
                "news",
                "新闻",
                "fact",
                "answer",
                null);

        presenter.presentNews(result, false);

        assertEquals("cache:news:fact", host.events.get(0));
        assertEquals("log:news tool success missing payload realtime=false", host.events.get(1));
        assertEquals("message:answer", host.events.get(2));
        assertEquals("render", host.events.get(3));
        assertEquals("state:ready", host.events.get(4));
        assertEquals(5, host.events.size());
    }

    @Test
    public void realtimeWeatherSuccessWithMissingPayloadDoesNotCrashOrShowCard() {
        Host host = new Host();
        ToolResultPresenter presenter = new ToolResultPresenter(host);
        ToolInteractionResult<WeatherTool.WeatherResult> result = ToolInteractionResult.success(
                "weather",
                "天气",
                "weatherFact",
                "weatherAnswer",
                null);

        presenter.presentWeather(result, true);

        assertEquals("cache:weather:weatherFact", host.events.get(0));
        assertEquals("log:weather success missing payload realtime=true", host.events.get(1));
        assertEquals("message:weatherAnswer", host.events.get(2));
        assertEquals("render", host.events.get(3));
        assertEquals("tts:weather:weatherAnswer", host.events.get(4));
        assertEquals(5, host.events.size());
    }

    @Test
    public void realtimeWeatherFailureStillCachesFactAndQueuesTts() {
        Host host = new Host();
        ToolResultPresenter presenter = new ToolResultPresenter(host);
        ToolInteractionResult<WeatherTool.WeatherResult> result = ToolInteractionResult.failure(
                "weather",
                "天气",
                "weatherFact",
                "weatherAnswer",
                "failed");

        presenter.presentWeather(result, true);

        assertEquals("cache:weather:weatherFact", host.events.get(0));
        assertEquals("log:weather error realtime=true message=failed", host.events.get(1));
        assertEquals("message:weatherAnswer", host.events.get(2));
        assertEquals("render", host.events.get(3));
        assertEquals("tts:weather:weatherAnswer", host.events.get(4));
    }

    @Test
    public void realtimeFailureWithNullAnswerFallsBackToErrorMessage() {
        Host host = new Host();
        ToolResultPresenter presenter = new ToolResultPresenter(host);
        ToolInteractionResult<WeatherTool.WeatherResult> result = ToolInteractionResult.failure(
                "weather",
                "天气",
                null,
                null,
                "failed");

        presenter.presentWeather(result, true);

        assertEquals("log:weather error realtime=true message=failed", host.events.get(0));
        assertEquals("message:failed", host.events.get(1));
        assertEquals("render", host.events.get(2));
        assertEquals("tts:weather:failed", host.events.get(3));
        assertEquals(4, host.events.size());
    }

    @Test
    public void realtimeFailureWithBlankFactDoesNotClearCachedFact() {
        Host host = new Host();
        ToolResultPresenter presenter = new ToolResultPresenter(host);
        ToolInteractionResult<NewsTool.NewsResult> result = ToolInteractionResult.failure(
                "news",
                "新闻",
                "  ",
                "answer",
                "failed");

        presenter.presentNews(result, true);

        assertEquals("log:news tool error realtime=true message=failed", host.events.get(0));
        assertEquals("message:answer", host.events.get(1));
        assertEquals("render", host.events.get(2));
        assertEquals("tts:news:answer", host.events.get(3));
        assertEquals(4, host.events.size());
    }

    @Test
    public void realtimeSuccessWithBlankAnswerQueuesNonEmptyFallback() {
        Host host = new Host();
        ToolResultPresenter presenter = new ToolResultPresenter(host);
        ToolInteractionResult<WeatherTool.WeatherResult> result = ToolInteractionResult.success(
                "weather",
                "天气",
                "weatherFact",
                "   ",
                sampleWeather());

        presenter.presentWeather(result, true);

        assertEquals("message:我已经拿到结果了。", host.events.get(3));
        assertEquals("render", host.events.get(4));
        assertEquals("tts:weather:我已经拿到结果了。", host.events.get(5));
    }

    @Test
    public void realtimeResultOffVoiceSurfaceDoesNotQueueTts() {
        Host host = new Host();
        host.voiceSurface = false;
        ToolResultPresenter presenter = new ToolResultPresenter(host);
        ToolInteractionResult<WeatherTool.WeatherResult> result = ToolInteractionResult.success(
                "weather",
                "天气",
                "weatherFact",
                "weatherAnswer",
                sampleWeather());

        presenter.presentWeather(result, true);

        assertEquals("message:weatherAnswer", host.events.get(3));
        assertEquals("render", host.events.get(4));
        assertEquals(5, host.events.size());
    }

    @Test
    public void textFailureWithBlankAnswerAndErrorUsesNonEmptyFallback() {
        Host host = new Host();
        ToolResultPresenter presenter = new ToolResultPresenter(host);
        ToolInteractionResult<NewsTool.NewsResult> result = ToolInteractionResult.failure(
                "news",
                "新闻",
                "",
                " ",
                "");

        presenter.presentNews(result, false);

        assertEquals("message:工具暂时不可用，请稍后再试。", host.events.get(1));
        assertEquals("render", host.events.get(2));
        assertEquals("state:ready", host.events.get(3));
    }

    private static WeatherTool.WeatherResult sampleWeather() {
        return new WeatherTool.WeatherResult(
                "深圳",
                28.3,
                30.1,
                66,
                12.4,
                "晴",
                "2026-06-05T12:00");
    }

    private static final class Host implements ToolResultPresenter.Host {
        final List<String> events = new ArrayList<>();
        boolean voiceSurface = true;

        @Override public void cacheToolFact(String toolId, String fact) {
            events.add("cache:" + toolId + ":" + fact);
        }

        @Override public void showNewsCard(NewsTool.NewsResult result) {
            events.add("newsCard:" + result.sourceUrl);
        }

        @Override public void showWeatherCard(WeatherTool.WeatherResult result) {
            events.add("weatherCard:" + result.placeName);
        }

        @Override public void addAssistantMessage(String text) {
            events.add("message:" + text);
        }

        @Override public void renderMessages() {
            events.add("render");
        }

        @Override public boolean isVoiceSurfaceActive() {
            return voiceSurface;
        }

        @Override public void queueToolTtsPlayback(String source, String text) {
            events.add("tts:" + source + ":" + text);
        }

        @Override public void setState(String nextState) {
            events.add("state:" + nextState);
        }

        @Override public void logToolResult(String message) {
            events.add("log:" + message);
        }
    }
}
