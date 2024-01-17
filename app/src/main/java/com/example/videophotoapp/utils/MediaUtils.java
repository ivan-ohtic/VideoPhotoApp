package com.example.videophotoapp.utils;

import android.content.Context;
import android.graphics.BitmapFactory;

public class MediaUtils {

    public MediaUtils() {}

    /**
     * Calcula un factor de escala para reducir el tamaño de la imagen.
     *
     * @param options Opciones de BitmapFactory.
     * @param reqWidth Ancho requerido.
     * @param reqHeight Altura requerida.
     * @return Factor de escala.
     */
    public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

}
