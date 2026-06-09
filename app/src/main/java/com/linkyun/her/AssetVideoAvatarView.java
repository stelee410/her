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

    private final VideoLayer standbyLayer;
    private final VideoLayer talkingLayer;
    private boolean speaking = false;

    AssetVideoAvatarView(Context context) {
        super(context);
        setBackgroundColor(Color.BLACK);
        standbyLayer = new VideoLayer(context, STANDBY_ASSET);
        talkingLayer = new VideoLayer(context, TALKING_ASSET);
        addView(standbyLayer.texture, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        addView(talkingLayer.texture, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        standbyLayer.texture.setAlpha(1f);
        talkingLayer.texture.setAlpha(0f);
    }

    void setSpeaking(boolean speaking) {
        if (this.speaking == speaking) return;
        this.speaking = speaking;
        applyVisibleLayer(true);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        standbyLayer.startIfReady();
        talkingLayer.startIfReady();
        applyVisibleLayer(false);
    }

    @Override
    protected void onDetachedFromWindow() {
        standbyLayer.release();
        talkingLayer.release();
        super.onDetachedFromWindow();
    }

    private void applyVisibleLayer(boolean animate) {
        View visible = speaking ? talkingLayer.texture : standbyLayer.texture;
        View hidden = speaking ? standbyLayer.texture : talkingLayer.texture;
        standbyLayer.startIfReady();
        talkingLayer.startIfReady();
        visible.bringToFront();
        visible.animate().cancel();
        hidden.animate().cancel();
        if (animate) {
            visible.animate().alpha(1f).setDuration(CROSSFADE_MS).start();
            hidden.animate().alpha(0f).setDuration(CROSSFADE_MS).start();
        } else {
            visible.setAlpha(1f);
            hidden.setAlpha(0f);
        }
    }

    private static final class VideoLayer implements TextureView.SurfaceTextureListener {
        final TextureView texture;
        private final Context context;
        private final String assetName;
        private MediaPlayer player;
        private Surface surface;
        private boolean surfaceReady = false;
        private int videoWidth = 0;
        private int videoHeight = 0;

        VideoLayer(Context context, String assetName) {
            this.context = context;
            this.assetName = assetName;
            texture = new TextureView(context);
            texture.setSurfaceTextureListener(this);
        }

        void startIfReady() {
            if (!surfaceReady) return;
            if (player == null) preparePlayer();
            if (player != null && !player.isPlaying()) player.start();
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
                nextPlayer.setLooping(true);
                nextPlayer.setVolume(0f, 0f);
                nextPlayer.setOnVideoSizeChangedListener((mp, width, height) -> {
                    videoWidth = width;
                    videoHeight = height;
                    updateTransform();
                });
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
            texture.setTransform(matrix);
        }
    }
}
