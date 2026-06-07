package com.linkyun.her;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

import java.util.Date;

public final class InitializationSummaryCompletionTest {
    @Test
    public void buildsPersistableNamesAndMemoryDocuments() throws Exception {
        JSONObject content = new JSONObject();
        content.put("agent_name", "Luna");
        content.put("user_name", "史蒂芬");
        content.put("user_md", "## 基本信息\n- 用户希望和 Agent 的关系：未明确");
        content.put("agent_md", "- Agent name: Luna\n- 与用户的关系定位：未明确");
        String transcript = "assistant: 你希望和我建立什么关系？\n" +
                "user: 我希望你像女朋友一样陪我\n";

        InitializationSummaryCompletion.Result result = InitializationSummaryCompletion.complete(
                content.toString(), transcript, "", "Mira", "Doris", "默认用户", new Date(0));

        assertEquals("Luna", result.agentName);
        assertEquals("史蒂芬", result.userName);
        assertEquals("女朋友", result.profile.relationship);
        assertTrue(result.userMemory.startsWith("# user.md\n\n- Agent name: Luna\n- Created at: "));
        assertTrue(result.userMemory.contains("用户希望和 Agent 的关系：女朋友"));
        assertTrue(result.agentMemory.startsWith("# Agent.md\n\n- Agent name: Luna\n- Created at: "));
        assertTrue(result.agentMemory.contains("史蒂芬的女朋友"));
    }

    @Test
    public void keepsCurrentDisplayNameWhenSummaryOmitsUserName() {
        InitializationSummaryCompletion.Result result = InitializationSummaryCompletion.complete(
                "{}", "", "", "Mira", "Doris", "当前用户", new Date(0));

        assertEquals("Mira", result.agentName);
        assertEquals("当前用户", result.userName);
        assertTrue(result.userMemory.contains("- Agent name: Mira"));
        assertTrue(result.agentMemory.contains("- Agent name: Mira"));
    }
}
