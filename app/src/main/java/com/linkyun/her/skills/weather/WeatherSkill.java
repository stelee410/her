package com.linkyun.her;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;

import java.util.Locale;

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

    static boolean shouldReuseLatestFact(String text, boolean hasLatestWeatherFact) {
        if (!hasLatestWeatherFact || text == null) return false;
        String value = text.trim();
        if (value.isEmpty()) return false;
        if (hasExplicitRefreshIntent(value)) return false;
        if (hasFutureDayShift(value)) return false;
        if (hasExplicitPlaceBeforeWeatherTerm(value)) return false;
        return hasWeatherReuseCue(value);
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

    private static boolean hasExplicitRefreshIntent(String value) {
        return value.contains("重新查") ||
                value.contains("再查") ||
                value.contains("查一下") ||
                value.contains("查一查") ||
                value.contains("查下") ||
                value.contains("查查") ||
                value.contains("帮我查") ||
                value.contains("重新看") ||
                value.contains("再看") ||
                value.contains("换成");
    }

    private static boolean hasFutureDayShift(String value) {
        return value.contains("明天") ||
                value.contains("后天") ||
                value.contains("大后天") ||
                value.contains("下周") ||
                value.contains("周一") ||
                value.contains("周二") ||
                value.contains("周三") ||
                value.contains("周四") ||
                value.contains("周五") ||
                value.contains("周六") ||
                value.contains("周日") ||
                value.contains("星期") ||
                value.contains("礼拜");
    }

    private static boolean hasExplicitPlaceBeforeWeatherTerm(String value) {
        String[] terms = {"天气", "气温", "温度"};
        for (String term : terms) {
            int index = value.indexOf(term);
            if (index <= 0) continue;
            String prefix = value.substring(0, index)
                    .replace("帮我", "")
                    .replace("请问", "")
                    .replace("看一下", "")
                    .replace("查一下", "")
                    .replace("的", "")
                    .trim();
            if (prefix.length() >= 2 && prefix.length() <= 12 && !isGenericWeatherPrefix(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isGenericWeatherPrefix(String prefix) {
        return prefix.isEmpty() ||
                "今天".equals(prefix) ||
                "现在".equals(prefix) ||
                "今晚".equals(prefix) ||
                "下午".equals(prefix) ||
                "晚上".equals(prefix) ||
                "刚才".equals(prefix) ||
                "刚刚".equals(prefix) ||
                "那".equals(prefix) ||
                "这个".equals(prefix) ||
                "当地".equals(prefix) ||
                "这里".equals(prefix) ||
                "我这里".equals(prefix) ||
                "我这边".equals(prefix);
    }

    private static boolean hasWeatherReuseCue(String value) {
        return value.contains("天气") ||
                value.contains("气温") ||
                value.contains("温度") ||
                value.contains("下雨") ||
                value.contains("会不会雨") ||
                value.contains("冷") ||
                value.contains("热") ||
                value.contains("带伞") ||
                value.contains("穿") ||
                value.contains("出门") ||
                value.contains("适合") ||
                value.toLowerCase(Locale.US).contains("weather");
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
        return "【天气查询结果】查询失败：" + failureMessage(message) + "。请向用户说明暂时没查到，并请用户稍后重试或指定城市。";
    }

    static boolean isTransientMemory(String text) {
        return text != null && (text.contains("【天气查询结果】") || text.contains("【系统事件】天气查询"));
    }

    static String failureMessage(String message) {
        if (message == null) return "工具异常";
        String clean = message.replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        return clean.isEmpty() ? "工具异常" : clean;
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
        OneShotCallbackGuard guard = new OneShotCallbackGuard();
        LocationListener listener = new LocationListener() {
            @Override public void onLocationChanged(Location location) {
                if (!guard.tryComplete()) return;
                manager.removeUpdates(this);
                success.onLocation(location);
            }

            @Override public void onProviderDisabled(String provider) { }
            @Override public void onProviderEnabled(String provider) { }
        };
        try {
            manager.requestSingleUpdate(provider, listener, Looper.getMainLooper());
            main.postDelayed(() -> {
                if (!guard.tryComplete()) return;
                manager.removeUpdates(listener);
                error.onError("暂时拿不到当前位置，请告诉我城市名。");
            }, 6500);
        } catch (IllegalArgumentException | SecurityException exception) {
            if (guard.tryComplete()) {
                error.onError("暂时拿不到当前位置，请告诉我城市名。");
            }
        }
    }

    interface LocationCallback {
        void onLocation(Location location);
    }

    interface ErrorCallback {
        void onError(String message);
    }
}
