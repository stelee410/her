package com.linkyun.her;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

final class BubbleDrawable extends android.graphics.drawable.Drawable {
    final boolean user;
    final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;

    BubbleDrawable(boolean user, float density) {
        this.user = user;
        this.density = density;
    }

    @Override public void draw(Canvas canvas) {
        paint.setColor(user ? 0xB08F3846 : 0xB0A93C4E);
        canvas.drawRoundRect(new RectF(getBounds()), dp(7), dp(7), paint);
    }

    @Override public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override public void setColorFilter(android.graphics.ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
    }

    @Override public int getOpacity() {
        return android.graphics.PixelFormat.TRANSLUCENT;
    }

    private int dp(float value) {
        return Math.round(value * density);
    }
}
