package com.linkyun.her;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.MediaPlayer;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

import java.io.IOException;

final class AssetVideoAvatarView extends FrameLayout {
    private static final long CROSSFADE_MS = 180;
    private static final String STANDBY_ASSET = "standby.mp4";
    private static final String TALKING_ASSET = "talking.mp4";
    private static final int PHASE_GREETING = 0;
    private static final int PHASE_IDLE = 1;
    private static final int PHASE_SPEAKING = 2;

    private final VideoLayer greetingLayer;
    private final VideoLayer standbyLayer;
    private final VideoLayer talkingLayer;
    private boolean speaking = false;
    private int phase = PHASE_IDLE;
    private boolean forceIdleAfterGreeting = false;

    AssetVideoAvatarView(Context context) {
        this(context, null);
    }

    AssetVideoAvatarView(Context context, TabletDemoCharacter character) {
        this(context, character, 0);
    }

    AssetVideoAvatarView(Context context, TabletDemoCharacter character, int verticalOffsetPx) {
        super(context);
        setBackgroundColor(Color.BLACK);
        if (character == null) {
            phase = PHASE_IDLE;
            greetingLayer = new VideoLayer(context, STANDBY_ASSET, true, null, verticalOffsetPx);
            standbyLayer = new VideoLayer(context, STANDBY_ASSET, true, null, verticalOffsetPx);
            talkingLayer = new VideoLayer(context, TALKING_ASSET, true, null, verticalOffsetPx);
        } else {
            phase = PHASE_GREETING;
            greetingLayer = new VideoLayer(context, character.greetingAsset, false, this::onGreetingFinished, verticalOffsetPx);
            standbyLayer = new VideoLayer(context, character.idleAsset, true, null, verticalOffsetPx);
            talkingLayer = new VideoLayer(context, character.speakingAsset, true, null, verticalOffsetPx);
        }
        addView(greetingLayer.texture, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        addView(standbyLayer.texture, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        addView(talkingLayer.texture, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        greetingLayer.texture.setAlpha(phase == PHASE_GREETING ? 1f : 0f);
        standbyLayer.texture.setAlpha(phase == PHASE_IDLE ? 1f : 0f);
        talkingLayer.texture.setAlpha(0f);
    }

    void setSpeaking(boolean speaking) {
        if (this.speaking == speaking) return;
        this.speaking = speaking;
        applyVisibleLayer(true);
    }

    void replayGreetingThenIdle() {
        speaking = false;
        forceIdleAfterGreeting = true;
        phase = PHASE_GREETING;
        greetingLayer.restartIfReady();
        applyVisibleLayer(true);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        greetingLayer.startIfReady();
        standbyLayer.startIfReady();
        talkingLayer.startIfReady();
        applyVisibleLayer(false);
    }

    @Override
    protected void onDetachedFromWindow() {
        greetingLayer.release();
        standbyLayer.release();
        talkingLayer.release();
        super.onDetachedFromWindow();
    }

    private void applyVisibleLayer(boolean animate) {
        phase = speaking ? PHASE_SPEAKING : (phase == PHASE_GREETING ? PHASE_GREETING : PHASE_IDLE);
        VideoLayer visibleLayer = visibleLayer();
        View visible = visibleLayer.texture;
        standbyLayer.startIfReady();
        talkingLayer.startIfReady();
        if (phase == PHASE_GREETING) greetingLayer.startIfReady();
        visible.bringToFront();
        greetingLayer.texture.animate().cancel();
        standbyLayer.texture.animate().cancel();
        talkingLayer.texture.animate().cancel();
        if (animate) {
            visible.animate().alpha(1f).setDuration(CROSSFADE_MS).start();
            fadeHiddenLayers(visibleLayer);
        } else {
            greetingLayer.texture.setAlpha(visibleLayer == greetingLayer ? 1f : 0f);
            standbyLayer.texture.setAlpha(visibleLayer == standbyLayer ? 1f : 0f);
            talkingLayer.texture.setAlpha(visibleLayer == talkingLayer ? 1f : 0f);
        }
    }

    private void fadeHiddenLayers(VideoLayer visibleLayer) {
        if (visibleLayer != greetingLayer) greetingLayer.texture.animate().alpha(0f).setDuration(CROSSFADE_MS).start();
        if (visibleLayer != standbyLayer) standbyLayer.texture.animate().alpha(0f).setDuration(CROSSFADE_MS).start();
        if (visibleLayer != talkingLayer) talkingLayer.texture.animate().alpha(0f).setDuration(CROSSFADE_MS).start();
    }

    private VideoLayer visibleLayer() {
        if (phase == PHASE_GREETING) return greetingLayer;
        if (phase == PHASE_SPEAKING) return talkingLayer;
        return standbyLayer;
    }

    private void onGreetingFinished() {
        if (phase != PHASE_GREETING) return;
        if (forceIdleAfterGreeting) {
            forceIdleAfterGreeting = false;
            speaking = false;
            phase = PHASE_IDLE;
        } else {
            phase = speaking ? PHASE_SPEAKING : PHASE_IDLE;
        }
        applyVisibleLayer(true);
    }

    private static final class VideoLayer implements TextureView.SurfaceTextureListener {
        final TextureView texture;
        private final Context context;
        private final String assetName;
        private final boolean looping;
        private final Runnable completion;
        private final int verticalOffsetPx;
        private MediaPlayer player;
        private Surface surface;
        private boolean surfaceReady = false;
        private int videoWidth = 0;
        private int videoHeight = 0;

        VideoLayer(Context context, String assetName, boolean looping, Runnable completion, int verticalOffsetPx) {
            this.context = context;
            this.assetName = assetName;
            this.looping = looping;
            this.completion = completion;
            this.verticalOffsetPx = verticalOffsetPx;
            texture = new TextureView(context);
            texture.setSurfaceTextureListener(this);
        }

        void startIfReady() {
            if (!surfaceReady) return;
            if (player == null) preparePlayer();
            if (player != null && !player.isPlaying()) player.start();
        }

        void restartIfReady() {
            if (!surfaceReady) return;
            if (player == null) preparePlayer();
            if (player == null) return;
            try {
                player.seekTo(0);
                player.start();
            } catch (RuntimeException ignored) { }
        }

        void release() {
            if (player != null) {
                player.release();
                player = null;
            }
            if (surface != null) {
                surface.release();
                surface = null;
            }
            surfaceReady = false;
        }

        @Override
        public void onSurfaceTextureAvailable(android.graphics.SurfaceTexture surfaceTexture, int width, int height) {
            surface = new Surface(surfaceTexture);
            surfaceReady = true;
            startIfReady();
        }

        @Override
        public void onSurfaceTextureSizeChanged(android.graphics.SurfaceTexture surfaceTexture, int width, int height) {
            updateTransform();
        }

        @Override
        public boolean onSurfaceTextureDestroyed(android.graphics.SurfaceTexture surfaceTexture) {
            release();
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(android.graphics.SurfaceTexture surfaceTexture) {
        }

        private void preparePlayer() {
            MediaPlayer nextPlayer = new MediaPlayer();
            try (AssetFileDescriptor descriptor = context.getAssets().openFd(assetName)) {
                nextPlayer.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
                nextPlayer.setSurface(surface);
                nextPlayer.setLooping(looping);
                nextPlayer.setVolume(0f, 0f);
                nextPlayer.setOnVideoSizeChangedListener((mp, width, height) -> {
                    videoWidth = width;
                    videoHeight = height;
                    updateTransform();
                });
                if (completion != null) {
                    nextPlayer.setOnCompletionListener(mp -> completion.run());
                }
                nextPlayer.prepare();
                player = nextPlayer;
            } catch (IOException | RuntimeException ignored) {
                nextPlayer.release();
            }
        }

        private void updateTransform() {
            int viewWidth = texture.getWidth();
            int viewHeight = texture.getHeight();
            if (viewWidth <= 0 || viewHeight <= 0 || videoWidth <= 0 || videoHeight <= 0) return;
            float scale = Math.max((float) viewWidth / videoWidth, (float) viewHeight / videoHeight);
            float scaledWidth = videoWidth * scale;
            float scaledHeight = videoHeight * scale;
            Matrix matrix = new Matrix();
            matrix.setScale(scaledWidth / viewWidth, scaledHeight / viewHeight, viewWidth / 2f, viewHeight / 2f);
            if (verticalOffsetPx != 0) {
                matrix.postTranslate(0, verticalOffsetPx);
            }
            texture.setTransform(matrix);
        }
    }
}
