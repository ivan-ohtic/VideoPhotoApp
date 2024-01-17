package com.example.videophotoapp.utils;


import android.util.DisplayMetrics;


public class MediaUtils {

    public int convertDpToPixel(float dp, DisplayMetrics displayMetrics) {
        return (int) (dp * displayMetrics.density);
    }

}
