package com.linkyun.her;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class InitializationSummaryRequestBuilder {
    private InitializationSummaryRequestBuilder() {
    }

    static Request build(String model, String agentName, String agentNameCandidates,
            String transcript) throws JSONException {
        String currentAgentName = clean(agentName);
        String candidates = clean(agentNameCandidates);
        String systemPrompt =
                "你是 Agent 的潜意识模型，负责把初始化访谈整理为长期记忆。只输出 JSON，不要解释。" +
                "格式：{\"agent_name\":\"...\",\"user_name\":\"...\",\"user_md\":\"...\",\"agent_md\":\"...\"}。" +
                "如果用户希望 Agent 自己取名，请选择一个像真人一样、洋气、短、适合长期陪伴使用的名字，并写入 agent_name。" +
                "推荐候选：" + candidates + "。禁止使用豆包、小包、包包、助手、AI、机器人、Doris 等平台感、幼稚或默认名。";
        String userPrompt =
                "用户预设的 Agent 名字：" + (currentAgentName.isEmpty() ? "未指定，请从初始化对话中自行确定" : currentAgentName) + "\n" +
                "请使用 c-her 总结以下初始化对话，生成 user.md 和 Agent.md。要求：\n" +
                "1. agent_name 是 Agent 最终名字；如果用户没有指定，而对话中表达希望 Agent 自己取名，你优先从这些候选中取一个，也可以选择同风格女性名字：" + candidates + "。不要使用豆包、小包、包包、Doris、助手、AI、机器人。\n" +
                "2. user_name 是用户希望被称呼的名字，必须从用户回答中提取。\n" +
                "3. user_md 包含用户姓名/称呼、用户希望和 Agent 的关系、用户的故事、沟通偏好、重要边界或目标。\n" +
                "4. agent_md 包含 Agent 的名字、默认语气、与该用户相处的关系定位、后续对话策略。\n" +
                "5. 信息不确定时写“未明确”，但不要把已明确回答的名字、关系写成未明确。\n" +
                "6. Markdown 内容要适合后续作为系统提示词注入。\n\n" +
                clean(transcript);

        JSONArray messages = new JSONArray();
        messages.put(message("system", systemPrompt));
        messages.put(message("user", userPrompt));

        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", 0);
        body.put("stream", false);
        return new Request(body, systemPrompt, userPrompt);
    }

    private static JSONObject message(String role, String content) throws JSONException {
        JSONObject message = new JSONObject();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    static final class Request {
        final JSONObject body;
        final String systemPrompt;
        final String userPrompt;

        Request(JSONObject body, String systemPrompt, String userPrompt) {
            this.body = body;
            this.systemPrompt = systemPrompt;
            this.userPrompt = userPrompt;
        }
    }
}
