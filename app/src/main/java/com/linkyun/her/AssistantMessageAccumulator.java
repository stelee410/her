package com.linkyun.her;

import java.util.List;

final class AssistantMessageAccumulator {
    interface IdFactory {
        String newId(String role);
    }

    private final List<Message> messages;
    private final IdFactory idFactory;
    private String activeAssistantId;

    AssistantMessageAccumulator(List<Message> messages, IdFactory idFactory) {
        this.messages = messages;
        this.idFactory = idFactory;
    }

    boolean appendDelta(String text) {
        if (activeAssistantId == null) {
            activeAssistantId = idFactory.newId("assistant");
            messages.add(new Message(activeAssistantId, "assistant", ""));
        }
        Message active = activeMessage();
        if (active == null) return false;
        active.text += text == null ? "" : text;
        return true;
    }

    Message activeMessage() {
        if (activeAssistantId == null) return null;
        for (Message message : messages) {
            if (message.id.equals(activeAssistantId)) return message;
        }
        return null;
    }

    void clearActive() {
        activeAssistantId = null;
    }

    boolean discardActive() {
        if (activeAssistantId == null) return false;
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i).id.equals(activeAssistantId)) {
                messages.remove(i);
                activeAssistantId = null;
                return true;
            }
        }
        activeAssistantId = null;
        return false;
    }

    boolean removeAssistantReplyAfterLastUser() {
        boolean removed = false;
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            if ("user".equals(message.role)) break;
            if ("assistant".equals(message.role)) {
                messages.remove(i);
                removed = true;
            }
        }
        activeAssistantId = null;
        return removed;
    }
}
