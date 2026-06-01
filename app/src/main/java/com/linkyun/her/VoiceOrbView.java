package com.linkyun.her;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.os.SystemClock;

final class VoiceOrbView extends HerMarkView {
    final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    long started = SystemClock.uptimeMillis();
    int level = 0;
    String conversationState = "idle";

    VoiceOrbView(Activity activity) {
        super(activity);
    }

    void setLevel(int next) {
        level = Math.max(0, Math.min(100, next));
        invalidate();
    }

    void setConversationState(String next) {
        conversationState = next == null ? "idle" : next;
        invalidate();
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        postInvalidateOnAnimation();
    }

    @Override protected void onDraw(Canvas canvas) {
        float t = ((SystemClock.uptimeMillis() - started) % 2400) / 2400f;
        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float energy = level / 100f;
        if ("speaking".equals(conversationState)) {
            energy = Math.max(energy, 0.34f + 0.14f * (float) Math.sin(t * Math.PI * 4));
        } else if ("thinking".equals(conversationState) || "connecting".equals(conversationState)) {
            energy = Math.max(energy, 0.18f + 0.08f * (float) Math.sin(t * Math.PI * 2));
        } else if ("listening".equals(conversationState)) {
            energy = Math.max(energy, 0.12f);
        }

        float base = Math.min(w, h) * (0.35f + energy * 0.035f);
        glow.setStyle(Paint.Style.FILL);
        glow.setShader(new LinearGradient(0, 0, w, h,
                0x33FFFFFF, "listening".equals(conversationState) ? 0x33FF6377 : 0x22B96A7C,
                Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, base + dp(18) * energy, glow);
        glow.setShader(null);

        glow.setStyle(Paint.Style.STROKE);
        glow.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < 4; i++) {
            float phase = (t + i * 0.2f) % 1f;
            int alpha = (int) ((55 + 85 * energy) * (1f - phase));
            glow.setColor((Math.max(0, Math.min(160, alpha)) << 24) | 0x00FFFFFF);
            glow.setStrokeWidth(dp(1.2f + energy * 2.2f));
            canvas.drawCircle(cx, cy, base + dp(10) + phase * dp(44 + 18 * energy), glow);
        }

        canvas.save();
        float scale = 1f + energy * 0.05f;
        canvas.scale(scale, scale, cx, cy);
        super.onDraw(canvas);
        canvas.restore();
        postInvalidateOnAnimation();
    }
}
