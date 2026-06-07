package com.linkyun.her;

import java.util.List;

final class ConversationHistory {
    private ConversationHistory() {
    }

    static String lastAssistantBeforeLatestUser(List<Message> messages) {
        if (messages == null || messages.isEmpty()) return "";
        boolean seenLatestUser = false;
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            if (message == null) continue;
            if (!seenLatestUser) {
                if ("user".equals(message.role)) seenLatestUser = true;
                continue;
            }
            if ("assistant".equals(message.role) && message.text != null) {
                return message.text.trim();
            }
        }
        return "";
    }

    static String recentDialogue(List<Message> messages, int limit) {
        if (messages == null || messages.isEmpty() || limit <= 0) return "";
        int start = Math.max(0, messages.size() - limit);
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < messages.size(); i++) {
            Message message = messages.get(i);
            if (message == null) continue;
            if (!"user".equals(message.role) && !"assistant".equals(message.role)) continue;
            if (message.text == null || message.text.trim().isEmpty()) continue;
            builder.append(message.role).append(": ").append(message.text.trim()).append('\n');
        }
        return builder.toString();
    }

    static String initializationTranscript(List<Message> messages) {
        if (messages == null || messages.isEmpty()) return "";
        StringBuilder transcript = new StringBuilder();
        for (Message message : messages) {
            if (message == null) continue;
            transcript.append(message.role).append(": ").append(message.text).append('\n');
        }
        return transcript.toString();
    }

    static String lastConversationLine(List<Message> messages, String fallback) {
        String line = lastLine(messages, true);
        return line.isEmpty() ? safeFallback(fallback) : line;
    }

    static String lastAnyLine(List<Message> messages, String fallback) {
        String line = lastLine(messages, false);
        return line.isEmpty() ? safeFallback(fallback) : line;
    }

    private static String lastLine(List<Message> messages, boolean userOrAssistantOnly) {
        if (messages == null || messages.isEmpty()) return "";
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            if (message == null) continue;
            if (userOrAssistantOnly && !"user".equals(message.role) && !"assistant".equals(message.role)) {
                continue;
            }
            if (message.text != null && !message.text.trim().isEmpty()) {
                return message.text.trim();
            }
        }
        return "";
    }

    private static String safeFallback(String fallback) {
        return fallback == null ? "" : fallback;
    }
}
