package com.linkyun.her;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

final class HomePage {
    static Views renderLanding(MainActivity activity, HerUi ui, LandingModel model, Callbacks callbacks) {
        if (model.jessAvatarHome) {
            return renderJessLanding(activity, ui, model, callbacks);
        }
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
        return new Views(root, rootState.moodVeil, null, null, null, null, null, null, null, homeTimeView, handwrittenNameView);
    }

    private static Views renderJessLanding(MainActivity activity, HerUi ui, LandingModel model, Callbacks callbacks) {
        HerUi.Root rootState = ui.baseRoot(model.mood);
        FrameLayout root = rootState.frame;

        AssetVideoAvatarView avatarView = new AssetVideoAvatarView(
                activity, TabletDemoCharacterCatalog.hiddenJess(), ui.dp(76));
        avatarView.setSpeaking(model.avatarSpeaking);
        root.addView(avatarView, ui.frame(-1, -1));

        LinearLayout top = ui.topBar("☰", "", "Aa", callbacks::onSettings, callbacks::onChat);
        root.addView(top);
        ImageButton call = phoneButton(activity, ui, callbacks::onVoiceHome);
        FrameLayout.LayoutParams callParams = ui.frame(ui.dp(54), ui.dp(54), Gravity.TOP | Gravity.RIGHT);
        callParams.topMargin = ui.dp(84);
        callParams.rightMargin = ui.dp(22);
        root.addView(call, callParams);

        LinearLayout dialogue = new LinearLayout(activity);
        dialogue.setOrientation(LinearLayout.VERTICAL);
        dialogue.setGravity(Gravity.CENTER);
        dialogue.setPadding(ui.dp(18), ui.dp(12), ui.dp(18), ui.dp(12));
        dialogue.setBackground(subtitleBackground(ui));
        TextView lastLine = ui.text(model.lastLine, 18, Color.WHITE, 0);
        lastLine.setGravity(Gravity.CENTER);
        lastLine.setMaxLines(3);
        lastLine.setLineSpacing(ui.dp(3), 1.0f);
        lastLine.setShadowLayer(ui.dp(3), 0, ui.dp(1), 0x99000000);
        dialogue.addView(lastLine, new LinearLayout.LayoutParams(-1, -2));
        TextView stateLabel = ui.text(model.stateLabel, 11, 0xB8FFFFFF, 0);
        stateLabel.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams stateInDialogue = new LinearLayout.LayoutParams(-1, -2);
        stateInDialogue.topMargin = ui.dp(6);
        dialogue.addView(stateLabel, stateInDialogue);
        FrameLayout.LayoutParams dialogueParams = ui.frame(-1, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        dialogueParams.leftMargin = ui.dp(18);
        dialogueParams.rightMargin = ui.dp(18);
        dialogueParams.bottomMargin = ui.dp(172);
        root.addView(dialogue, dialogueParams);

        TextView asrHint = ui.text(TextModeAsrGesture.label(TextModeAsrGesture.NEUTRAL), 15, 0xCCFFFFFF, 0);
        asrHint.setGravity(Gravity.CENTER);
        asrHint.setVisibility(model.asrListening ? View.VISIBLE : View.GONE);
        FrameLayout.LayoutParams hintParams = ui.frame(-1, ui.dp(32), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        hintParams.bottomMargin = ui.dp(124);
        root.addView(asrHint, hintParams);

        View contentCard = null;
        if (model.weather != null) {
            contentCard = WeatherCard.voice(activity, ui, model.weather);
        } else if (model.news != null) {
            contentCard = NewsCard.voice(activity, ui, model.news);
        }
        if (contentCard != null) {
            FrameLayout.LayoutParams cardParams = ui.frame(-1, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            cardParams.leftMargin = ui.dp(18);
            cardParams.rightMargin = ui.dp(18);
            cardParams.topMargin = ui.dp(146);
            root.addView(contentCard, cardParams);
        }

        ImageButton asr = new ImageButton(activity);
        asr.setImageResource(model.asrListening ? R.drawable.ic_stop_text_input : R.drawable.ic_mic_text_input);
        asr.setColorFilter(0xFFFFFFFF);
        asr.setBackground(controlBackground(ui, 0xAAFF6377, 999));
        asr.setPadding(ui.dp(24), ui.dp(24), ui.dp(24), ui.dp(24));
        asr.setContentDescription("按住说话");
        asr.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                callbacks.onAsrPressStart(event.getRawY());
                return true;
            case MotionEvent.ACTION_MOVE:
                callbacks.onAsrPressMove(event.getRawY());
                return true;
            case MotionEvent.ACTION_UP:
                v.performClick();
                callbacks.onAsrPressEnd(event.getRawY());
                return true;
            case MotionEvent.ACTION_CANCEL:
                callbacks.onAsrPressCancel();
                return true;
            default:
                return true;
            }
        });
        FrameLayout.LayoutParams asrParams = ui.frame(ui.dp(94), ui.dp(94), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        asrParams.bottomMargin = ui.dp(20);
        root.addView(asr, asrParams);

        top.bringToFront();
        call.bringToFront();
        if (contentCard != null) contentCard.bringToFront();
        dialogue.bringToFront();
        asrHint.bringToFront();
        asr.bringToFront();
        return new Views(root, rootState.moodVeil, null, null, avatarView,
                lastLine, stateLabel, null, null, null, null, asr, asrHint);
    }

    static Views renderVoice(MainActivity activity, HerUi ui, VoiceModel model, Callbacks callbacks) {
        HerUi.Root rootState = ui.baseRoot(model.mood);
        FrameLayout root = rootState.frame;
        LinearLayout top = ui.topBar("☰", "", "Aa", callbacks::onSettings, callbacks::onChat);
        root.addView(top);
        boolean assetsFullscreen = model.digitalAvatarEnabled &&
                AvatarPlaybackMode.isAssetsFullscreen(model.avatarPlaybackMode);

        LinearLayout center = new LinearLayout(activity);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(model.digitalAvatarEnabled ? Gravity.TOP | Gravity.CENTER_HORIZONTAL : Gravity.CENTER);
        center.setPadding(ui.dp(30), model.digitalAvatarEnabled ? ui.dp(108) : 0, ui.dp(30),
                model.digitalAvatarEnabled ? 0 : ui.dp(86));
        root.addView(center, ui.frame(-1, -1));

        VoiceOrbView voiceOrbView = null;
        DigitalAvatarView digitalAvatarView = null;
        AssetVideoAvatarView assetVideoAvatarView = null;
        if (model.digitalAvatarEnabled) {
            if (assetsFullscreen) {
                assetVideoAvatarView = new AssetVideoAvatarView(activity, model.tabletDemoCharacter);
                assetVideoAvatarView.setSpeaking(model.avatarSpeaking);
                root.addView(assetVideoAvatarView, ui.frame(-1, -1));
            } else {
                digitalAvatarView = new DigitalAvatarView(activity);
                digitalAvatarView.setAvatarState(model.avatarEmotion, model.avatarSpeaking);
                FrameLayout.LayoutParams avatarParams = ui.frame(ui.dp(600), ui.dp(800), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
                root.addView(digitalAvatarView, avatarParams);
            }
        } else {
            voiceOrbView = new VoiceOrbView(activity);
            voiceOrbView.setOnClickListener(v -> callbacks.onToggleMic());
            center.addView(voiceOrbView, new LinearLayout.LayoutParams(ui.dp(178), ui.dp(178)));
        }

        TextView voiceLastTurnView = null;
        TextView stateLabel = null;
        if (assetsFullscreen && model.tabletDemoMode) {
            voiceLastTurnView = ui.text(model.lastLine, 20, Color.WHITE, 0);
            voiceLastTurnView.setGravity(Gravity.CENTER);
            voiceLastTurnView.setLineSpacing(ui.dp(4), 1.0f);
            voiceLastTurnView.setMaxLines(3);
            voiceLastTurnView.setShadowLayer(ui.dp(4), 0, ui.dp(1), 0xCC000000);
            voiceLastTurnView.setBackground(subtitleBackground(ui));
            voiceLastTurnView.setPadding(ui.dp(22), ui.dp(10), ui.dp(22), ui.dp(10));
            FrameLayout.LayoutParams lastParams = ui.frame(-1, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            lastParams.leftMargin = ui.dp(22);
            lastParams.rightMargin = ui.dp(22);
            lastParams.bottomMargin = ui.dp(88);
            root.addView(voiceLastTurnView, lastParams);

            stateLabel = ui.text(model.stateLabel, 12, 0xB8FFFFFF, 0);
            stateLabel.setGravity(Gravity.CENTER);
            FrameLayout.LayoutParams stateParams = ui.frame(-1, ui.dp(28), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            stateParams.bottomMargin = ui.dp(66);
            root.addView(stateLabel, stateParams);
        } else if (!assetsFullscreen) {
            voiceLastTurnView = ui.text(model.lastLine, 22, Color.WHITE, 0);
            voiceLastTurnView.setGravity(Gravity.CENTER);
            voiceLastTurnView.setLineSpacing(ui.dp(4), 1.0f);
            voiceLastTurnView.setPadding(ui.dp(8), ui.dp(34), ui.dp(8), 0);
            LinearLayout.LayoutParams lastParams = new LinearLayout.LayoutParams(-1, -2);
            lastParams.topMargin = ui.dp(6);
            center.addView(voiceLastTurnView, lastParams);

            stateLabel = ui.text(model.stateLabel, 12, 0xA8FFE0E0, 0);
            stateLabel.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams stateParams = new LinearLayout.LayoutParams(-1, -2);
            stateParams.topMargin = ui.dp(16);
            center.addView(stateLabel, stateParams);
        }

        AudioLevelView audioLevelView = null;
        TextView micButton = null;
        TextView hangUpButton = ui.text("挂断", 15, Color.WHITE, 700);
        hangUpButton.setGravity(Gravity.CENTER);
        hangUpButton.setBackground(controlBackground(ui, 0xCCB3263A, 22));
        hangUpButton.setOnClickListener(v -> callbacks.onHangUp());
        FrameLayout.LayoutParams hangUpParams = ui.frame(ui.dp(94), ui.dp(52),
                Gravity.BOTTOM | Gravity.RIGHT);
        hangUpParams.rightMargin = ui.dp(28);
        hangUpParams.bottomMargin = ui.dp(20);
        root.addView(hangUpButton, hangUpParams);
        if (assetsFullscreen && model.tabletDemoMode) {
            micButton = ui.text(model.demoMicPaused ? "▶" : "Ⅱ", 30, 0xFFFF6377, 0);
            micButton.setGravity(Gravity.CENTER);
            micButton.setOnClickListener(v -> callbacks.onToggleMic());
            FrameLayout.LayoutParams micParams = ui.frame(ui.dp(78), ui.dp(70), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            micParams.bottomMargin = ui.dp(12);
            root.addView(micButton, micParams);
        } else if (!model.digitalAvatarEnabled) {
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
        if (voiceLastTurnView != null) voiceLastTurnView.bringToFront();
        if (stateLabel != null) stateLabel.bringToFront();
        if (audioLevelView != null) audioLevelView.bringToFront();
        if (micButton != null) micButton.bringToFront();
        hangUpButton.bringToFront();
        top.bringToFront();
        return new Views(root, rootState.moodVeil, voiceOrbView, digitalAvatarView, assetVideoAvatarView,
                voiceLastTurnView, stateLabel, audioLevelView, micButton, null, null);
    }

    private static GradientDrawable subtitleBackground(HerUi ui) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(0x8C000000);
        background.setCornerRadius(ui.dp(8));
        return background;
    }

    private static GradientDrawable controlBackground(HerUi ui, int color, int radiusDp) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(ui.dp(radiusDp));
        background.setStroke(ui.dp(1), 0x33FFFFFF);
        return background;
    }

    private static ImageButton phoneButton(MainActivity activity, HerUi ui, Runnable action) {
        ImageButton button = new ImageButton(activity);
        button.setImageResource(R.drawable.ic_phone_call);
        button.setColorFilter(0xFFFFFFFF);
        button.setBackground(controlBackground(ui, 0x661C141A, 999));
        button.setPadding(ui.dp(14), ui.dp(14), ui.dp(14), ui.dp(14));
        button.setContentDescription("语音通话");
        button.setOnClickListener(v -> action.run());
        return button;
    }

    interface Callbacks {
        void onSettings();
        void onChat();
        void onVoiceHome();
        void onToggleMic();
        default void onHangUp() { }
        default void onAsrPressStart(float rawY) { }
        default void onAsrPressMove(float rawY) { }
        default void onAsrPressEnd(float rawY) { }
        default void onAsrPressCancel() { }
    }

    static final class LandingModel {
        final String userName;
        final String agentName;
        final int mood;
        final boolean jessAvatarHome;
        final String lastLine;
        final String stateLabel;
        final boolean asrListening;
        final boolean avatarSpeaking;
        final WeatherTool.WeatherResult weather;
        final NewsTool.NewsResult news;

        LandingModel(String userName, String agentName, int mood) {
            this(userName, agentName, mood, false, "", "", false, false, null, null);
        }

        LandingModel(String userName, String agentName, int mood, boolean jessAvatarHome,
                String lastLine, String stateLabel, boolean asrListening, boolean avatarSpeaking,
                WeatherTool.WeatherResult weather, NewsTool.NewsResult news) {
            this.userName = userName;
            this.agentName = agentName;
            this.mood = mood;
            this.jessAvatarHome = jessAvatarHome;
            this.lastLine = lastLine;
            this.stateLabel = stateLabel;
            this.asrListening = asrListening;
            this.avatarSpeaking = avatarSpeaking;
            this.weather = weather;
            this.news = news;
        }
    }

    static final class VoiceModel {
        final String lastLine;
        final String stateLabel;
        final int mood;
        final WeatherTool.WeatherResult weather;
        final NewsTool.NewsResult news;
        final boolean digitalAvatarEnabled;
        final String avatarPlaybackMode;
        final boolean avatarSpeaking;
        final String avatarEmotion;
        final boolean tabletDemoMode;
        final boolean demoMicPaused;
        final TabletDemoCharacter tabletDemoCharacter;

        VoiceModel(String lastLine, String stateLabel, int mood, WeatherTool.WeatherResult weather, NewsTool.NewsResult news,
                boolean digitalAvatarEnabled, String avatarPlaybackMode, boolean avatarSpeaking, String avatarEmotion) {
            this(lastLine, stateLabel, mood, weather, news, digitalAvatarEnabled, avatarPlaybackMode,
                    avatarSpeaking, avatarEmotion, false, false, null);
        }

        VoiceModel(String lastLine, String stateLabel, int mood, WeatherTool.WeatherResult weather, NewsTool.NewsResult news,
                boolean digitalAvatarEnabled, String avatarPlaybackMode, boolean avatarSpeaking, String avatarEmotion,
                boolean tabletDemoMode, boolean demoMicPaused, TabletDemoCharacter tabletDemoCharacter) {
            this.lastLine = lastLine;
            this.stateLabel = stateLabel;
            this.mood = mood;
            this.weather = weather;
            this.news = news;
            this.digitalAvatarEnabled = digitalAvatarEnabled;
            this.avatarPlaybackMode = AvatarPlaybackMode.normalize(avatarPlaybackMode);
            this.avatarSpeaking = avatarSpeaking;
            this.avatarEmotion = avatarEmotion;
            this.tabletDemoMode = tabletDemoMode;
            this.demoMicPaused = demoMicPaused;
            this.tabletDemoCharacter = tabletDemoCharacter;
        }
    }

    static final class Views {
        final FrameLayout root;
        final MoodVeil moodVeil;
        final VoiceOrbView voiceOrbView;
        final DigitalAvatarView digitalAvatarView;
        final AssetVideoAvatarView assetVideoAvatarView;
        final TextView voiceLastTurnView;
        final TextView stateLabel;
        final AudioLevelView audioLevelView;
        final TextView micButton;
        final TextView homeTimeView;
        final TextView handwrittenNameView;
        final ImageButton textAsrButton;
        final TextView textAsrHint;

        Views(FrameLayout root, MoodVeil moodVeil, VoiceOrbView voiceOrbView, DigitalAvatarView digitalAvatarView,
                AssetVideoAvatarView assetVideoAvatarView, TextView voiceLastTurnView,
                TextView stateLabel, AudioLevelView audioLevelView, TextView micButton,
                TextView homeTimeView, TextView handwrittenNameView) {
            this(root, moodVeil, voiceOrbView, digitalAvatarView, assetVideoAvatarView,
                    voiceLastTurnView, stateLabel, audioLevelView, micButton,
                    homeTimeView, handwrittenNameView, null, null);
        }

        Views(FrameLayout root, MoodVeil moodVeil, VoiceOrbView voiceOrbView, DigitalAvatarView digitalAvatarView,
                AssetVideoAvatarView assetVideoAvatarView, TextView voiceLastTurnView,
                TextView stateLabel, AudioLevelView audioLevelView, TextView micButton,
                TextView homeTimeView, TextView handwrittenNameView,
                ImageButton textAsrButton, TextView textAsrHint) {
            this.root = root;
            this.moodVeil = moodVeil;
            this.voiceOrbView = voiceOrbView;
            this.digitalAvatarView = digitalAvatarView;
            this.assetVideoAvatarView = assetVideoAvatarView;
            this.voiceLastTurnView = voiceLastTurnView;
            this.stateLabel = stateLabel;
            this.audioLevelView = audioLevelView;
            this.micButton = micButton;
            this.homeTimeView = homeTimeView;
            this.handwrittenNameView = handwrittenNameView;
            this.textAsrButton = textAsrButton;
            this.textAsrHint = textAsrHint;
        }
    }
}
