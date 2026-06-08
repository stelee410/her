package com.linkyun.her;

import android.content.Context;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.io.IOException;

final class DigitalAvatarView extends FrameLayout {
    private final ImageView image;
    private int currentResourceId = 0;
    private boolean speaking = false;

    DigitalAvatarView(Context context) {
        super(context);
        setBackgroundColor(Color.TRANSPARENT);
        setClickable(true);

        image = new ImageView(context);
        image.setBackgroundColor(Color.TRANSPARENT);
        image.setAdjustViewBounds(true);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        addView(image, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
    }

    void setSpeaking(boolean speaking) {
        this.speaking = speaking;
        int resourceId = speaking ? R.drawable.avatar_talking : R.drawable.avatar_standby;
        if (currentResourceId == resourceId) {
            startAnimationIfPossible();
            return;
        }
        currentResourceId = resourceId;
        if (Build.VERSION.SDK_INT >= 28) {
            loadAnimatedResource(resourceId);
        } else {
            image.setImageResource(speaking
                    ? R.drawable.avatar_talking_fallback
                    : R.drawable.avatar_standby_fallback);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (currentResourceId == 0) setSpeaking(speaking);
        startAnimationIfPossible();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopAnimationIfPossible();
        super.onDetachedFromWindow();
    }

    private void loadAnimatedResource(int resourceId) {
        try {
            ImageDecoder.Source source = ImageDecoder.createSource(getResources(), resourceId);
            Drawable drawable = ImageDecoder.decodeDrawable(source);
            image.setImageDrawable(drawable);
            startAnimationIfPossible();
        } catch (IOException error) {
            image.setImageResource(speaking
                    ? R.drawable.avatar_talking_fallback
                    : R.drawable.avatar_standby_fallback);
        }
    }

    private void startAnimationIfPossible() {
        if (Build.VERSION.SDK_INT < 28) return;
        Drawable drawable = image.getDrawable();
        if (drawable instanceof AnimatedImageDrawable) {
            AnimatedImageDrawable animated = (AnimatedImageDrawable) drawable;
            animated.setRepeatCount(AnimatedImageDrawable.REPEAT_INFINITE);
            if (!animated.isRunning()) animated.start();
        }
    }

    private void stopAnimationIfPossible() {
        if (Build.VERSION.SDK_INT < 28) return;
        Drawable drawable = image.getDrawable();
        if (drawable instanceof AnimatedImageDrawable) {
            ((AnimatedImageDrawable) drawable).stop();
        }
    }
}
