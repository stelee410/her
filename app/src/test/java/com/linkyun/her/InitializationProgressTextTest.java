package com.linkyun.her;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class InitializationProgressTextTest {
    @Test
    public void summaryTextOverridesStepProgress() {
        assertEquals("潜意识模型正在写入 user.md / Agent.md",
                InitializationProgressText.build(true, 0, 3));
    }

    @Test
    public void buildsThreeInitializationSteps() {
        assertEquals("初始化 1/3 · 名字和称呼",
                InitializationProgressText.build(false, 0, 3));
        assertEquals("初始化 2/3 · 你希望的关系",
                InitializationProgressText.build(false, 1, 3));
        assertEquals("初始化 3/3 · 你的故事",
                InitializationProgressText.build(false, 2, 3));
    }

    @Test
    public void clampsNegativeAndOverflowTurns() {
        assertEquals("初始化 1/3 · 名字和称呼",
                InitializationProgressText.build(false, -2, 3));
        assertEquals("初始化 3/3 · 你的故事",
                InitializationProgressText.build(false, 99, 3));
    }

    @Test
    public void protectsInvalidTargetTurnCount() {
        assertEquals("初始化 1/1 · 名字和称呼",
                InitializationProgressText.build(false, 0, 0));
    }
}
