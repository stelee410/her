package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.location.Location;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public final class WeatherInteractionHandlerTest {
    @Test
    public void citySuccessBuildsUnifiedToolResult() {
        WeatherTool.WeatherResult weather = sampleWeather();
        WeatherInteractionHandler handler = new WeatherInteractionHandler(new WeatherInteractionHandler.Source() {
            @Override public void queryCity(String city, WeatherTool.CallbackResult callback) {
                assertEquals("深圳", city);
                callback.onSuccess(weather);
            }

            @Override public void queryLocation(Location location, WeatherTool.CallbackResult callback) { }
        });
        List<ToolInteractionResult<WeatherTool.WeatherResult>> results = new ArrayList<>();

        handler.queryCity(" 深圳天气 ", "深圳", results::add);

        assertEquals(1, results.size());
        ToolInteractionResult<WeatherTool.WeatherResult> result = results.get(0);
        assertTrue(result.success);
        assertEquals("weather", result.tool);
        assertEquals("深圳天气", result.question);
        assertSame(weather, result.payload);
        assertTrue(result.fact.contains("【天气查询结果】"));
        assertTrue(result.fact.contains("用户刚才问：深圳天气"));
        assertTrue(result.answer.contains("深圳现在晴"));
        assertEquals("", result.errorMessage);
    }

    @Test
    public void failureBuildsUnifiedFailureResult() {
        WeatherInteractionHandler handler = new WeatherInteractionHandler((WeatherInteractionHandler.Source) null);
        List<ToolInteractionResult<WeatherTool.WeatherResult>> results = new ArrayList<>();

        handler.fail("", "没有定位权限", results::add);

        assertEquals(1, results.size());
        ToolInteractionResult<WeatherTool.WeatherResult> result = results.get(0);
        assertFalse(result.success);
        assertEquals("天气", result.question);
        assertEquals("没有定位权限", result.errorMessage);
        assertTrue(result.fact.contains("查询失败：没有定位权限"));
        assertEquals("暂时没查到天气，没有定位权限。你可以稍后再试，或者告诉我具体城市。", result.answer);
    }

    @Test
    public void nullErrorBuildsGenericFailureResult() {
        WeatherInteractionHandler handler = new WeatherInteractionHandler(new WeatherInteractionHandler.Source() {
            @Override public void queryCity(String city, WeatherTool.CallbackResult callback) {
                callback.onError(null);
            }

            @Override public void queryLocation(Location location, WeatherTool.CallbackResult callback) { }
        });
        List<ToolInteractionResult<WeatherTool.WeatherResult>> results = new ArrayList<>();

        handler.queryCity("深圳天气", "深圳", results::add);

        assertEquals(1, results.size());
        ToolInteractionResult<WeatherTool.WeatherResult> result = results.get(0);
        assertFalse(result.success);
        assertEquals("工具异常", result.errorMessage);
        assertTrue(result.fact.contains("查询失败：工具异常"));
        assertEquals("暂时没查到天气，工具异常。你可以稍后再试，或者告诉我具体城市。", result.answer);
    }

    @Test
    public void nullSuccessBuildsFailureResultInsteadOfCrashing() {
        WeatherInteractionHandler handler = new WeatherInteractionHandler(new WeatherInteractionHandler.Source() {
            @Override public void queryCity(String city, WeatherTool.CallbackResult callback) {
                callback.onSuccess(null);
            }

            @Override public void queryLocation(Location location, WeatherTool.CallbackResult callback) { }
        });
        List<ToolInteractionResult<WeatherTool.WeatherResult>> results = new ArrayList<>();

        handler.queryCity("深圳天气", "深圳", results::add);

        assertEquals(1, results.size());
        ToolInteractionResult<WeatherTool.WeatherResult> result = results.get(0);
        assertFalse(result.success);
        assertEquals("深圳天气", result.question);
        assertEquals("天气结果为空", result.errorMessage);
        assertTrue(result.fact.contains("查询失败：天气结果为空"));
        assertEquals("暂时没查到天气，天气结果为空。你可以稍后再试，或者告诉我具体城市。", result.answer);
    }

    @Test
    public void citySourceExceptionBuildsFailureResultInsteadOfCrashing() {
        WeatherInteractionHandler handler = new WeatherInteractionHandler(new WeatherInteractionHandler.Source() {
            @Override public void queryCity(String city, WeatherTool.CallbackResult callback) {
                throw new RuntimeException("city boom");
            }

            @Override public void queryLocation(Location location, WeatherTool.CallbackResult callback) { }
        });
        List<ToolInteractionResult<WeatherTool.WeatherResult>> results = new ArrayList<>();

        handler.queryCity("深圳天气", "深圳", results::add);

        assertEquals(1, results.size());
        ToolInteractionResult<WeatherTool.WeatherResult> result = results.get(0);
        assertFalse(result.success);
        assertEquals("深圳天气", result.question);
        assertEquals("city boom", result.errorMessage);
        assertTrue(result.fact.contains("查询失败：city boom"));
        assertEquals("暂时没查到天气，city boom。你可以稍后再试，或者告诉我具体城市。", result.answer);
    }

    @Test
    public void locationSourceExceptionWithoutMessageUsesGenericFailure() {
        WeatherInteractionHandler handler = new WeatherInteractionHandler(new WeatherInteractionHandler.Source() {
            @Override public void queryCity(String city, WeatherTool.CallbackResult callback) { }

            @Override public void queryLocation(Location location, WeatherTool.CallbackResult callback) {
                throw new RuntimeException();
            }
        });
        List<ToolInteractionResult<WeatherTool.WeatherResult>> results = new ArrayList<>();

        handler.queryLocation("当前位置天气", null, results::add);

        assertEquals(1, results.size());
        ToolInteractionResult<WeatherTool.WeatherResult> result = results.get(0);
        assertFalse(result.success);
        assertEquals("当前位置天气", result.question);
        assertEquals("工具异常", result.errorMessage);
        assertTrue(result.fact.contains("查询失败：工具异常"));
        assertEquals("暂时没查到天气，工具异常。你可以稍后再试，或者告诉我具体城市。", result.answer);
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
}
