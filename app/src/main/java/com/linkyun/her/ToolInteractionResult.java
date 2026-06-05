package com.linkyun.her;

final class ToolInteractionResult<T> {
    final String tool;
    final String question;
    final boolean success;
    final String fact;
    final String answer;
    final String errorMessage;
    final T payload;

    private ToolInteractionResult(String tool, String question, boolean success,
            String fact, String answer, String errorMessage, T payload) {
        this.tool = tool;
        this.question = question;
        this.success = success;
        this.fact = fact;
        this.answer = answer;
        this.errorMessage = errorMessage;
        this.payload = payload;
    }

    static <T> ToolInteractionResult<T> success(String tool, String question,
            String fact, String answer, T payload) {
        return new ToolInteractionResult<>(tool, question, true, fact, answer, "", payload);
    }

    static <T> ToolInteractionResult<T> failure(String tool, String question,
            String fact, String answer, String errorMessage) {
        return new ToolInteractionResult<>(tool, question, false, fact, answer,
                errorMessage == null ? "" : errorMessage, null);
    }
}
