package com.linkyun.her;

final class NewsInteractionHandler {
    interface Source {
        void fetchDaily(NewsTool.CallbackResult callback);
    }

    interface Callback {
        void onResult(ToolInteractionResult<NewsTool.NewsResult> result);
    }

    private static final String TOOL = "news";

    private final Source source;

    NewsInteractionHandler(NewsTool newsTool) {
        this(newsTool == null ? null : newsTool::fetchDaily);
    }

    NewsInteractionHandler(Source source) {
        this.source = source;
    }

    void fetch(String question, Callback callback) {
        String normalizedQuestion = normalizeQuestion(question);
        if (source == null) {
            callback.onResult(failure(normalizedQuestion, "新闻工具不可用"));
            return;
        }
        try {
            source.fetchDaily(new NewsTool.CallbackResult() {
                @Override public void onSuccess(NewsTool.NewsResult result) {
                    if (result == null) {
                        callback.onResult(failure(normalizedQuestion, "新闻结果为空"));
                        return;
                    }
                    ToolInteractionResult<NewsTool.NewsResult> toolResult;
                    try {
                        toolResult = ToolInteractionResult.success(
                                TOOL,
                                normalizedQuestion,
                                result.fact(normalizedQuestion),
                                result.shortAnswer(),
                                result);
                    } catch (RuntimeException error) {
                        toolResult = failure(normalizedQuestion, "新闻结果异常：" + exceptionMessage(error));
                    }
                    callback.onResult(toolResult);
                }

                @Override public void onError(String message) {
                    callback.onResult(failure(normalizedQuestion, message));
                }
            });
        } catch (RuntimeException error) {
            callback.onResult(failure(normalizedQuestion, exceptionMessage(error)));
        }
    }

    private static ToolInteractionResult<NewsTool.NewsResult> failure(String question, String message) {
        String error = NewsSkill.failureMessage(message);
        return ToolInteractionResult.failure(
                TOOL,
                question,
                NewsSkill.failureFact(error),
                "暂时没读到新闻热点，" + error + "。你可以稍后再试一下。",
                error);
    }

    private static String normalizeQuestion(String question) {
        if (question == null || question.trim().isEmpty()) return "每日新闻热点";
        return question.trim();
    }

    private static String exceptionMessage(RuntimeException error) {
        String message = error == null ? "" : error.getMessage();
        return message == null || message.trim().isEmpty() ? "工具异常" : message.trim();
    }
}
