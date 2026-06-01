package com.linkyun.her;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

class HerMarkView extends View {
    final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    HerMarkView(Activity activity) {
        super(activity);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setClickable(true);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float r = Math.min(w, h) * 0.37f;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(0xDDFFFFFF);
        paint.setStrokeWidth(dp(1));
        canvas.drawCircle(cx, cy, r, paint);
        paint.setColor(0x44FFFFFF);
        canvas.drawCircle(cx, cy, r + dp(12), paint);
        canvas.drawCircle(cx, cy, r + dp(22), paint);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(dp(4));
        Path path = new Path();
        float left = cx - r * 0.55f;
        float right = cx + r * 0.55f;
        path.moveTo(left, cy);
        path.cubicTo(left, cy - r * 0.36f, cx - r * 0.12f, cy - r * 0.36f, cx, cy);
        path.cubicTo(cx + r * 0.12f, cy + r * 0.36f, right, cy + r * 0.36f, right, cy);
        path.cubicTo(right, cy - r * 0.36f, cx + r * 0.12f, cy - r * 0.36f, cx, cy);
        path.cubicTo(cx - r * 0.12f, cy + r * 0.36f, left, cy + r * 0.36f, left, cy);
        canvas.drawPath(path, paint);
    }

    int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
