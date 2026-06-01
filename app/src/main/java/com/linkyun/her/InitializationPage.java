package com.linkyun.her;

import android.graphics.Color;
import android.text.InputType;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

final class InitializationPage {
    static Views renderSetup(MainActivity activity, HerUi ui, String agentName, Runnable settings, StartCallback startCallback) {
        HerUi.Root rootState = ui.baseRoot(0);
        FrameLayout root = rootState.frame;
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        content.setPadding(ui.dp(34), 0, ui.dp(34), 0);
        root.addView(content, ui.frame(-1, -1));

        InitOrbView mark = new InitOrbView(activity, () -> false);
        content.addView(mark, new LinearLayout.LayoutParams(ui.dp(130), ui.dp(130)));

        TextView title = ui.text("Create your Agent", 28, Color.WHITE, 0);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = ui.dp(34);
        content.addView(title, titleParams);

        TextView subtitle = ui.text("先给她一个名字。接下来她会主动介绍自己，然后了解你的称呼、你希望彼此是什么关系，以及你的生活习惯。", 15, 0xB8FFE0E0, 0);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setLineSpacing(ui.dp(3), 1.0f);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.topMargin = ui.dp(14);
        content.addView(subtitle, subtitleParams);

        EditText nameInput = new EditText(activity);
        nameInput.setText(agentName);
        nameInput.setHint("Agent name");
        nameInput.setHintTextColor(0x80FFE0E0);
        nameInput.setTextColor(Color.WHITE);
        nameInput.setTextSize(21);
        nameInput.setSingleLine(true);
        nameInput.setGravity(Gravity.CENTER);
        nameInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        nameInput.setBackgroundColor(0x22FFFFFF);
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(-1, ui.dp(58));
        inputParams.topMargin = ui.dp(28);
        content.addView(nameInput, inputParams);

        TextView start = ui.text("Initialize", 19, Color.WHITE, 600);
        start.setGravity(Gravity.CENTER);
        start.setBackground(ui.bubbleDrawable(true));
        start.setOnClickListener(v -> startCallback.onStart(nameInput.getText().toString().trim()));
        LinearLayout.LayoutParams startParams = new LinearLayout.LayoutParams(-1, ui.dp(58));
        startParams.topMargin = ui.dp(18);
        content.addView(start, startParams);
        return new Views(root, rootState.moodVeil, null, null, null, null, null);
    }

    static Views renderHome(MainActivity activity, HerUi ui, Model model, Callbacks callbacks) {
        HerUi.Root rootState = ui.baseRoot(model.mood);
        FrameLayout root = rootState.frame;
        root.addView(ui.topBar("☰", "", "", callbacks::onSettings, null));

        LinearLayout center = new LinearLayout(activity);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER);
        center.setPadding(ui.dp(28), 0, ui.dp(28), ui.dp(96));
        root.addView(center, ui.frame(-1, -1));

        InitOrbView mark = new InitOrbView(activity, callbacks::isSummarizing);
        center.addView(mark, new LinearLayout.LayoutParams(ui.dp(154), ui.dp(154)));

        TextView initProgressView = ui.text(model.progressText, 14, 0xCCFFE0E0, 0);
        initProgressView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(-1, -2);
        progressParams.topMargin = ui.dp(30);
        center.addView(initProgressView, progressParams);

        TextView initLastTurnView = ui.text(model.lastLine, 21, Color.WHITE, 0);
        initLastTurnView.setGravity(Gravity.CENTER);
        initLastTurnView.setLineSpacing(ui.dp(4), 1.0f);
        initLastTurnView.setPadding(ui.dp(14), ui.dp(12), ui.dp(14), ui.dp(12));
        LinearLayout.LayoutParams lastParams = new LinearLayout.LayoutParams(-1, -2);
        lastParams.topMargin = ui.dp(22);
        center.addView(initLastTurnView, lastParams);

        TextView stateLabel = ui.text(model.stateLabel, 12, 0x99FFE0E0, 0);
        stateLabel.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams stateParams = new LinearLayout.LayoutParams(-1, -2);
        stateParams.topMargin = ui.dp(8);
        center.addView(stateLabel, stateParams);

        AudioLevelView audioLevelView = new AudioLevelView(activity);
        FrameLayout.LayoutParams levelParams = ui.frame(ui.dp(190), ui.dp(12), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        levelParams.bottomMargin = ui.dp(100);
        root.addView(audioLevelView, levelParams);

        TextView micButton = ui.text("♩", 34, 0xFFFF6377, 0);
        micButton.setGravity(Gravity.CENTER);
        micButton.setOnClickListener(v -> callbacks.onToggleMic());
        FrameLayout.LayoutParams micParams = ui.frame(ui.dp(78), ui.dp(70), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        micParams.bottomMargin = ui.dp(22);
        root.addView(micButton, micParams);

        return new Views(root, rootState.moodVeil, initProgressView, initLastTurnView, stateLabel, audioLevelView, micButton);
    }

    interface StartCallback {
        void onStart(String name);
    }

    interface Callbacks {
        void onSettings();
        void onToggleMic();
        boolean isSummarizing();
    }

    static final class Model {
        final String progressText;
        final String lastLine;
        final String stateLabel;
        final int mood;

        Model(String progressText, String lastLine, String stateLabel, int mood) {
            this.progressText = progressText;
            this.lastLine = lastLine;
            this.stateLabel = stateLabel;
            this.mood = mood;
        }
    }

    static final class Views {
        final FrameLayout root;
        final MoodVeil moodVeil;
        final TextView initProgressView;
        final TextView initLastTurnView;
        final TextView stateLabel;
        final AudioLevelView audioLevelView;
        final TextView micButton;

        Views(FrameLayout root, MoodVeil moodVeil, TextView initProgressView, TextView initLastTurnView,
                TextView stateLabel, AudioLevelView audioLevelView, TextView micButton) {
            this.root = root;
            this.moodVeil = moodVeil;
            this.initProgressView = initProgressView;
            this.initLastTurnView = initLastTurnView;
            this.stateLabel = stateLabel;
            this.audioLevelView = audioLevelView;
            this.micButton = micButton;
        }
    }
}
