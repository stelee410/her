package com.linkyun.her;

import android.graphics.Canvas;
import android.graphics.Paint;

final class BottomLineDrawable extends android.graphics.drawable.ColorDrawable {
    final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    @Override public void draw(Canvas canvas) {
        super.draw(canvas);
        paint.setColor(0x18FFFFFF);
        paint.setStrokeWidth(1);
        canvas.drawLine(0, getBounds().bottom - 1, getBounds().right, getBounds().bottom - 1, paint);
    }
}
