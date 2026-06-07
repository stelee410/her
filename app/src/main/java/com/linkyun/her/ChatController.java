package com.linkyun.her;

import org.json.JSONException;

final class ChatController {
    interface Sender {
        void sendChat(String model, String instructions, String text, AgentApiClient.ReplyCallback callback)
                throws JSONException;
    }

    interface Host {
        boolean hasApiKey();
        String buildTextChatInstructions() throws JSONException;
        void setState(String nextState);
        void toastError(String message);
        void showReplyPlaceholder();
        void hideReplyPlaceholder();
        void breatheScreenForAssistantReply();
        void addAssistantMessage(String text);
        void renderMessages();
        boolean isInitializing();
        void updateInitProgress();
    }

    private final String model;
    private final Sender sender;
    private final Host host;
    private int requestSeq;

    ChatController(String model, Sender sender, Host host) {
        this.model = model;
        this.sender = sender;
        this.host = host;
    }

    void sendText(String text) {
        int requestId = ++requestSeq;
        if (!host.hasApiKey()) {
            host.toastError("Missing AGENTLLM_API_KEY in local.properties");
            host.setState("error");
            return;
        }
        host.setState("processing");
        host.showReplyPlaceholder();
        String instructions;
        try {
            instructions = host.buildTextChatInstructions();
            sender.sendChat(model, instructions, text, new AgentApiClient.ReplyCallback() {
                @Override public void onSuccess(String reply) {
                    if (!isCurrent(requestId)) return;
                    handleSuccess(reply);
                }

                @Override public void onError(String message) {
                    if (!isCurrent(requestId)) return;
                    host.hideReplyPlaceholder();
                    host.toastError(message);
                    host.setState("error");
                }
            });
        } catch (JSONException error) {
            host.hideReplyPlaceholder();
            host.toastError("构建文本聊天请求失败");
            host.setState("error");
        }
    }

    private boolean isCurrent(int requestId) {
        return requestId == requestSeq;
    }

    private void handleSuccess(String reply) {
        if (reply == null || reply.isEmpty()) {
            host.hideReplyPlaceholder();
            host.toastError("文本聊天返回为空");
            host.setState("error");
            return;
        }
        host.hideReplyPlaceholder();
        host.breatheScreenForAssistantReply();
        host.addAssistantMessage(reply);
        host.renderMessages();
        if (host.isInitializing()) {
            host.updateInitProgress();
        }
        host.setState("ready");
    }
}
