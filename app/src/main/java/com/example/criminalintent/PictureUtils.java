package com.example.criminalintent;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public final class PictureUtils {
    private PictureUtils() {
    }

    public static Bitmap getScaledBitmap(String path, int destWidth, int destHeight) {
        if (path == null || destWidth <= 0 || destHeight <= 0) {
            return null;
        }

        BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
        boundsOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, boundsOptions);

        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
            return null;
        }

        int sampleSize = 1;
        if (boundsOptions.outHeight > destHeight || boundsOptions.outWidth > destWidth) {
            int heightScale = Math.round((float) boundsOptions.outHeight / (float) destHeight);
            int widthScale = Math.round((float) boundsOptions.outWidth / (float) destWidth);
            sampleSize = Math.max(1, Math.max(heightScale, widthScale));
        }

        BitmapFactory.Options scaledOptions = new BitmapFactory.Options();
        scaledOptions.inSampleSize = sampleSize;
        return BitmapFactory.decodeFile(path, scaledOptions);
    }
}
