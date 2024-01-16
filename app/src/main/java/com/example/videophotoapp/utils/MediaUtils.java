package com.example.videophotoapp.utils;


import android.util.DisplayMetrics;


public class MediaUtils {

    public static int convertDpToPixel(float dp, DisplayMetrics displayMetrics) {
        return (int) (dp * displayMetrics.density);
    }

}
