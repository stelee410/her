package com.linkyun.her;

final class ConversationInstructionsComposer {
    private ConversationInstructionsComposer() {
    }

    static String normal(String agentName, String userName, String userMemory,
            String agentMemory, String dynamicTone, String conversationMemory,
            String recentDialogue, String latestWeatherFact, boolean pendingRealtimeWeatherAnswer,
            String latestNewsFact, boolean pendingRealtimeNewsAnswer,
            String baseInstructions, int limit) {
        return PromptMemoryComposer.compose(
                agentName,
                userName,
                userMemory,
                agentMemory,
                dynamicTone,
                conversationMemory,
                recentDialogue,
                toolPromptBlocks(latestWeatherFact, pendingRealtimeWeatherAnswer,
                        latestNewsFact, pendingRealtimeNewsAnswer),
                baseInstructions,
                limit);
    }

    static String initialization(String agentName, String candidates, int userTurns,
            String baseInstructions, String initBasePrompt) {
        String currentAgentName = clean(agentName);
        String candidateText = clean(candidates);
        String identityLine = currentAgentName.isEmpty()
                ? "你还没有固定名字。请在首次主动介绍自己时，为自己取一个像真人一样、洋气、自然、短、适合长期陪伴用户的女性名字，并在后续初始化中保持使用这个名字。推荐候选本轮已随机排序：" + candidateText + "。你也可以选择同风格名字；禁止使用豆包、小包、包包、助手、AI、机器人、Doris 等平台感、幼稚或默认名。\n"
                : "你叫 " + currentAgentName + "。\n";
        return identityLine +
                safe(baseInstructions) + "\n" +
                safe(initBasePrompt) + "\n" +
                "第 0 步：你必须先主动介绍自己，说清楚你是一个 AI Agent，也是用户的朋友和助理。如果还没有固定名字，请你自己取一个像真人一样、洋气、自然的女性名字并告诉用户。本轮随机排序候选包括：" + candidateText + "。你也可以选择同风格名字；不要使用豆包、Doris 或平台/助手感名字。\n" +
                "第 1 题：用户的名字，以及希望你怎么称呼用户。\n" +
                "第 2 题：用户希望和你建立什么关系。\n" +
                "第 3 题：用户的故事，一段开放式自我介绍，包括近况、经历、在意的事或希望你记住的部分。\n" +
                "第三题回答后，不要再输出新的轮次；客户端会关闭语音交互模型，并交给潜意识模型写入 user.md 和 Agent.md。\n" +
                initializationGuide(currentAgentName, userTurns);
    }

    static String initializationGuide(String agentName, int userTurns) {
        String currentAgentName = clean(agentName);
        if (userTurns <= 0) {
            if (currentAgentName.isEmpty()) {
                return "当前阶段：还没有收到用户回答。收到系统事件后，你必须主动问候用户，先用第一人称告诉用户你给自己取的名字，然后只问第 1 题：用户的名字，以及希望你怎么称呼用户。名字必须像真人，不要叫豆包、Doris 或任何平台/助手感名字。";
            }
            return "当前阶段：还没有收到用户回答。收到系统事件后，你必须主动问候用户，介绍自己是 " + currentAgentName + "，然后只问第 1 题：用户的名字，以及希望你怎么称呼用户。";
        }
        if (userTurns == 1) {
            return "当前阶段：已经收到第 1 题答案。你的下一次回复只能简短回应，然后问第 2 题：用户希望和你建立什么关系。";
        }
        if (userTurns == 2) {
            return "当前阶段：已经收到第 2 题答案。你的下一次回复只能简短回应，然后问第 3 题：请用户开放地讲讲自己的故事、近况、在意的事，或希望你记住的部分。";
        }
        return "当前阶段：三题都已回答。不要继续聊天或提问，只说明你正在写入 user.md。";
    }

    static String toolPromptBlocks(String latestWeatherFact, boolean pendingRealtimeWeatherAnswer,
            String latestNewsFact, boolean pendingRealtimeNewsAnswer) {
        return WeatherSkill.promptBlock(latestWeatherFact, pendingRealtimeWeatherAnswer) +
                NewsSkill.promptBlock(latestNewsFact, pendingRealtimeNewsAnswer);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
