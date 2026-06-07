package com.linkyun.her;

import android.graphics.Color;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

final class ChatPage {
    static Views render(MainActivity activity, HerUi ui, Model model, Callbacks callbacks) {
        HerUi.Root rootState = ui.baseRoot(model.mood);
        FrameLayout root = rootState.frame;
        LinearLayout top = ui.topBar("‹", model.agentName, "", callbacks::onBack, null);
        root.addView(top);

        TextView stateLabel = ui.text(model.stateLabel, 11, 0x99FFE0E0, 0);
        stateLabel.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams stateParams = ui.frame(-1, ui.dp(22), Gravity.TOP);
        stateParams.topMargin = ui.dp(54);
        root.addView(stateLabel, stateParams);

        TextView initProgressView = null;
        if (model.initializing) {
            initProgressView = ui.text(model.progressText, 13, 0xCCFFE0E0, 0);
            initProgressView.setGravity(Gravity.CENTER);
            FrameLayout.LayoutParams initParams = ui.frame(-1, ui.dp(26), Gravity.TOP);
            initParams.topMargin = ui.dp(76);
            root.addView(initProgressView, initParams);
        }

        ScrollView messageScroll = new ScrollView(activity);
        messageScroll.setFillViewport(true);
        messageScroll.setPadding(0, model.initializing ? ui.dp(108) : ui.dp(80), 0, ui.dp(98));
        LinearLayout messageList = new LinearLayout(activity);
        messageList.setOrientation(LinearLayout.VERTICAL);
        messageList.setPadding(ui.dp(26), ui.dp(10), ui.dp(26), ui.dp(28));
        messageScroll.addView(messageList, new ScrollView.LayoutParams(-1, -2));
        root.addView(messageScroll, ui.frame(-1, -1));
        renderMessages(activity, ui, messageList, messageScroll, model.messages, model.replyPlaceholderText);

        TextView voiceHoldHint = ui.text(TextModeAsrGesture.label(TextModeAsrGesture.NEUTRAL), 17, 0xCCFFFFFF, 0);
        voiceHoldHint.setGravity(Gravity.CENTER);
        voiceHoldHint.setVisibility(model.asrListening ? View.VISIBLE : View.GONE);
        FrameLayout.LayoutParams hintParams = ui.frame(-1, ui.dp(42), Gravity.BOTTOM);
        hintParams.bottomMargin = ui.dp(92);
        root.addView(voiceHoldHint, hintParams);

        LinearLayout input = new LinearLayout(activity);
        input.setGravity(Gravity.CENTER_VERTICAL);
        input.setPadding(ui.dp(22), ui.dp(12), ui.dp(18), ui.dp(12));
        input.setBackgroundColor(0x88523B48);

        EditText composer = new EditText(activity);
        composer.setHint("Type a message...");
        composer.setHintTextColor(0x80FFE0E0);
        composer.setTextColor(Color.WHITE);
        composer.setTextSize(18);
        composer.setSingleLine(true);
        composer.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        composer.setBackgroundColor(Color.TRANSPARENT);
        input.addView(composer, new LinearLayout.LayoutParams(0, ui.dp(56), 1));

        ImageButton asr = new ImageButton(activity);
        asr.setImageResource(model.asrListening ? R.drawable.ic_stop_text_input : R.drawable.ic_mic_text_input);
        asr.setColorFilter(0xDDFFFFFF);
        asr.setBackgroundColor(Color.TRANSPARENT);
        asr.setPadding(ui.dp(12), ui.dp(12), ui.dp(12), ui.dp(12));
        asr.setContentDescription("按住说话");
        asr.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                callbacks.onAsrPressStart(event.getRawX());
                return true;
            case MotionEvent.ACTION_MOVE:
                callbacks.onAsrPressMove(event.getRawX());
                return true;
            case MotionEvent.ACTION_UP:
                v.performClick();
                callbacks.onAsrPressEnd(event.getRawX());
                return true;
            case MotionEvent.ACTION_CANCEL:
                callbacks.onAsrPressCancel();
                return true;
            default:
                return true;
            }
        });
        input.addView(asr, new LinearLayout.LayoutParams(ui.dp(48), ui.dp(56)));

        TextView send = ui.text("➤", 26, 0xFFFF6377, 0);
        send.setGravity(Gravity.CENTER);
        send.setOnClickListener(v -> {
            String value = composer.getText().toString().trim();
            composer.setText("");
            callbacks.onSend(value);
        });
        input.addView(send, new LinearLayout.LayoutParams(ui.dp(50), ui.dp(56)));

        root.addView(input, ui.frame(-1, ui.dp(92), Gravity.BOTTOM));
        top.bringToFront();
        return new Views(root, rootState.moodVeil, messageList, messageScroll, composer, stateLabel, initProgressView, asr, voiceHoldHint);
    }

    static void renderMessages(MainActivity activity, HerUi ui, LinearLayout messageList, ScrollView messageScroll, List<Message> messages) {
        renderMessages(activity, ui, messageList, messageScroll, messages, "");
    }

    static void renderMessages(MainActivity activity, HerUi ui, LinearLayout messageList,
            ScrollView messageScroll, List<Message> messages, String replyPlaceholderText) {
        if (messageList == null) return;
        messageList.removeAllViews();
        for (Message message : messages) {
            View row;
            if (message.weather != null) {
                row = WeatherCard.chat(activity, ui, message);
            } else if (message.news != null) {
                row = NewsCard.chat(activity, ui, message);
            } else {
                row = bubble(activity, ui, message);
            }
            messageList.addView(row);
            Space gap = new Space(activity);
            messageList.addView(gap, new LinearLayout.LayoutParams(1, ui.dp(18)));
        }
        if (replyPlaceholderText != null && !replyPlaceholderText.isEmpty()) {
            messageList.addView(replyPlaceholder(activity, ui, replyPlaceholderText));
            Space gap = new Space(activity);
            messageList.addView(gap, new LinearLayout.LayoutParams(1, ui.dp(18)));
        }
        messageScroll.postDelayed(() -> messageScroll.fullScroll(View.FOCUS_DOWN), 80);
    }

    private static View replyPlaceholder(MainActivity activity, HerUi ui, String text) {
        LinearLayout wrap = new LinearLayout(activity);
        wrap.setGravity(Gravity.LEFT);
        LinearLayout column = new LinearLayout(activity);
        column.setOrientation(LinearLayout.VERTICAL);
        TextView body = ui.text(text, 22, Color.WHITE, 0);
        body.setGravity(Gravity.CENTER);
        body.setPadding(ui.dp(18), ui.dp(10), ui.dp(18), ui.dp(12));
        body.setBackground(ui.bubbleDrawable(false));
        column.addView(body, new LinearLayout.LayoutParams(ui.dp(86), ui.dp(52)));
        int width = activity.getResources().getDisplayMetrics().widthPixels - ui.dp(110);
        wrap.addView(column, new LinearLayout.LayoutParams(Math.max(ui.dp(190), width), -2));
        return wrap;
    }

    private static View bubble(MainActivity activity, HerUi ui, Message message) {
        LinearLayout wrap = new LinearLayout(activity);
        wrap.setGravity(message.role.equals("user") ? Gravity.RIGHT : Gravity.LEFT);
        LinearLayout column = new LinearLayout(activity);
        column.setOrientation(LinearLayout.VERTICAL);
        TextView body = ui.text(message.text.isEmpty() ? "..." : message.text, 19, Color.WHITE, 0);
        body.setLineSpacing(ui.dp(2), 1.0f);
        body.setPadding(ui.dp(16), ui.dp(13), ui.dp(16), ui.dp(13));
        body.setBackground(ui.bubbleDrawable(message.role.equals("user")));
        column.addView(body, new LinearLayout.LayoutParams(-2, -2));
        TextView time = ui.text(DateFormat.getTimeInstance(DateFormat.SHORT, Locale.US).format(new Date(message.timestamp)), 12, 0x80FFE0E0, 0);
        time.setPadding(ui.dp(8), ui.dp(6), ui.dp(8), 0);
        time.setGravity(message.role.equals("user") ? Gravity.RIGHT : Gravity.LEFT);
        column.addView(time, new LinearLayout.LayoutParams(-1, -2));
        int width = activity.getResources().getDisplayMetrics().widthPixels - ui.dp(110);
        wrap.addView(column, new LinearLayout.LayoutParams(Math.max(ui.dp(190), width), -2));
        return wrap;
    }

    interface Callbacks {
        void onBack();
        void onAsrPressStart(float rawX);
        void onAsrPressMove(float rawX);
        void onAsrPressEnd(float rawX);
        void onAsrPressCancel();
        void onSend(String text);
    }

    static final class Model {
        final String agentName;
        final String stateLabel;
        final boolean initializing;
        final String progressText;
        final boolean asrListening;
        final String replyPlaceholderText;
        final int mood;
        final List<Message> messages;

        Model(String agentName, String stateLabel, boolean initializing, String progressText,
                boolean asrListening, String replyPlaceholderText, int mood, List<Message> messages) {
            this.agentName = agentName;
            this.stateLabel = stateLabel;
            this.initializing = initializing;
            this.progressText = progressText;
            this.asrListening = asrListening;
            this.replyPlaceholderText = replyPlaceholderText;
            this.mood = mood;
            this.messages = messages;
        }
    }

    static final class Views {
        final FrameLayout root;
        final MoodVeil moodVeil;
        final LinearLayout messageList;
        final ScrollView messageScroll;
        final EditText composer;
        final TextView stateLabel;
        final TextView initProgressView;
        final ImageButton asrButton;
        final TextView asrHint;

        Views(FrameLayout root, MoodVeil moodVeil, LinearLayout messageList, ScrollView messageScroll,
                EditText composer, TextView stateLabel, TextView initProgressView, ImageButton asrButton, TextView asrHint) {
            this.root = root;
            this.moodVeil = moodVeil;
            this.messageList = messageList;
            this.messageScroll = messageScroll;
            this.composer = composer;
            this.stateLabel = stateLabel;
            this.initProgressView = initProgressView;
            this.asrButton = asrButton;
            this.asrHint = asrHint;
        }
    }
}
