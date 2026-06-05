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
        source.fetchDaily(new NewsTool.CallbackResult() {
            @Override public void onSuccess(NewsTool.NewsResult result) {
                callback.onResult(ToolInteractionResult.success(
                        TOOL,
                        normalizedQuestion,
                        result.fact(normalizedQuestion),
                        result.shortAnswer(),
                        result));
            }

            @Override public void onError(String message) {
                callback.onResult(failure(normalizedQuestion, message));
            }
        });
    }

    private static ToolInteractionResult<NewsTool.NewsResult> failure(String question, String message) {
        String error = message == null ? "" : message;
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
}
