package com.linkyun.her;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

final class AudioLevelView extends View {
    final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    int level = 0;

    AudioLevelView(Activity activity) {
        super(activity);
    }

    void setLevel(int next) {
        level = Math.max(0, Math.min(100, next));
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        float radius = getHeight() / 2f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x26FFFFFF);
        canvas.drawRoundRect(new RectF(0, 0, getWidth(), getHeight()), radius, radius, paint);
        float width = Math.max(getHeight(), getWidth() * (level / 100f));
        paint.setColor(0xFFFF6377);
        canvas.drawRoundRect(new RectF(0, 0, width, getHeight()), radius, radius, paint);
    }
}
