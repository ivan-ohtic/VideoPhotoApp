package com.example.videophotoapp.utils;

import android.content.Context;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import java.io.File;

public class MediaUtils {

    public static void configureImageView(int x, int y, int width, int height, View imageView, Context context) {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                convertDpToPixel(width, context),
                convertDpToPixel(height, context));
        params.leftMargin = convertDpToPixel(x, context);
        params.topMargin = convertDpToPixel(y, context);
        imageView.setLayoutParams(params);
    }

    public static void configurePlayerView(int x, int y, int width, int height, View playerView, Context context) {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                convertDpToPixel(width, context),
                convertDpToPixel(height, context));
        params.leftMargin = convertDpToPixel(x, context);
        params.topMargin = convertDpToPixel(y, context);
        playerView.setLayoutParams(params);
    }

    public static int convertDpToPixel(float dp, Context context) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    public static void playVideo(String videoName, PlayerView playerView, ExoPlayer player) {
        MediaItem mediaItem = MediaItem.fromUri(String.valueOf(new File(playerView.getContext().getFilesDir(), videoName).toURI()));
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
    }
}
