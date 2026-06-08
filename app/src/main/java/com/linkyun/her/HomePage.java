package com.linkyun.her;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

final class HomePage {
    static Views renderLanding(MainActivity activity, HerUi ui, LandingModel model, Callbacks callbacks) {
        HerUi.Root rootState = ui.baseRoot(model.mood);
        FrameLayout root = rootState.frame;
        LinearLayout top = ui.topBar("☰", "", "Aa", callbacks::onSettings, callbacks::onChat);
        root.addView(top);

        LinearLayout center = new LinearLayout(activity);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER);
        center.setPadding(ui.dp(30), 0, ui.dp(30), ui.dp(74));
        root.addView(center, ui.frame(-1, -1));

        HerMarkView mark = new HerMarkView(activity);
        mark.setOnClickListener(v -> callbacks.onVoiceHome());
        center.addView(mark, new LinearLayout.LayoutParams(ui.dp(174), ui.dp(174)));

        TextView greeting = ui.text("Hello, " + model.userName + "\nI'm", 32, Color.WHITE, 700);
        greeting.setGravity(Gravity.CENTER);
        greeting.setLineSpacing(ui.dp(8), 1.0f);
        LinearLayout.LayoutParams greetingParams = new LinearLayout.LayoutParams(-1, -2);
        greetingParams.topMargin = ui.dp(38);
        center.addView(greeting, greetingParams);

        TextView handwrittenNameView = ui.text("", 42, Color.WHITE, 0);
        handwrittenNameView.setGravity(Gravity.CENTER);
        handwrittenNameView.setIncludeFontPadding(false);
        handwrittenNameView.setTypeface(Typeface.create("casual", Typeface.NORMAL));
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(-1, -2);
        nameParams.topMargin = ui.dp(16);
        center.addView(handwrittenNameView, nameParams);

        TextView homeTimeView = ui.text("", 42, Color.WHITE, 0);
        homeTimeView.setGravity(Gravity.CENTER);
        homeTimeView.setVisibility(TextView.GONE);

        TextView voiceEntry = ui.text("^", 32, Color.WHITE, 700);
        voiceEntry.setGravity(Gravity.CENTER);
        voiceEntry.setOnClickListener(v -> callbacks.onVoiceHome());
        FrameLayout.LayoutParams entryParams = ui.frame(ui.dp(78), ui.dp(70), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        entryParams.bottomMargin = ui.dp(24);
        root.addView(voiceEntry, entryParams);

        top.bringToFront();
        return new Views(root, rootState.moodVeil, null, null, null, null, null, null, homeTimeView, handwrittenNameView);
    }

