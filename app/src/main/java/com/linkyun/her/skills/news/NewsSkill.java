package com.linkyun.her;

import java.util.Locale;
import java.util.regex.Pattern;

final class NewsSkill {
    static final long VOICE_CARD_TIMEOUT_MS = 30_000;
    static final String LOOKUP_ACK_PROMPT =
            "【系统事件】用户要查询新闻热点。请只说“请稍等，我正在帮你准备，一会儿就可以了哦”。不要说任何新闻内容，不要继续播报，不要使用历史上下文，不要提到系统事件或工具调用。";
    static final String SUCCESS_BROADCAST_PROMPT =
            "【系统事件】每日新闻热点读取完成。请立刻根据当前临时工具结果里的编号列表，用自然中文向用户播报最新新闻摘要。只回答列表里的新闻热点本身，不要补充列表外新闻，不要提到系统事件、fact 或工具调用。";
    static final String FAILURE_BROADCAST_PROMPT =
            "【系统事件】每日新闻热点读取失败。请根据当前临时工具结果向用户简短说明，暂时没读到新闻热点。不要提到系统事件、fact 或工具调用。";
    private static final Pattern NEWS_QUERY_PATTERN = Pattern.compile(
            "(查|查询|查看|看|看看|读|念|播|播报|讲|说).{0,8}(新闻|热点)|" +
                    "(新闻|热点).{0,8}(查|查询|查看|看|看看|读|念|播|播报|讲|说)");

    private NewsSkill() { }

    static boolean isNewsQuestion(String text) {
        if (text == null) return false;
        String value = text.trim();
        if (value.isEmpty()) return false;
        String normalized = value.replaceAll("\\s+", "");
        String lower = normalized.toLowerCase(Locale.US);
        return value.contains("每日新闻热点") ||
                value.contains("每日热点新闻") ||
                value.contains("今日新闻热点") ||
                value.contains("今日热点新闻") ||
                value.contains("今天新闻热点") ||
                value.contains("今天热点新闻") ||
                value.contains("新闻热点") ||
                value.contains("热点新闻") ||
                value.contains("今日热点") ||
                value.contains("今天热点") ||
                value.contains("每日热点") ||
                value.contains("最近热点") ||
                value.contains("有什么新闻") ||
                value.contains("看看新闻") ||
                value.contains("查新闻") ||
                value.contains("查一下新闻") ||
                value.contains("查下新闻") ||
                value.contains("查一查新闻") ||
                value.contains("查看新闻") ||
                value.contains("查看一下新闻") ||
                value.contains("查询新闻") ||
                value.contains("查询一下新闻") ||
                value.contains("查热点") ||
                value.contains("查一下热点") ||
                value.contains("查下热点") ||
                value.contains("查一查热点") ||
                value.contains("查看热点") ||
                value.contains("查看一下热点") ||
                value.contains("查询热点") ||
                value.contains("查询一下热点") ||
                NEWS_QUERY_PATTERN.matcher(normalized).find() ||
                lower.contains("daily news") ||
                lower.contains("news hotspots");
    }

    static String promptBlock(String latestNewsFact, boolean pendingRealtimeNewsAnswer) {
        return "【最高优先级：新闻/热点查询规则】\n" +
                "当用户表达任何查询、查看、收听、播报新闻或热点的意图时，例如“查新闻”“查一下新闻”“看看今天热点”“有什么新闻”“每日新闻热点”等，" +
                "你必须只回答一句：“请稍等，我正在帮你准备，一会儿就可以了哦”。\n" +
                "不要播报任何新闻标题或摘要；不要根据历史上下文、记忆、旧 fact 或猜测回答新闻内容；不要说你已经查到了；不要继续展开；不再继续播报。\n" +
                "新闻查询由后台 c-her 工具执行器读取 https://agentnews.linkyun.co/。\n" +
                "只有当你收到“【系统事件】每日新闻热点读取完成”时，才可以根据当前临时工具结果播报新闻；这时必须只使用刚刚注入的每日新闻热点结果，不要使用历史记忆或旧新闻。\n";
    }

    static String failureFact(String message) {
        return "【每日新闻热点结果】读取失败：" + failureMessage(message) + "。请向用户说明暂时没读到新闻热点，并请用户稍后重试。";
    }

    static boolean isTransientMemory(String text) {
        return text != null && (text.contains("【每日新闻热点结果】") || text.contains("【系统事件】每日新闻热点"));
    }

    static String failureMessage(String message) {
        if (message == null) return "工具异常";
        String clean = message.replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        return clean.isEmpty() ? "工具异常" : clean;
    }
}
