package com.linkyun.her;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ConversationInstructionsComposerTest {
    @Test
    public void initializationWithoutAgentNameAsksAgentToSelfNameAndOnlyAskFirstQuestion() {
        String instructions = ConversationInstructionsComposer.initialization(
                "", "Mira, Nora", 0, "基础指令", "初始化基础提示");

        assertTrue(instructions.startsWith("你还没有固定名字。"));
        assertTrue(instructions.contains("推荐候选本轮已随机排序：Mira, Nora"));
        assertTrue(instructions.contains("禁止使用豆包、小包、包包、助手、AI、机器人、Doris"));
        assertTrue(instructions.contains("只问第 1 题：用户的名字，以及希望你怎么称呼用户"));
        assertTrue(instructions.contains("基础指令"));
        assertTrue(instructions.contains("初始化基础提示"));
    }

    @Test
    public void initializationWithAgentNameKeepsPresetIdentity() {
        String instructions = ConversationInstructionsComposer.initialization(
                "Clara", "Mira, Nora", 0, "基础指令", "初始化基础提示");

        assertTrue(instructions.startsWith("你叫 Clara。"));
        assertTrue(instructions.contains("介绍自己是 Clara，然后只问第 1 题"));
        assertFalse(instructions.startsWith("你还没有固定名字。"));
    }

    @Test
    public void initializationGuidesSecondAndThirdQuestionsInOrder() {
        assertTrue(ConversationInstructionsComposer.initializationGuide("Clara", 1)
                .contains("问第 2 题：用户希望和你建立什么关系"));
        assertTrue(ConversationInstructionsComposer.initializationGuide("Clara", 2)
                .contains("问第 3 题：请用户开放地讲讲自己的故事"));
        assertTrue(ConversationInstructionsComposer.initializationGuide("Clara", 3)
                .contains("不要继续聊天或提问，只说明你正在写入 user.md"));
    }

    @Test
    public void toolPromptBlocksContainWeatherThenNewsRules() {
        String blocks = ConversationInstructionsComposer.toolPromptBlocks(
                "【天气查询结果】上海晴。", true,
                "【每日新闻热点结果】1. A", false);

        assertTrue(blocks.indexOf("天气查询规则") < blocks.indexOf("【最高优先级：新闻/热点查询规则】"));
        assertTrue(blocks.contains("【天气查询结果】上海晴。"));
        assertTrue(blocks.contains("下一次发言请优先回答这个天气问题。"));
        assertTrue(blocks.contains("只有当你收到“【系统事件】每日新闻热点读取完成”时"));
    }

    @Test
    public void normalPromptPreservesIdentityAndInjectsToolRules() {
        String prompt = ConversationInstructionsComposer.normal(
                "Luna",
                "史蒂芬",
                "# user.md\n- 姓名/称呼：史蒂芬\n- 用户希望和 Agent 的关系：女朋友\n",
                "# Agent.md\n- 与用户的关系定位：用户希望你作为史蒂芬的女朋友。\n",
                "温柔自然",
                "长期摘要",
                "user: 查一下天气\n",
                "【天气查询结果】上海晴。",
                true,
                "",
                false,
                "基础指令",
                1600);

        assertTrue(prompt.contains("基础指令"));
        assertTrue(prompt.contains("天气查询规则"));
        assertTrue(prompt.contains("新闻/热点查询规则"));
        assertTrue(prompt.contains("【不可裁剪的身份和关系设定】"));
        assertTrue(prompt.contains("- Agent 名字：Luna"));
        assertTrue(prompt.contains("- 用户称呼：史蒂芬"));
        assertTrue(prompt.contains("- 用户希望和 Agent 的关系：女朋友"));
        assertTrue(prompt.contains("下一次发言请优先回答这个天气问题。"));
    }
}
