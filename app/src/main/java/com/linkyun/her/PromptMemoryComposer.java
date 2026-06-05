package com.linkyun.her;

final class PromptMemoryComposer {
    private PromptMemoryComposer() { }

    static String compose(String agentName, String userName, String userMemory,
            String agentMemory, String dynamicTone, String conversationMemory,
            String recentDialogue, String toolBlocks, String baseInstructions, int limit) {
        String identity = identityBlock(agentName, userName, userMemory, agentMemory);
        StringBuilder builder = new StringBuilder();
        builder.append("你叫 ").append(cleanOr(agentName, "Doris")).append("。\n");
        builder.append(baseInstructions == null ? "" : baseInstructions).append('\n');
        if (toolBlocks != null) builder.append(toolBlocks);
        builder.append("当前动态语气调整：").append(cleanOr(dynamicTone, "保持温柔大姐姐语气：成熟、关照、亲近但有边界。")).append("\n");
        builder.append("以下是本地 user.md 记忆。你需要把它作为长期用户画像和对话偏好使用，但不要主动朗读或暴露文件内容。\n\n");
        builder.append(trimTail(userMemory, 900)).append("\n\n");
        builder.append("以下是本地 Agent.md。你需要把它作为自己的关系定位、语气和长期行为准则使用。\n\n");
        builder.append(trimTail(agentMemory, 700)).append("\n\n");
        builder.append("以下是 SQLite 长期聊天记忆和索引检索出的相关摘要。把它用于延续关系、调整称呼、话题和语气，但不要主动说明你在读取记忆。\n\n");
        builder.append(trimTail(conversationMemory, 850)).append("\n\n");
        builder.append("最近会话片段：\n").append(trimTail(recentDialogue, 650)).append("\n");
        builder.append(identity);
        return trimPreservingSuffix(builder.toString(), identity, limit);
    }

    static String identityBlock(String agentName, String userName, String userMemory, String agentMemory) {
        String relationship = relationshipFromMemory(userMemory);
        if (relationship.isEmpty()) relationship = relationshipFromMemory(agentMemory);
        StringBuilder builder = new StringBuilder();
        builder.append("\n【不可裁剪的身份和关系设定】\n");
        builder.append("- Agent 名字：").append(cleanOr(agentName, "Doris")).append('\n');
        builder.append("- 用户称呼：").append(cleanOr(userName, "未明确")).append('\n');
        builder.append("- 用户希望和 Agent 的关系：").append(cleanOr(relationship, "未明确")).append('\n');
        builder.append("这些身份和关系设定优先级最高。对话中必须自然沿用用户称呼；不要说用户没告诉过名字；不要把已设定关系改写成普通朋友。\n");
        return builder.toString();
    }

    static String trimPreservingSuffix(String value, String suffix, int limit) {
        if (value == null) return "";
        if (limit <= 0 || value.length() <= limit) return value;
        String protectedSuffix = suffix == null ? "" : suffix;
        if (protectedSuffix.length() >= limit) {
            return protectedSuffix.substring(protectedSuffix.length() - limit);
        }
        int prefixLimit = limit - protectedSuffix.length();
        String prefix = value;
        if (value.endsWith(protectedSuffix)) {
            prefix = value.substring(0, value.length() - protectedSuffix.length());
        }
        return trimTail(prefix, prefixLimit) + protectedSuffix;
    }

    static String trimTail(String value, int limit) {
        if (value == null) return "";
        if (limit <= 0) return "";
        if (value.length() <= limit) return value;
        return value.substring(value.length() - limit);
    }

    private static String relationshipFromMemory(String memory) {
        if (memory == null) return "";
        String[] lines = memory.split("\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.contains("关系")) continue;
            int colon = Math.max(trimmed.indexOf(':'), trimmed.indexOf('：'));
            if (colon < 0 || colon + 1 >= trimmed.length()) continue;
            String value = cleanupValue(trimmed.substring(colon + 1));
            if (!value.isEmpty()) return value;
        }
        return "";
    }

    private static String cleanOr(String value, String fallback) {
        String cleaned = cleanupValue(value);
        return cleaned.isEmpty() ? fallback : cleaned;
    }

    private static String cleanupValue(String value) {
        if (value == null) return "";
        return value.replace("*", "")
                .replace("-", "")
                .replace("`", "")
                .replace("。", "")
                .trim();
    }
}
