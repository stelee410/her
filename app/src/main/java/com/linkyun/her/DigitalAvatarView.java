package com.linkyun.her;

import android.content.Context;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.io.IOException;

final class DigitalAvatarView extends FrameLayout {
    private static final long CROSSFADE_MS = 160;

    private final ImageView idleImage;
    private final ImageView speakingImage;
    private final ImageView actionImage;
    private boolean speaking = false;
    private boolean actionPlaying = false;
    private String emotion = AvatarVideoCatalog.EMOTION_NEUTRAL;
    private int wardrobeIndex = 0;
    private int idleResourceId = 0;
    private int speakingResourceId = 0;

    DigitalAvatarView(Context context) {
        super(context);
        setBackgroundColor(Color.TRANSPARENT);
        setClickable(true);
        idleImage = imageView(context);
        speakingImage = imageView(context);
        actionImage = imageView(context);
        addView(idleImage, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        addView(speakingImage, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        addView(actionImage, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        actionImage.setAlpha(0f);
        setOnClickListener(v -> cycleWardrobe());
        applyResources();
        applySpeakingState(false);
    }

    void setAvatarState(String emotion, boolean speaking) {
        String normalized = AvatarVideoCatalog.normalizeEmotion(emotion);
        boolean emotionChanged = !this.emotion.equals(normalized);
        this.emotion = normalized;
        this.speaking = speaking;
        if (emotionChanged) applyResources();
        if (!actionPlaying) applySpeakingState(true);
    }

    void setSpeaking(boolean speaking) {
        setAvatarState(emotion, speaking);
    }

    void playOnce(int resourceId) {
        if (resourceId == 0) return;
        actionPlaying = true;
        idleImage.animate().cancel();
        speakingImage.animate().cancel();
        actionImage.animate().cancel();
        idleImage.animate().alpha(0f).setDuration(CROSSFADE_MS).start();
        speakingImage.animate().alpha(0f).setDuration(CROSSFADE_MS).start();
        setAnimatedResource(actionImage, resourceId, false, this::finishActionPlayback);
        actionImage.setAlpha(0f);
        actionImage.animate().alpha(1f).setDuration(CROSSFADE_MS).start();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAnimationIfPossible(idleImage);
        startAnimationIfPossible(speakingImage);
        startAnimationIfPossible(actionImage);
    }

    @Override
    protected void onDetachedFromWindow() {
        stopAnimationIfPossible(idleImage);
        stopAnimationIfPossible(speakingImage);
        stopAnimationIfPossible(actionImage);
        super.onDetachedFromWindow();
    }

    private ImageView imageView(Context context) {
        ImageView view = new ImageView(context);
        view.setBackgroundColor(Color.TRANSPARENT);
        view.setAdjustViewBounds(true);
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        return view;
    }

    private void cycleWardrobe() {
        wardrobeIndex = AvatarVideoCatalog.nextWardrobeIndex(wardrobeIndex);
        applyResources();
        if (!actionPlaying) applySpeakingState(true);
    }

    private void applyResources() {
        int nextIdle = AvatarVideoCatalog.idleVideo(emotion, wardrobeIndex);
        int nextSpeaking = AvatarVideoCatalog.speakingVideo(emotion, wardrobeIndex);
        if (idleResourceId != nextIdle) {
            idleResourceId = nextIdle;
            setAnimatedResource(idleImage, idleResourceId);
        }
        if (speakingResourceId != nextSpeaking) {
            speakingResourceId = nextSpeaking;
            setAnimatedResource(speakingImage, speakingResourceId);
        }
    }

    private void applySpeakingState(boolean animate) {
        if (actionPlaying) return;
        float idleAlpha = speaking ? 0f : 1f;
        float speakingAlpha = speaking ? 1f : 0f;
        idleImage.animate().cancel();
        speakingImage.animate().cancel();
        if (animate) {
            idleImage.animate().alpha(idleAlpha).setDuration(CROSSFADE_MS).start();
            speakingImage.animate().alpha(speakingAlpha).setDuration(CROSSFADE_MS).start();
        } else {
            idleImage.setAlpha(idleAlpha);
            speakingImage.setAlpha(speakingAlpha);
        }
        startAnimationIfPossible(idleImage);
        startAnimationIfPossible(speakingImage);
    }

    private void setAnimatedResource(ImageView image, int resourceId) {
        setAnimatedResource(image, resourceId, true, null);
    }

    private void setAnimatedResource(ImageView image, int resourceId, boolean loop, Runnable onEnd) {
        stopAnimationIfPossible(image);
        if (Build.VERSION.SDK_INT >= 28) {
            try {
                ImageDecoder.Source source = ImageDecoder.createSource(getResources(), resourceId);
                Drawable drawable = ImageDecoder.decodeDrawable(source);
                image.setImageDrawable(drawable);
                startAnimationIfPossible(image, loop, onEnd);
                return;
            } catch (IOException ignored) {
            }
        }
        image.setImageResource(resourceId);
    }

    private void startAnimationIfPossible(ImageView image) {
        startAnimationIfPossible(image, true, null);
    }

    private void startAnimationIfPossible(ImageView image, boolean loop, Runnable onEnd) {
        if (Build.VERSION.SDK_INT < 28) return;
        Drawable drawable = image.getDrawable();
        if (drawable instanceof AnimatedImageDrawable) {
            AnimatedImageDrawable animated = (AnimatedImageDrawable) drawable;
            animated.clearAnimationCallbacks();
            animated.setRepeatCount(loop ? AnimatedImageDrawable.REPEAT_INFINITE : 0);
            if (!loop && onEnd != null) {
                animated.registerAnimationCallback(new Animatable2.AnimationCallback() {
                    @Override public void onAnimationEnd(Drawable drawable) {
                        post(onEnd);
                    }
                });
            }
            if (!animated.isRunning()) animated.start();
        }
    }

    private void stopAnimationIfPossible(ImageView image) {
        if (Build.VERSION.SDK_INT < 28) return;
        Drawable drawable = image.getDrawable();
        if (drawable instanceof AnimatedImageDrawable) {
            AnimatedImageDrawable animated = (AnimatedImageDrawable) drawable;
            animated.clearAnimationCallbacks();
            animated.stop();
        }
    }

    private void finishActionPlayback() {
        if (!actionPlaying) return;
        actionPlaying = false;
        speaking = false;
        actionImage.animate().cancel();
        actionImage.animate().alpha(0f).setDuration(CROSSFADE_MS).withEndAction(() -> {
            stopAnimationIfPossible(actionImage);
            actionImage.setImageDrawable(null);
        }).start();
        applyResources();
        applySpeakingState(true);
    }
}
