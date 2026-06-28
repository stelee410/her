package com.linkyun.her;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

import java.util.ArrayList;
import java.util.List;

final class RetroTvOverlayView extends FrameLayout {
    interface Listener {
        void onDismiss();
    }

    private static final long STARTUP_MS = 1450;

    private final HerUi ui;
    private final Listener listener;
    private final FrameLayout stage;
    private final LinearLayout body;
    private final LinearLayout header;
    private final FrameLayout screen;
    private final LinearLayout controls;
    private final PlayerView playerView;
    private final ExoPlayer player;
    private final View scanlines;
    private final LinearLayout channelBug;
    private final FrameLayout startupView;
    private final TextView titleView;
    private final TextView counterView;
    private final TextView emptyView;
    private final TextView playPauseButton;
    private final TextView fullButton;
    private TextView channelNameView;
    private TextView channelSubView;
    private final List<TvChannel> playlist = new ArrayList<>();
    private Runnable startupRunnable;
    private int index;
    private float touchDownX;
    private float touchDownY;
    private boolean userPaused;
    private boolean fullScreen;

    RetroTvOverlayView(Context context, HerUi ui, Listener listener) {
        super(context);
        this.ui = ui;
        this.listener = listener;
        setClickable(true);
        setFocusable(true);
        setBackgroundColor(0xF20A0907);

        stage = new FrameLayout(context);
        addView(stage);

        body = new LinearLayout(context);
        body.setOrientation(LinearLayout.VERTICAL);
        stage.addView(body, new FrameLayout.LayoutParams(-1, -1));

        header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        titleView = label("MYTV 01", 16, 0xFFFFE7A4, true);
        header.addView(titleView, new LinearLayout.LayoutParams(0, ui.dp(34), 1));
        counterView = label("00:00", 13, 0xFF7EF6B2, false);
        counterView.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        header.addView(counterView, new LinearLayout.LayoutParams(ui.dp(108), ui.dp(34)));
        body.addView(header, new LinearLayout.LayoutParams(-1, ui.dp(38)));

        screen = new FrameLayout(context);
        body.addView(screen);

        player = new ExoPlayer.Builder(context).build();
        playerView = new PlayerView(context);
        playerView.setUseController(false);
        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
        playerView.setKeepContentOnPlayerReset(true);
        playerView.setPlayer(player);
        screen.addView(playerView, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        scanlines = new ScanlineView(context);
        screen.addView(scanlines, new FrameLayout.LayoutParams(-1, -1));

        channelBug = createChannelBug(context);
        screen.addView(channelBug);
        channelBug.setVisibility(GONE);
        updateChannelBugMode();

        startupView = createStartupView(context);
        screen.addView(startupView, new FrameLayout.LayoutParams(-1, -1));

        emptyView = label("", 16, 0xFFEADBC6, false);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setPadding(ui.dp(18), 0, ui.dp(18), 0);
        screen.addView(emptyView, new FrameLayout.LayoutParams(-1, -1));

        controls = new LinearLayout(context);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        controls.setPadding(ui.dp(6), 0, ui.dp(6), 0);
        playPauseButton = control("PAUSE");
        TextView nextButton = control("NEXT");
        fullButton = control("FULL");
        TextView closeButton = control("CLOSE");
        controls.addView(playPauseButton, new LinearLayout.LayoutParams(0, ui.dp(42), 1));
        controls.addView(nextButton, new LinearLayout.LayoutParams(0, ui.dp(42), 1));
        controls.addView(fullButton, new LinearLayout.LayoutParams(0, ui.dp(42), 1));
        controls.addView(closeButton, new LinearLayout.LayoutParams(0, ui.dp(42), 1));
        body.addView(controls, new LinearLayout.LayoutParams(-1, ui.dp(46)));

        playPauseButton.setOnClickListener(v -> togglePlayback());
        nextButton.setOnClickListener(v -> playNext());
        fullButton.setOnClickListener(v -> setFullScreen(!fullScreen));
        closeButton.setOnClickListener(v -> dismiss());
        channelBug.setOnClickListener(v -> {
            if (fullScreen) setFullScreen(false);
        });
        View.OnTouchListener fullscreenSwipeListener = (view, event) -> handleFullscreenSwipe(event);
        screen.setOnTouchListener(fullscreenSwipeListener);
        playerView.setOnTouchListener(fullscreenSwipeListener);
        player.addListener(new Player.Listener() {
            @Override public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED) {
                    playNext();
                } else if (playbackState == Player.STATE_READY && !userPaused) {
                    playPauseButton.setText("PAUSE");
                } else if (playbackState == Player.STATE_BUFFERING) {
                    counterView.setText("BUFFER");
                }
            }

            @Override public void onPlayerError(PlaybackException error) {
                post(() -> {
                    if (playlist.size() > 1) playNext();
                    else showEmpty("频道暂时没有信号\n" + error.getErrorCodeName());
                });
            }
        });
        applyTvFrameLayout();
    }

    void show(List<TvChannel> channels) {
        removeStartupCallback();
        playlist.clear();
        if (channels != null) playlist.addAll(channels);
        userPaused = false;
        setVisibility(VISIBLE);
        if (playlist.isEmpty()) {
            player.stop();
            titleView.setText("MYTV");
            counterView.setText("NO SIGNAL");
            playPauseButton.setText("PLAY");
            channelBug.setVisibility(GONE);
            startupView.setVisibility(GONE);
            showEmpty("没有可用的线上频道\n也可以把视频放到 " + MyTvPlaylist.displayPath());
            return;
        }
        emptyView.setVisibility(GONE);
        channelBug.setVisibility(GONE);
        startupView.setVisibility(VISIBLE);
        index = Math.max(0, Math.min(index, playlist.size() - 1));
        titleView.setText("MYTV STARTUP");
        counterView.setText("AI TV");
        startupRunnable = () -> {
            startupRunnable = null;
            startupView.setVisibility(GONE);
            channelBug.setVisibility(VISIBLE);
            updateChannelBugMode();
            playCurrent();
        };
        postDelayed(startupRunnable, STARTUP_MS);
    }

    void dismiss() {
        removeStartupCallback();
        player.stop();
        player.clearMediaItems();
        player.release();
        if (getParent() instanceof FrameLayout) {
            ((FrameLayout) getParent()).removeView(this);
        }
        if (listener != null) listener.onDismiss();
    }

    private void playCurrent() {
        if (playlist.isEmpty()) return;
        TvChannel channel = playlist.get(index);
        titleView.setText("MYTV " + twoDigits(index + 1) + " · " + channel.title);
        counterView.setText(channel.live ? "LIVE" : (index + 1) + "/" + playlist.size());
        playPauseButton.setText("PAUSE");
        channelBug.setVisibility(VISIBLE);
        updateChannelBugMode();
        player.setMediaItem(MediaItem.fromUri(channel.uri));
        player.setPlayWhenReady(!userPaused);
        player.prepare();
    }

    private void playNext() {
        if (playlist.isEmpty()) return;
        removeStartupCallback();
        startupView.setVisibility(GONE);
        channelBug.setVisibility(VISIBLE);
        updateChannelBugMode();
        index = (index + 1) % playlist.size();
        userPaused = false;
        playCurrent();
    }

    private void playPrevious() {
        if (playlist.isEmpty()) return;
        removeStartupCallback();
        startupView.setVisibility(GONE);
        channelBug.setVisibility(VISIBLE);
        updateChannelBugMode();
        index = (index - 1 + playlist.size()) % playlist.size();
        userPaused = false;
        playCurrent();
    }

    private boolean handleFullscreenSwipe(MotionEvent event) {
        if (!fullScreen || playlist.size() <= 1) return false;
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            touchDownX = event.getX();
            touchDownY = event.getY();
            return true;
        }
        if (event.getAction() != MotionEvent.ACTION_UP) return true;
        float dx = event.getX() - touchDownX;
        float dy = event.getY() - touchDownY;
        if (Math.abs(dy) < ui.dp(72) || Math.abs(dy) < Math.abs(dx) * 1.2f) return true;
        if (dy < 0) playNext();
        else playPrevious();
        return true;
    }

    private void togglePlayback() {
        if (playlist.isEmpty()) return;
        if (player.isPlaying()) {
            userPaused = true;
            player.pause();
            playPauseButton.setText("PLAY");
        } else {
            userPaused = false;
            player.play();
            playPauseButton.setText("PAUSE");
        }
    }

    private void showEmpty(String text) {
        emptyView.setText(text);
        emptyView.setVisibility(VISIBLE);
    }

    private void setFullScreen(boolean enabled) {
        fullScreen = enabled;
        if (fullScreen) {
            applyFullScreenLayout();
        } else {
            applyTvFrameLayout();
        }
        fullButton.setText(fullScreen ? "FRAME" : "FULL");
        updateChannelBugMode();
    }

    private void updateChannelBugMode() {
        channelBug.setPadding(ui.dp(fullScreen ? 12 : 9), ui.dp(fullScreen ? 6 : 4),
                ui.dp(fullScreen ? 12 : 9), ui.dp(fullScreen ? 6 : 4));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ui.dp(fullScreen ? 188 : 132),
                ui.dp(fullScreen ? 52 : 40),
                Gravity.TOP | Gravity.LEFT);
        params.leftMargin = ui.dp(fullScreen ? 20 : 14);
        params.topMargin = ui.dp(fullScreen ? 18 : 14);
        channelBug.setLayoutParams(params);
        if (channelNameView != null) {
            channelNameView.setText("AI TV");
            channelNameView.setTextSize(fullScreen ? 16 : 14);
        }
        if (channelSubView != null) {
            channelSubView.setText(currentChannelSubtitle(fullScreen));
            channelSubView.setTextSize(fullScreen ? 9 : 9);
        }
    }

    private String currentChannelSubtitle(boolean fullscreen) {
        if (playlist.isEmpty()) return fullscreen ? "上下滑动换台" : "NO SIGNAL";
        TvChannel channel = playlist.get(index);
        String label = channel.title;
        if (channel.live) label += " · LIVE";
        else if (!channel.subtitle.isEmpty()) label += " · " + channel.subtitle;
        return fullscreen ? label + " · 上下滑动换台" : label;
    }

    private void applyTvFrameLayout() {
        setBackgroundColor(0xF20A0907);
        stage.setBackground(new CabinetDrawable(ui.dp(18)));
        int side = ui.dp(18);
        stage.setPadding(side, side, side, side);
        FrameLayout.LayoutParams stageParams = ui.frame(-1, -1, Gravity.CENTER);
        stageParams.leftMargin = ui.dp(18);
        stageParams.rightMargin = ui.dp(18);
        stageParams.topMargin = ui.dp(34);
        stageParams.bottomMargin = ui.dp(34);
        stage.setLayoutParams(stageParams);
        body.setPadding(ui.dp(18), ui.dp(16), ui.dp(18), ui.dp(14));
        header.setVisibility(VISIBLE);
        controls.setVisibility(VISIBLE);
        screen.setBackground(new ScreenDrawable(ui.dp(14)));
        int screenPad = ui.dp(10);
        screen.setPadding(screenPad, screenPad, screenPad, screenPad);
        LinearLayout.LayoutParams screenParams = new LinearLayout.LayoutParams(-1, 0, 1);
        screenParams.topMargin = ui.dp(4);
        screenParams.bottomMargin = ui.dp(12);
        screen.setLayoutParams(screenParams);
        scanlines.setVisibility(VISIBLE);
    }

    private void applyFullScreenLayout() {
        setBackgroundColor(Color.BLACK);
        stage.setBackgroundColor(Color.BLACK);
        stage.setPadding(0, 0, 0, 0);
        stage.setLayoutParams(ui.frame(-1, -1));
        body.setPadding(0, 0, 0, 0);
        header.setVisibility(GONE);
        controls.setVisibility(GONE);
        screen.setBackgroundColor(Color.BLACK);
        screen.setPadding(0, 0, 0, 0);
        screen.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1));
        scanlines.setVisibility(GONE);
    }

    private void removeStartupCallback() {
        if (startupRunnable == null) return;
        removeCallbacks(startupRunnable);
        startupRunnable = null;
    }

    private FrameLayout createStartupView(Context context) {
        FrameLayout view = new FrameLayout(context);
        view.setBackground(new StartupDrawable());
        ImageView logo = new ImageView(context);
        logo.setImageResource(R.drawable.mytv_ai_tv_startup);
        logo.setAdjustViewBounds(true);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        FrameLayout.LayoutParams logoParams = new FrameLayout.LayoutParams(ui.dp(260), ui.dp(260),
                Gravity.CENTER);
        view.addView(logo, logoParams);
        TextView boot = label("SIGNAL LOCKED  ·  AI TV", 12, 0xB8D9FFF3, true);
        boot.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams bootParams = new FrameLayout.LayoutParams(-1, ui.dp(34),
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        bootParams.leftMargin = ui.dp(18);
        bootParams.rightMargin = ui.dp(18);
        bootParams.bottomMargin = ui.dp(20);
        view.addView(boot, bootParams);
        return view;
    }

    private LinearLayout createChannelBug(Context context) {
        LinearLayout bug = new LinearLayout(context);
        bug.setOrientation(LinearLayout.VERTICAL);
        bug.setGravity(Gravity.CENTER);
        bug.setBackground(new ChannelBugDrawable(ui.dp(5)));
        channelNameView = label("AI TV", 14, 0xFFF8D985, true);
        channelNameView.setGravity(Gravity.CENTER);
        channelNameView.setIncludeFontPadding(false);
        channelSubView = label("NO SIGNAL", 9, 0xFF7EF6F0, true);
        channelSubView.setGravity(Gravity.CENTER);
        channelSubView.setIncludeFontPadding(false);
        bug.addView(channelNameView, new LinearLayout.LayoutParams(-1, 0, 1.25f));
        bug.addView(channelSubView, new LinearLayout.LayoutParams(-1, 0, 1));
        return bug;
    }

    private TextView label(String text, int sp, int color, boolean bold) {
        TextView view = ui.text(text, sp, color, bold ? 700 : 0);
        view.setSingleLine(false);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private TextView control(String text) {
        TextView view = label(text, 13, 0xFFFFF1CB, true);
        view.setGravity(Gravity.CENTER);
        view.setBackground(new ControlDrawable(ui.dp(5)));
        view.setClickable(true);
        return view;
    }

    private static String twoDigits(int value) {
        return value < 10 ? "0" + value : String.valueOf(value);
    }

    private static final class StartupDrawable extends android.graphics.drawable.Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        @Override public void draw(Canvas canvas) {
            RectF bounds = new RectF(getBounds());
            paint.setShader(new LinearGradient(0, bounds.top, 0, bounds.bottom,
                    0xFF020507, 0xFF15100B, Shader.TileMode.CLAMP));
            canvas.drawRect(bounds, paint);
            paint.setShader(null);
            paint.setColor(0x18FFFFFF);
            for (int y = 0; y < bounds.height(); y += 9) {
                canvas.drawRect(bounds.left, y, bounds.right, y + 2, paint);
            }
        }

        @Override public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override public void setColorFilter(android.graphics.ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
        }

        @Override public int getOpacity() {
            return android.graphics.PixelFormat.OPAQUE;
        }
    }

    private static final class CabinetDrawable extends android.graphics.drawable.Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int radius;

        CabinetDrawable(int radius) {
            this.radius = radius;
        }

        @Override public void draw(Canvas canvas) {
            RectF bounds = new RectF(getBounds());
            paint.setShader(new LinearGradient(0, bounds.top, 0, bounds.bottom,
                    0xFF4A291B, 0xFF1A100B, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(bounds, radius, radius, paint);
            paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(2, radius / 7f));
            paint.setColor(0xFFB47A3C);
            canvas.drawRoundRect(bounds.left + 3, bounds.top + 3,
                    bounds.right - 3, bounds.bottom - 3, radius, radius, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        @Override public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override public void setColorFilter(android.graphics.ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
        }

        @Override public int getOpacity() {
            return android.graphics.PixelFormat.OPAQUE;
        }
    }

    private static final class ScreenDrawable extends android.graphics.drawable.Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int radius;

        ScreenDrawable(int radius) {
            this.radius = radius;
        }

        @Override public void draw(Canvas canvas) {
            RectF bounds = new RectF(getBounds());
            paint.setColor(0xFF070A08);
            canvas.drawRoundRect(bounds, radius, radius, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(3, radius / 4f));
            paint.setColor(0xFF2A3429);
            canvas.drawRoundRect(bounds, radius, radius, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        @Override public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override public void setColorFilter(android.graphics.ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
        }

        @Override public int getOpacity() {
            return android.graphics.PixelFormat.OPAQUE;
        }
    }

    private static final class ChannelBugDrawable extends android.graphics.drawable.Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int radius;

        ChannelBugDrawable(int radius) {
            this.radius = radius;
        }

        @Override public void draw(Canvas canvas) {
            RectF bounds = new RectF(getBounds());
            paint.setColor(0xB8050708);
            canvas.drawRoundRect(bounds, radius, radius, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.5f);
            paint.setColor(0x99F6CE76);
            canvas.drawRoundRect(bounds, radius, radius, paint);
            paint.setStyle(Paint.Style.FILL);
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
    }

    private static final class ControlDrawable extends android.graphics.drawable.Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int radius;

        ControlDrawable(int radius) {
            this.radius = radius;
        }

        @Override public void draw(Canvas canvas) {
            RectF bounds = new RectF(getBounds());
            bounds.inset(4, 4);
            paint.setColor(0xFF241711);
            canvas.drawRoundRect(bounds, radius, radius, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);
            paint.setColor(0xFF9D6D36);
            canvas.drawRoundRect(bounds, radius, radius, paint);
            paint.setStyle(Paint.Style.FILL);
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
    }

    private static final class ScanlineView extends View {
        private final Paint paint = new Paint();

        ScanlineView(Context context) {
            super(context);
            setWillNotDraw(false);
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            paint.setColor(0x22000000);
            for (int y = 0; y < getHeight(); y += 6) {
                canvas.drawRect(0, y, getWidth(), y + 2, paint);
            }
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);
            paint.setColor(0x33FFFFFF);
            canvas.drawRoundRect(new RectF(1, 1, getWidth() - 1, getHeight() - 1), 14, 14, paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }
}
