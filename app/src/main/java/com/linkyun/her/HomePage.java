package com.linkyun.her;

import android.graphics.Color;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

final class HomePage {
    static Views render(MainActivity activity, HerUi ui, Model model, Callbacks callbacks) {
        HerUi.Root rootState = ui.baseRoot(model.mood);
        FrameLayout root = rootState.frame;
        LinearLayout top = ui.topBar("☰", "", "Aa", callbacks::onSettings, callbacks::onChat);
        root.addView(top);

        LinearLayout center = new LinearLayout(activity);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER);
        center.setPadding(ui.dp(30), 0, ui.dp(30), ui.dp(86));
        root.addView(center, ui.frame(-1, -1));

        VoiceOrbView voiceOrbView = new VoiceOrbView(activity);
        voiceOrbView.setOnClickListener(v -> callbacks.onToggleMic());
        center.addView(voiceOrbView, new LinearLayout.LayoutParams(ui.dp(178), ui.dp(178)));

        TextView voiceLastTurnView = ui.text(model.lastLine, 22, Color.WHITE, 0);
        voiceLastTurnView.setGravity(Gravity.CENTER);
        voiceLastTurnView.setLineSpacing(ui.dp(4), 1.0f);
        voiceLastTurnView.setPadding(ui.dp(8), ui.dp(34), ui.dp(8), 0);
        LinearLayout.LayoutParams lastParams = new LinearLayout.LayoutParams(-1, -2);
        lastParams.topMargin = ui.dp(6);
        center.addView(voiceLastTurnView, lastParams);

        TextView stateLabel = ui.text(model.stateLabel, 12, 0xA8FFE0E0, 0);
        stateLabel.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams stateParams = new LinearLayout.LayoutParams(-1, -2);
        stateParams.topMargin = ui.dp(16);
        center.addView(stateLabel, stateParams);

        AudioLevelView audioLevelView = new AudioLevelView(activity);
        FrameLayout.LayoutParams levelParams = ui.frame(ui.dp(190), ui.dp(12), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        levelParams.bottomMargin = ui.dp(94);
        root.addView(audioLevelView, levelParams);

        TextView micButton = ui.text("♩", 36, 0xFFFF6377, 0);
        micButton.setGravity(Gravity.CENTER);
        micButton.setOnClickListener(v -> callbacks.onToggleMic());
        FrameLayout.LayoutParams micParams = ui.frame(ui.dp(78), ui.dp(70), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        micParams.bottomMargin = ui.dp(18);
        root.addView(micButton, micParams);

        top.bringToFront();
        return new Views(root, rootState.moodVeil, voiceOrbView, voiceLastTurnView, stateLabel, audioLevelView, micButton);
    }

    interface Callbacks {
        void onSettings();
        void onChat();
        void onToggleMic();
    }

    static final class Model {
        final String lastLine;
        final String stateLabel;
        final int mood;

        Model(String lastLine, String stateLabel, int mood) {
            this.lastLine = lastLine;
            this.stateLabel = stateLabel;
            this.mood = mood;
        }
    }

    static final class Views {
        final FrameLayout root;
        final MoodVeil moodVeil;
        final VoiceOrbView voiceOrbView;
        final TextView voiceLastTurnView;
        final TextView stateLabel;
        final AudioLevelView audioLevelView;
        final TextView micButton;

        Views(FrameLayout root, MoodVeil moodVeil, VoiceOrbView voiceOrbView, TextView voiceLastTurnView,
                TextView stateLabel, AudioLevelView audioLevelView, TextView micButton) {
            this.root = root;
            this.moodVeil = moodVeil;
            this.voiceOrbView = voiceOrbView;
            this.voiceLastTurnView = voiceLastTurnView;
            this.stateLabel = stateLabel;
            this.audioLevelView = audioLevelView;
            this.micButton = micButton;
        }
    }
}
