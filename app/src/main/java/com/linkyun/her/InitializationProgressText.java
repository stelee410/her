package com.linkyun.her;

final class InitializationProgressText {
    private InitializationProgressText() {
    }

    static String build(boolean summaryInProgress, int userTurns, int targetTurns) {
        if (summaryInProgress) return "潜意识模型正在写入 user.md / Agent.md";
        int target = Math.max(1, targetTurns);
        int step = Math.min(Math.max(0, userTurns) + 1, target);
        return "初始化 " + step + "/" + target + " · " + labelForStep(step);
    }

    private static String labelForStep(int step) {
        if (step == 1) return "名字和称呼";
        if (step == 2) return "你希望的关系";
        return "你的故事";
    }
}
