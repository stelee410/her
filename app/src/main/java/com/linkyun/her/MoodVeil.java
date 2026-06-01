package com.linkyun.her;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.View;

final class MoodVeil extends View {
    final Paint paint = new Paint();
    int mood = 0;

    MoodVeil(Activity activity) {
        super(activity);
    }

    void setMood(int next) {
        if (mood == next) return;
        mood = next;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        int start;
        int end;
        int wash;
        if (mood == 1) {
            start = 0x806E6EA8;
            end = 0xAA221C34;
            wash = 0x262B2343;
        } else if (mood == 2) {
            start = 0x80D64C58;
            end = 0xAA3D2530;
            wash = 0x24FFB15C;
        } else if (mood == 3) {
            start = 0x8055798B;
            end = 0xAA202D35;
            wash = 0x24233345;
        } else {
            start = 0x80C93445;
            end = 0xAA281D2C;
            wash = 0x2230182A;
        }
        paint.setShader(new LinearGradient(0, 0, getWidth(), getHeight(),
                start, end, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        paint.setShader(null);
        paint.setColor(wash);
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
    }
}
