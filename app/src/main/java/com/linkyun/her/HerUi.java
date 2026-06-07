package com.linkyun.her;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

final class HerUi {
    final Activity activity;

    HerUi(Activity activity) {
        this.activity = activity;
    }

    Root baseRoot(int mood) {
        FrameLayout frame = new FrameLayout(activity);
        ImageView bg = new ImageView(activity);
        bg.setImageResource(activity.getResources().getIdentifier("her_background", "drawable", activity.getPackageName()));
        bg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        frame.addView(bg, frame(-1, -1));
        MoodVeil moodVeil = new MoodVeil(activity);
        moodVeil.setMood(mood);
        frame.addView(moodVeil, frame(-1, -1));
        return new Root(frame, moodVeil);
    }

    LinearLayout topBar(String left, String title, String right, Runnable leftAction, Runnable rightAction) {
        LinearLayout bar = new LinearLayout(activity);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(22), dp(20), dp(22), 0);
        TextView l = icon(left);
        if (leftAction != null) {
            l.setOnClickListener(v -> leftAction.run());
        } else {
            l.setClickable(false);
            l.setFocusable(false);
        }
        bar.addView(l, new LinearLayout.LayoutParams(dp(54), dp(58)));
        TextView middle = text(title, 18, Color.WHITE, 500);
        middle.setGravity(Gravity.CENTER);
        bar.addView(middle, new LinearLayout.LayoutParams(0, dp(58), 1));
        TextView r = icon(right);
        if (rightAction != null) {
            r.setOnClickListener(v -> rightAction.run());
        } else {
            r.setClickable(false);
            r.setFocusable(false);
        }
        bar.addView(r, new LinearLayout.LayoutParams(dp(54), dp(58)));
        bar.setLayoutParams(frame(-1, dp(86), Gravity.TOP));
        return bar;
    }

    LinearLayout screenList(FrameLayout root) {
        LinearLayout list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(28), dp(92), dp(28), dp(28));
        root.addView(list, frame(-1, -1));
        return list;
    }

    LinearLayout row() {
        LinearLayout row = new LinearLayout(activity);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));
        row.setMinimumHeight(dp(72));
        row.setBackground(new BottomLineDrawable());
        return row;
    }

    View navRow(String symbol, String label, String value, Runnable action) {
        LinearLayout row = row();
        TextView s = text(symbol, 23, 0xCCFFFFFF, 0);
        s.setGravity(Gravity.CENTER);
        row.addView(s, new LinearLayout.LayoutParams(dp(48), -1));
        addNavRowText(row, label, value, action);
        return row;
    }

    View navRow(int iconResId, String label, String value, Runnable action) {
        LinearLayout row = row();
        ImageView icon = new ImageView(activity);
        icon.setImageResource(iconResId);
        icon.setColorFilter(0xCCFFFFFF);
        icon.setPadding(dp(12), dp(18), dp(12), dp(18));
        row.addView(icon, new LinearLayout.LayoutParams(dp(48), -1));
        addNavRowText(row, label, value, action);
        return row;
    }

    private void addNavRowText(LinearLayout row, String label, String value, Runnable action) {
        row.addView(text(label, 16, Color.WHITE, 0), new LinearLayout.LayoutParams(0, -1, 1));
        TextView val = text(value, 13, 0x99FFE0E0, 0);
        val.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        row.addView(val, new LinearLayout.LayoutParams(dp(150), -1));
        TextView chevron = text(action == null ? "" : "›", 28, 0xCCFFFFFF, 0);
        chevron.setGravity(Gravity.CENTER);
        row.addView(chevron, new LinearLayout.LayoutParams(dp(28), -1));
        if (action != null) row.setOnClickListener(v -> action.run());
    }

    TextView icon(String value) {
        TextView view = text(value, 29, Color.WHITE, 0);
        view.setGravity(Gravity.CENTER);
        view.setClickable(true);
        return view;
    }

    TextView text(String value, int sp, int color, int weight) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setIncludeFontPadding(true);
        if (weight > 0) view.setTypeface(Typeface.DEFAULT, weight >= 700 ? Typeface.BOLD : Typeface.NORMAL);
        return view;
    }

    BubbleDrawable bubbleDrawable(boolean user) {
        return new BubbleDrawable(user, activity.getResources().getDisplayMetrics().density);
    }

    FrameLayout.LayoutParams frame(int width, int height) {
        return new FrameLayout.LayoutParams(width, height);
    }

    FrameLayout.LayoutParams frame(int width, int height, int gravity) {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height);
        params.gravity = gravity;
        return params;
    }

    int dp(float value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    static final class Root {
        final FrameLayout frame;
        final MoodVeil moodVeil;

        Root(FrameLayout frame, MoodVeil moodVeil) {
            this.frame = frame;
            this.moodVeil = moodVeil;
        }
    }
}
