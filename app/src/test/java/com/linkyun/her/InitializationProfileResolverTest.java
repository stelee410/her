package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

import java.util.Date;

public final class InitializationProfileResolverTest {
    @Test
    public void resolvesJsonProfileAndFillsMissingRelationship() {
        String content = "{\"agent_name\":\"Luna\",\"user_name\":\"史蒂芬\",\"user_md\":\"## 基本信息\\n- 用户希望和 Agent 的关系：未明确\",\"agent_md\":\"- Agent name: Luna\\n- 与用户的关系定位：未明确\"}";
        String transcript = "assistant: 你希望和我建立什么关系？\nuser: 我希望你像女朋友一样陪我\n";

        InitializationProfileResolver.Profile profile = InitializationProfileResolver.resolve(
                content, transcript, "", "Mira", "Doris");

        assertEquals("Luna", profile.agentName);
        assertEquals("史蒂芬", profile.userName);
        assertEquals("女朋友", profile.relationship);
        assertTrue(profile.userMd.contains("用户希望和 Agent 的关系：女朋友"));
        assertTrue(profile.agentMd.contains("史蒂芬的女朋友"));
    }

    @Test
    public void fallsBackToTranscriptUserNameAndAgentName() {
        String transcript = "assistant: 我叫 Nora，很高兴认识你。你叫什么？\n" +
                "user: 我叫史蒂芬\n" +
                "assistant: 你希望和我建立什么关系？\n" +
                "user: 朋友和助理\n";

        InitializationProfileResolver.Profile profile = InitializationProfileResolver.resolve(
                "{}", transcript, "", "Mira", "Doris");

        assertEquals("Nora", profile.agentName);
        assertEquals("史蒂芬", profile.userName);
        assertEquals("朋友和助理", profile.relationship);
        assertTrue(profile.userMd.contains("姓名/称呼：史蒂芬"));
    }

    @Test
    public void rejectsPlatformAgentNameAndUsesFallback() {
        String content = "{\"agent_name\":\"Doris\",\"user_name\":\"\",\"user_md\":\"{}\",\"agent_md\":\"{}\"}";

        InitializationProfileResolver.Profile profile = InitializationProfileResolver.resolve(
                content, "", "", "Mira", "Doris");

        assertEquals("Mira", profile.agentName);
        assertTrue(profile.agentMd.contains("Agent name: Mira"));
    }

    @Test
    public void rawMarkdownCanProvideUserName() {
        String content = "# user.md\n- 姓名/称呼：小林\n";

        InitializationProfileResolver.Profile profile = InitializationProfileResolver.resolve(
                content, "", "Clara", "Mira", "Doris");

        assertEquals("Clara", profile.agentName);
        assertEquals("小林", profile.userName);
        assertTrue(profile.userMd.contains("小林"));
    }

    @Test
    public void emptyJsonUserNameDoesNotFallThroughToNextField() {
        String content = "{\"user_name\":\"\",\"user_md\":\"## 基本信息\\n- 重要边界：不要假装已经记住未说过的事。\"}";

        assertEquals("", InitializationProfileResolver.extractUserName(content));
    }

    @Test
    public void profileBuildsMemoryDocumentsAndDisplayName() {
        InitializationProfileResolver.Profile profile = InitializationProfileResolver.resolve(
                "{}", "", "", "Mira", "Doris");
        Date createdAt = new Date(0);

        assertTrue(profile.userMemory(createdAt).startsWith("# user.md\n\n- Agent name: Mira\n- Created at: "));
        assertTrue(profile.agentMemory(createdAt).startsWith("# Agent.md\n\n- Agent name: Mira\n- Created at: "));
        assertTrue(profile.userMemory(createdAt).contains("## 基本信息"));
        assertTrue(profile.agentMemory(createdAt).contains("- Agent name: Mira"));
        assertEquals("fallback", profile.displayUserName("fallback"));
    }

    @Test
    public void appendsExplicitSettingsWhenLongMarkdownMissesTranscriptFacts() throws Exception {
        JSONObject contentObject = new JSONObject();
        contentObject.put("agent_name", "Mira");
        contentObject.put("user_name", "");
        contentObject.put("user_md", "## 基本信息\n- 喜欢夜里聊天，也希望对话自然一点。\n- 重要边界：不要假装已经记住未说过的事。");
        contentObject.put("agent_md", "- Agent name: Mira\n- 默认语气：成熟、温柔、亲近，像真实的人一样自然表达。");
        String content = contentObject.toString();
        String transcript = "assistant: 你叫什么？\n" +
                "user: 我叫史蒂芬\n" +
                "assistant: 你希望和我建立什么关系？\n" +
                "user: 我希望你是我的女朋友\n";

        InitializationProfileResolver.Profile profile = InitializationProfileResolver.resolve(
                content, transcript, "", "Clara", "Doris");

        assertEquals("史蒂芬", profile.userName);
        assertEquals("女朋友", profile.relationship);
        assertTrue(profile.userMd, profile.userMd.contains("## 初始化显式设定"));
        assertTrue(profile.userMd, profile.userMd.contains("姓名/称呼：史蒂芬"));
        assertTrue(profile.agentMd, profile.agentMd.contains("## 初始化显式设定"));
        assertTrue(profile.agentMd, profile.agentMd.contains("史蒂芬的女朋友"));
    }
}
