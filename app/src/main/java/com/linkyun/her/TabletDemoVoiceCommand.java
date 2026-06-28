package com.linkyun.her;

import java.util.Locale;

final class TabletDemoVoiceCommand {
    private TabletDemoVoiceCommand() {
    }

    static String normalize(String text) {
        if (text == null) return "";
        return text.replace("，", "")
                .replace("。", "")
                .replace("！", "")
                .replace("？", "")
                .replace(",", "")
                .replace(".", "")
                .replace("!", "")
                .replace("?", "")
                .trim();
    }

    static boolean shouldReplayGreeting(String normalized) {
        if (normalized == null) return false;
        return normalized.contains("打个招呼") ||
                normalized.contains("打声招呼") ||
                normalized.contains("打招呼") ||
                normalized.contains("重新打招呼") ||
                normalized.contains("再打个招呼");
    }

    static boolean shouldChangeAvatar(String normalized) {
        if (normalized == null) return false;
        return normalized.contains("换个形象") ||
                normalized.contains("换一个形象") ||
                normalized.contains("换角色") ||
                normalized.contains("换一个角色") ||
                normalized.contains("换下一个角色") ||
                normalized.contains("切换角色") ||
                normalized.contains("下一个角色") ||
                normalized.contains("换个人") ||
                normalized.contains("换一个人") ||
                normalized.contains("换套衣服") ||
                normalized.contains("换身衣服") ||
                normalized.contains("换装");
    }

    static boolean shouldRegisterIdentity(String normalized) {
        if (normalized == null) return false;
        return normalized.contains("登记身份") ||
                normalized.contains("注册身份") ||
                normalized.contains("绑定身份") ||
                normalized.contains("录入身份") ||
                normalized.contains("登记这张卡") ||
                normalized.contains("绑定这张卡");
    }

    static boolean shouldShowHiddenJess(String normalized) {
        if (normalized == null) return false;
        String lower = normalized.toLowerCase(Locale.US);
        return lower.contains("jess") ||
                normalized.contains("杰西卡") ||
                normalized.contains("隐藏人物") ||
                normalized.contains("隐藏角色") ||
                normalized.contains("打开我的秘密") ||
                normalized.contains("叫Jess出来") ||
                normalized.contains("叫杰西卡出来");
    }
}
