package com.linkyun.her;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class InitializationCompletionDisplayTest {
    @Test
    public void completedStateContainsFinalInitializationUiValues() {
        InitializationCompletionDisplay.State state = InitializationCompletionDisplay.completed();

        assertEquals("初始化完成", state.progressText);
        assertEquals("我记住啦。我们从这里重新开始。", state.lastTurnText);
        assertEquals(100, state.audioLevel);
        assertEquals(5000, state.homeDelayMs);
    }
}
