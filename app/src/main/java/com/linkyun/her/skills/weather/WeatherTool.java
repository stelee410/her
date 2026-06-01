package com.linkyun.her;

import android.location.Location;
import android.os.Handler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

final class WeatherTool {
    interface CallbackResult {
        void onSuccess(WeatherResult result);
        void onError(String message);
    }

    private final OkHttpClient http;
    private final Handler main;

    WeatherTool(OkHttpClient http, Handler main) {
        this.http = http;
        this.main = main;
    }

    void queryCity(String city, CallbackResult callback) {
        String encoded = urlEncode(city);
        Request request = new Request.Builder()
                .url("https://geocoding-api.open-meteo.com/v1/search?name=" + encoded + "&count=1&language=zh&format=json")
                .build();
        http.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException error) {
                main.post(() -> callback.onError("城市定位失败：" + error.getMessage()));
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() == null ? "" : response.body().string();
                if (!response.isSuccessful()) {
                    main.post(() -> callback.onError("城市定位接口失败：" + response.code()));
                    return;
                }
                try {
                    JSONArray results = new JSONObject(body).optJSONArray("results");
                    if (results == null || results.length() == 0) {
                        main.post(() -> callback.onError("没有找到这个城市：" + city));
                        return;
                    }
                    JSONObject first = results.getJSONObject(0);
                    String resolvedName = first.optString("name", city);
                    double lat = first.getDouble("latitude");
                    double lon = first.getDouble("longitude");
                    queryCoordinates(resolvedName, lat, lon, callback);
                } catch (JSONException error) {
                    main.post(() -> callback.onError("解析城市定位失败"));
                }
            }
        });
    }

    void queryLocation(Location location, CallbackResult callback) {
        queryCoordinates("当前位置", location.getLatitude(), location.getLongitude(), callback);
    }

    private void queryCoordinates(String placeName, double lat, double lon, CallbackResult callback) {
        String url = String.format(Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.5f&longitude=%.5f&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m&timezone=auto",
                lat, lon);
        Request request = new Request.Builder().url(url).build();
        http.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException error) {
                main.post(() -> callback.onError("天气查询失败：" + error.getMessage()));
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() == null ? "" : response.body().string();
                if (!response.isSuccessful()) {
                    main.post(() -> callback.onError("天气接口失败：" + response.code()));
                    return;
                }
                try {
                    JSONObject current = new JSONObject(body).getJSONObject("current");
                    WeatherResult result = new WeatherResult(
                            placeName,
                            current.optDouble("temperature_2m", Double.NaN),
                            current.optDouble("apparent_temperature", Double.NaN),
                            current.optInt("relative_humidity_2m", -1),
                            current.optDouble("wind_speed_10m", Double.NaN),
                            conditionForCode(current.optInt("weather_code", -1)),
                            current.optString("time", ""));
                    main.post(() -> callback.onSuccess(result));
                } catch (JSONException error) {
                    main.post(() -> callback.onError("解析天气失败"));
                }
            }
        });
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException error) {
            return value;
        }
    }

    private static String conditionForCode(int code) {
        if (code == 0) return "晴";
        if (code == 1 || code == 2) return "少云";
        if (code == 3) return "阴";
        if (code == 45 || code == 48) return "有雾";
        if (code >= 51 && code <= 57) return "毛毛雨";
        if (code >= 61 && code <= 67) return "下雨";
        if (code >= 71 && code <= 77) return "下雪";
        if (code >= 80 && code <= 82) return "阵雨";
        if (code >= 85 && code <= 86) return "阵雪";
        if (code >= 95 && code <= 99) return "雷雨";
        return "天气状况未知";
    }

    static final class WeatherResult {
        final String placeName;
        final double temperatureC;
        final double apparentTemperatureC;
        final int humidity;
        final double windKmh;
        final String condition;
        final String observedAt;

        WeatherResult(String placeName, double temperatureC, double apparentTemperatureC,
                int humidity, double windKmh, String condition, String observedAt) {
            this.placeName = placeName;
            this.temperatureC = temperatureC;
            this.apparentTemperatureC = apparentTemperatureC;
            this.humidity = humidity;
            this.windKmh = windKmh;
            this.condition = condition;
            this.observedAt = observedAt;
        }

        String fact(String userQuestion) {
            return "【天气查询结果】用户刚才问：" + userQuestion + "\n" +
                    "地点：" + placeName + "\n" +
                    "天气：" + condition + "\n" +
                    "气温：" + oneDecimal(temperatureC) + "℃\n" +
                    "体感温度：" + oneDecimal(apparentTemperatureC) + "℃\n" +
                    "湿度：" + (humidity >= 0 ? humidity + "%" : "未知") + "\n" +
                    "风速：" + oneDecimal(windKmh) + " km/h\n" +
                    "观测时间：" + observedAt + "\n" +
                    "请直接用自然中文回答天气，不要说你不能查天气，也不要提工具调用。";
        }

        String shortAnswer() {
            return placeName + "现在" + condition + "，" +
                    oneDecimal(temperatureC) + "℃，体感" + oneDecimal(apparentTemperatureC) +
                    "℃，湿度" + (humidity >= 0 ? humidity + "%" : "未知") +
                    "，风速" + oneDecimal(windKmh) + " km/h。";
        }

        private static String oneDecimal(double value) {
            if (Double.isNaN(value)) return "未知";
            return String.format(Locale.US, "%.1f", value);
        }

        String temperatureText() {
            return oneDecimal(temperatureC) + "℃";
        }

        String apparentTemperatureText() {
            return oneDecimal(apparentTemperatureC) + "℃";
        }

        String humidityText() {
            return humidity >= 0 ? humidity + "%" : "未知";
        }

        String windText() {
            return oneDecimal(windKmh) + " km/h";
        }
    }
}