    static Views renderVoice(MainActivity activity, HerUi ui, VoiceModel model, Callbacks callbacks) {
        HerUi.Root rootState = ui.baseRoot(model.mood);
        FrameLayout root = rootState.frame;
        LinearLayout top = ui.topBar("☰", "", "Aa", callbacks::onSettings, callbacks::onChat);
        root.addView(top);

        LinearLayout center = new LinearLayout(activity);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(model.digitalAvatarEnabled ? Gravity.TOP | Gravity.CENTER_HORIZONTAL : Gravity.CENTER);
        center.setPadding(ui.dp(30), model.digitalAvatarEnabled ? ui.dp(108) : 0, ui.dp(30),
                model.digitalAvatarEnabled ? 0 : ui.dp(86));
        root.addView(center, ui.frame(-1, -1));

        VoiceOrbView voiceOrbView = null;
        DigitalAvatarView digitalAvatarView = null;
        if (model.digitalAvatarEnabled) {
            digitalAvatarView = new DigitalAvatarView(activity);
            digitalAvatarView.setAvatarState(model.avatarEmotion, model.avatarSpeaking);
            FrameLayout.LayoutParams avatarParams = ui.frame(ui.dp(600), ui.dp(800), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            root.addView(digitalAvatarView, avatarParams);
        } else {
            voiceOrbView = new VoiceOrbView(activity);
            voiceOrbView.setOnClickListener(v -> callbacks.onToggleMic());
            center.addView(voiceOrbView, new LinearLayout.LayoutParams(ui.dp(178), ui.dp(178)));
        }

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

        AudioLevelView audioLevelView = null;
        TextView micButton = null;
        if (!model.digitalAvatarEnabled) {
            audioLevelView = new AudioLevelView(activity);
            FrameLayout.LayoutParams levelParams = ui.frame(ui.dp(190), ui.dp(12), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            levelParams.bottomMargin = ui.dp(94);
            root.addView(audioLevelView, levelParams);

            micButton = ui.text("♩", 36, 0xFFFF6377, 0);
            micButton.setGravity(Gravity.CENTER);
            micButton.setOnClickListener(v -> callbacks.onToggleMic());
            FrameLayout.LayoutParams micParams = ui.frame(ui.dp(78), ui.dp(70), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            micParams.bottomMargin = ui.dp(18);
            root.addView(micButton, micParams);
        }

        if (model.weather != null) {
            View weatherCard = WeatherCard.voice(activity, ui, model.weather);
            FrameLayout.LayoutParams weatherParams = ui.frame(-1, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            weatherParams.leftMargin = ui.dp(18);
            weatherParams.rightMargin = ui.dp(18);
            weatherParams.topMargin = ui.dp(84);
            root.addView(weatherCard, weatherParams);
        } else if (model.news != null) {
            View newsCard = NewsCard.voice(activity, ui, model.news);
            FrameLayout.LayoutParams newsParams = ui.frame(-1, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            newsParams.leftMargin = ui.dp(18);
            newsParams.rightMargin = ui.dp(18);
            newsParams.topMargin = ui.dp(84);
            root.addView(newsCard, newsParams);
        }

        if (model.digitalAvatarEnabled) center.bringToFront();
        if (audioLevelView != null) audioLevelView.bringToFront();
        if (micButton != null) micButton.bringToFront();
        top.bringToFront();
        return new Views(root, rootState.moodVeil, voiceOrbView, digitalAvatarView, voiceLastTurnView, stateLabel, audioLevelView, micButton, null, null);
    }

    interface Callbacks {
        void onSettings();
        void onChat();
        void onVoiceHome();
        void onToggleMic();
    }

    static final class LandingModel {
        final String userName;
        final String agentName;
        final int mood;

        LandingModel(String userName, String agentName, int mood) {
            this.userName = userName;
            this.agentName = agentName;
            this.mood = mood;
        }
    }

    static final class VoiceModel {
        final String lastLine;
        final String stateLabel;
        final int mood;
        final WeatherTool.WeatherResult weather;
        final NewsTool.NewsResult news;
        final boolean digitalAvatarEnabled;
        final boolean avatarSpeaking;
        final String avatarEmotion;

        VoiceModel(String lastLine, String stateLabel, int mood, WeatherTool.WeatherResult weather, NewsTool.NewsResult news,
                boolean digitalAvatarEnabled, boolean avatarSpeaking, String avatarEmotion) {
            this.lastLine = lastLine;
            this.stateLabel = stateLabel;
            this.mood = mood;
            this.weather = weather;
            this.news = news;
            this.digitalAvatarEnabled = digitalAvatarEnabled;
            this.avatarSpeaking = avatarSpeaking;
            this.avatarEmotion = avatarEmotion;
        }
    }

    static final class Views {
        final FrameLayout root;
        final MoodVeil moodVeil;
        final VoiceOrbView voiceOrbView;
        final DigitalAvatarView digitalAvatarView;
        final TextView voiceLastTurnView;
        final TextView stateLabel;
        final AudioLevelView audioLevelView;
        final TextView micButton;
        final TextView homeTimeView;
        final TextView handwrittenNameView;

        Views(FrameLayout root, MoodVeil moodVeil, VoiceOrbView voiceOrbView, DigitalAvatarView digitalAvatarView, TextView voiceLastTurnView,
                TextView stateLabel, AudioLevelView audioLevelView, TextView micButton,
                TextView homeTimeView, TextView handwrittenNameView) {
            this.root = root;
            this.moodVeil = moodVeil;
            this.voiceOrbView = voiceOrbView;
            this.digitalAvatarView = digitalAvatarView;
            this.voiceLastTurnView = voiceLastTurnView;
            this.stateLabel = stateLabel;
            this.audioLevelView = audioLevelView;
            this.micButton = micButton;
            this.homeTimeView = homeTimeView;
            this.handwrittenNameView = handwrittenNameView;
        }
    }
}
