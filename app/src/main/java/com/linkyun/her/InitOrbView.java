package com.linkyun.her;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.os.SystemClock;

final class InitOrbView extends HerMarkView {
    interface SummaryState {
        boolean isSummarizing();
    }

    final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    final SummaryState summaryState;
    long started = SystemClock.uptimeMillis();

    InitOrbView(Activity activity, SummaryState summaryState) {
        super(activity);
        this.summaryState = summaryState;
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        postInvalidateOnAnimation();
    }

    @Override protected void onDraw(Canvas canvas) {
        float t = ((SystemClock.uptimeMillis() - started) % 3600) / 3600f;
        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        boolean summarizing = summaryState != null && summaryState.isSummarizing();
        if (summarizing) {
            float flip = (float) Math.cos(t * Math.PI * 8);
            canvas.save();
            canvas.scale(Math.max(0.18f, Math.abs(flip)), 1f, cx, cy);
            canvas.rotate(t * 360f, cx, cy);
        }
        float base = Math.min(w, h) * 0.38f;
        glow.setStyle(Paint.Style.STROKE);
        glow.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < 4; i++) {
            float phase = (t + i * 0.18f) % 1f;
            int alpha = (int) (95 * (1f - phase));
            glow.setColor((alpha << 24) | 0x00FFFFFF);
            glow.setStrokeWidth(dp(1.2f + i * 0.3f));
            canvas.drawCircle(cx, cy, base + dp(8) + phase * dp(36), glow);
        }
        glow.setStyle(Paint.Style.FILL);
        glow.setShader(new LinearGradient(0, 0, w, h, 0x44FFFFFF, 0x11FF6377, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, base * (0.92f + 0.04f * (float) Math.sin(t * Math.PI * 2)), glow);
        glow.setShader(null);
        super.onDraw(canvas);
        if (summarizing) canvas.restore();
        postInvalidateOnAnimation();
    }
}
