package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class WeatherToolTest {
    @Test
    public void resultConstructorNormalizesNullableTextFields() {
        WeatherTool.WeatherResult result = new WeatherTool.WeatherResult(
                null,
                Double.NaN,
                Double.NaN,
                -1,
                Double.NaN,
                "  多云\u00A0转晴  ",
                null);

        assertEquals("", result.placeName);
        assertEquals("多云 转晴", result.condition);
        assertEquals("", result.observedAt);
        assertEquals("未知℃", result.temperatureText());
        assertEquals("未知℃", result.apparentTemperatureText());
        assertEquals("未知", result.humidityText());
        assertEquals("未知 km/h", result.windText());
    }

    @Test
    public void normalizedResultBuildsSafeFactAndAnswer() {
        WeatherTool.WeatherResult result = new WeatherTool.WeatherResult(
                null,
                24.5,
                25.1,
                60,
                8.2,
                null,
                null);

        assertTrue(result.fact("天气").contains("地点："));
        assertTrue(result.fact("天气").contains("天气："));
        assertEquals("现在，24.5℃，体感25.1℃，湿度60%，风速8.2 km/h。", result.shortAnswer());
    }

    @Test
    public void failureFactUsesGenericMessageForBlankInput() {
        assertTrue(WeatherSkill.failureFact(" \u00A0 ").contains("查询失败：工具异常"));
        assertEquals("工具异常", WeatherSkill.failureMessage(null));
    }
}
