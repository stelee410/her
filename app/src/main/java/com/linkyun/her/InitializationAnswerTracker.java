package com.linkyun.her;

import java.util.Locale;

final class InitializationAnswerTracker {
    private InitializationAnswerTracker() {
    }

    static int nextTurn(int currentTurn, String lastAssistant, String userText) {
        if (isInitializationSmallTalk(userText) || isShortConfirmation(userText)) return currentTurn;
        if (currentTurn <= 0) {
            if (asksForUserName(lastAssistant) || hasNameCue(userText)) return 1;
            return currentTurn;
        }
        if (currentTurn == 1) {
            if (asksForRelationship(lastAssistant) || isUsableProfileValue(relationshipFromText(userText))) {
                return 2;
            }
            return currentTurn;
        }
        if (currentTurn == 2) {
            if (asksForStory(lastAssistant) || isLikelyStoryAnswer(userText)) return 3;
        }
        return currentTurn;
    }

    static boolean asksForUserName(String text) {
        if (text == null) return false;
        return text.contains("叫什么") || text.contains("怎么称呼") ||
                text.contains("称呼你") || text.contains("你的名字") ||
                text.contains("你叫什么");
    }

    static boolean asksForRelationship(String text) {
        if (text == null) return false;
        return text.contains("什么样的关系") ||
                (text.contains("希望和我") && text.contains("关系")) ||
                (text.contains("希望我") && text.contains("关系")) ||
                text.contains("建立什么关系") || text.contains("我们是什么关系");
    }

    static boolean asksForStory(String text) {
        if (text == null) return false;
        return text.contains("你的故事") || text.contains("讲讲你") ||
                text.contains("讲讲自己") || text.contains("最近在忙") ||
                text.contains("在意的事") || text.contains("希望我记住");
    }

    static boolean isInitializationSmallTalk(String text) {
        if (text == null) return true;
        String normalized = normalizeShortReply(text);
        return normalized.equals("你好") || normalized.equals("哈喽") ||
                normalized.equals("hello") || normalized.equalsIgnoreCase("hi") ||
                normalized.equals("在吗") || normalized.equals("喂");
    }

    static boolean isShortConfirmation(String text) {
        String normalized = normalizeShortReply(text).toLowerCase(Locale.US);
        return normalized.equals("可以") || normalized.equals("可以呀") ||
                normalized.equals("可以啊") || normalized.equals("好") ||
                normalized.equals("好的") || normalized.equals("嗯") ||
                normalized.equals("嗯嗯") || normalized.equals("对") ||
                normalized.equals("是的") || normalized.equals("行") ||
                normalized.equals("没问题") || normalized.equals("ok") ||
                normalized.equals("yes");
    }

    static boolean hasNameCue(String text) {
        if (text == null) return false;
        return text.contains("我叫") || text.contains("我是") ||
                text.contains("你可以叫我") || text.contains("叫我") ||
                text.contains("称呼我");
    }

    static String relationshipFromText(String text) {
        if (text == null) return "";
        if (text.contains("女朋友") || text.contains("恋人") || text.contains("情侣")) return "女朋友";
        if (text.contains("男朋友")) return "男朋友";
        if (text.contains("助理") || text.contains("助手")) return "朋友和助理";
        if (text.contains("朋友")) return "朋友";
        return "";
    }

    static boolean isLikelyStoryAnswer(String text) {
        String normalized = normalizeShortReply(text);
        if (normalized.length() < 6) return false;
        if (isShortConfirmation(text)) return false;
        if (normalized.length() <= 12 && isUsableProfileValue(relationshipFromText(text))) return false;
        return true;
    }

    static String normalizeShortReply(String text) {
        if (text == null) return "";
        return text.trim()
                .replace("。", "")
                .replace("，", "")
                .replace(",", "")
                .replace(".", "")
                .replace("！", "")
                .replace("!", "")
                .replace("？", "")
                .replace("?", "")
                .replace("~", "")
                .replace("～", "")
                .trim();
    }

    private static boolean isUsableProfileValue(String value) {
        if (value == null) return false;
        String trimmed = value.trim();
        String lower = trimmed.toLowerCase(Locale.US);
        return !trimmed.isEmpty()
                && trimmed.length() <= 20
                && !trimmed.contains("未明确")
                && !trimmed.contains("不知道")
                && !trimmed.contains("没有")
                && !trimmed.equals("你好")
                && !trimmed.equals("哈喽")
                && !trimmed.equals("在吗")
                && !lower.equals("there")
                && !lower.equals("hi")
                && !lower.equals("hello");
    }
}
