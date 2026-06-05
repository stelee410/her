package com.linkyun.her;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class PromptMemoryComposerTest {
    @Test
    public void preservesIdentityBlockWhenPromptIsTrimmed() {
        String longMemory = repeat("很长的长期记忆内容。", 300);

        String prompt = PromptMemoryComposer.compose(
                "Luna",
                "史蒂芬",
                "# user.md\n- 姓名/称呼：史蒂芬\n- 用户希望和 Agent 的关系：女朋友\n" + longMemory,
                "# Agent.md\n- 与用户的关系定位：用户希望你作为史蒂芬的女朋友。\n" + longMemory,
                "温柔自然",
                longMemory,
                longMemory,
                "天气工具规则\n新闻工具规则\n",
                "基础指令",
                1200);

        assertTrue(prompt.length() <= 1200);
        assertTrue(prompt.contains("【不可裁剪的身份和关系设定】"));
        assertTrue(prompt.contains("- Agent 名字：Luna"));
        assertTrue(prompt.contains("- 用户称呼：史蒂芬"));
        assertTrue(prompt.contains("- 用户希望和 Agent 的关系：女朋友"));
        assertTrue(prompt.contains("不要说用户没告诉过名字"));
    }

    @Test
    public void trimPreservingSuffixKeepsSuffixAtEnd() {
        String suffix = "关键身份：史蒂芬";
        String prompt = PromptMemoryComposer.trimPreservingSuffix(
                repeat("前文", 100) + suffix,
                suffix,
                30);

        assertTrue(prompt.endsWith(suffix));
        assertTrue(prompt.length() <= 30);
        assertFalse(prompt.contains(repeat("前文", 20)));
    }

    private static String repeat(String value, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }
}
