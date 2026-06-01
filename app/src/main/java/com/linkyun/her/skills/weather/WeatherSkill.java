package com.linkyun.her;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class WeatherSkill {
    static final long VOICE_CARD_TIMEOUT_MS = 30_000;
    static final String SUCCESS_BROADCAST_PROMPT =
            "【系统事件】天气查询完成。请立刻根据当前临时工具结果播报给用户，只回答天气结果本身，不要提到系统事件、fact 或工具调用。";
    static final String FAILURE_BROADCAST_PROMPT =
            "【系统事件】天气查询失败。请根据当前临时工具结果向用户简短说明，没有查到天气，并请用户稍后重试或指定城市。不要提到系统事件、fact 或工具调用。";

    private WeatherSkill() { }

    static boolean isWeatherQuestion(String text) {
        if (text == null) return false;
        String value = text.trim();
        if (value.isEmpty()) return false;
        boolean hasWeatherTerm = value.contains("天气") ||
                value.contains("气温") ||
                value.contains("温度") ||
                value.contains("下雨") ||
                value.contains("会不会雨") ||
                value.toLowerCase(Locale.US).contains("weather");
        if (!hasWeatherTerm) return false;
        if (isWeatherFollowup(value) && !hasFreshQueryIntent(value)) return false;
        return true;
    }

    static String extractCity(String text) {
        if (text == null) return "";
        String normalized = text.replace("的天气", "天气")
                .replace("天气怎么样", "天气")
                .replace("天气如何", "天气");
        Pattern pattern = Pattern.compile("([\\u4e00-\\u9fa5A-Za-z]{2,24})(?:天气|气温|温度|会下雨|下雨)");
        Matcher matcher = pattern.matcher(normalized);
        String city = "";
        while (matcher.find()) {
            String candidate = cleanCityCandidate(matcher.group(1));
            if (!candidate.isEmpty()) city = candidate;
        }
        return city;
    }

    private static boolean isWeatherFollowup(String value) {
        return value.contains("刚才") ||
                value.contains("查出来") ||
                value.contains("没看到") ||
                value.contains("没有看到") ||
                value.contains("小卡片") ||
                value.contains("卡片");
    }

    private static boolean hasFreshQueryIntent(String value) {
        return value.contains("重新查") ||
                value.contains("再查") ||
                value.contains("查一下") ||
                value.contains("查查") ||
                value.contains("帮我查") ||
                value.contains("现在") ||
                value.contains("今天") ||
                value.contains("今晚") ||
                value.contains("明天") ||
                value.contains("怎么样") ||
                value.contains("如何") ||
                value.contains("会不会");
    }

    private static String cleanCityCandidate(String value) {
        if (value == null) return "";
        String city = value
                .replace("帮我重新查一下", "")
                .replace("帮我再查一下", "")
                .replace("帮我查一下", "")
                .replace("帮我查查", "")
                .replace("帮我查", "")
                .replace("查一下", "")
                .replace("查查", "")
                .replace("帮我重新", "")
                .replace("帮我再", "")
                .replace("重新查", "")
                .replace("再查", "")
                .replace("重新", "")
                .replace("今天", "")
                .replace("明天", "")
                .replace("后天", "")
                .replace("昨天", "")
                .replace("现在", "")
                .replace("今晚", "")
                .replace("晚上", "")
                .replace("早上", "")
                .replace("上午", "")
                .replace("中午", "")
                .replace("下午", "")
                .replace("当地", "")
                .replace("这里", "")
                .replace("我这边", "")
                .replace("我这里", "")
                .replace("请问", "")
                .replace("你刚才", "")
                .replace("刚才", "")
                .replace("可是", "")
                .replace("我没有看到", "")
                .replace("没有看到", "")
                .replace("没看到", "")
                .replace("小卡片", "")
                .replace("卡片", "")
                .trim();
        if (city.length() > 8 && city.endsWith("的")) city = city.substring(0, city.length() - 1);
        if (city.endsWith("的")) city = city.substring(0, city.length() - 1);
        if (city.length() < 2) return "";
        if (isInvalidCityCandidate(city)) return "";
        return city;
    }

    private static boolean isInvalidCityCandidate(String city) {
        return "天气".equals(city) ||
                "气温".equals(city) ||
                "温度".equals(city) ||
                "当地".equals(city) ||
                "这里".equals(city) ||
                "今天".equals(city) ||
                "明天".equals(city) ||
                "晚上".equals(city) ||
                "刚才".equals(city) ||
                "你刚才".equals(city) ||
                city.contains("看到") ||
                city.contains("卡片");
    }

    static String promptBlock(String latestWeatherFact, boolean pendingRealtimeWeatherAnswer) {
        String weatherInstruction = "天气查询规则：当用户询问天气、气温、温度或下雨时，如果没有最新天气查询结果，" +
                "不要编造天气，只简短说“稍等，我查一下天气”。客户端会查询天气并通过 fact 更新给你。" +
                "如果 fact 或下方临时工具结果中包含【天气查询结果】，你必须优先根据该结果直接回答用户刚才的天气问题，不要说不能查天气，不要提工具调用。\n";
        if (latestWeatherFact == null || latestWeatherFact.trim().isEmpty()) return weatherInstruction;
        return weatherInstruction +
                "当前临时工具结果：\n" + latestWeatherFact + "\n" +
                (pendingRealtimeWeatherAnswer ? "下一次发言请优先回答这个天气问题。\n" : "");
    }

    static String failureFact(String message) {
        return "【天气查询结果】查询失败：" + message + "。请向用户说明暂时没查到，并请用户稍后重试或指定城市。";
    }

    static boolean isTransientMemory(String text) {
        return text != null && (text.contains("【天气查询结果】") || text.contains("【系统事件】天气查询"));
    }

    static Location bestLastLocation(LocationManager manager) {
        Location best = null;
        for (String provider : manager.getProviders(true)) {
            try {
                Location location = manager.getLastKnownLocation(provider);
                if (location == null) continue;
                if (best == null || location.getTime() > best.getTime()) best = location;
            } catch (SecurityException ignored) { }
        }
        return best;
    }

    static void requestSingleLocation(LocationManager manager, Handler main,
            LocationCallback success, ErrorCallback error) {
        String provider = manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                ? LocationManager.NETWORK_PROVIDER
                : LocationManager.GPS_PROVIDER;
        LocationListener listener = new LocationListener() {
            @Override public void onLocationChanged(Location location) {
                manager.removeUpdates(this);
                success.onLocation(location);
            }

            @Override public void onProviderDisabled(String provider) { }
            @Override public void onProviderEnabled(String provider) { }
        };
        try {
            manager.requestSingleUpdate(provider, listener, Looper.getMainLooper());
            main.postDelayed(() -> {
                manager.removeUpdates(listener);
                error.onError("暂时拿不到当前位置，请告诉我城市名。");
            }, 6500);
        } catch (IllegalArgumentException | SecurityException exception) {
            error.onError("暂时拿不到当前位置，请告诉我城市名。");
        }
    }

    interface LocationCallback {
        void onLocation(Location location);
    }

    interface ErrorCallback {
        void onError(String message);
    }
}
