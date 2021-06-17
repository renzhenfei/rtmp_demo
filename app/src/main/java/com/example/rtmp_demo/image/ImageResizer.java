package com.example.rtmp_demo.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.FileDescriptor;

public class ImageResizer {

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int outWidth = options.outWidth;
        int outHeight = options.outHeight;
        int sampleSize = 1;
        if (outWidth > reqWidth || outHeight > reqHeight) {
            long ws = Math.round(outWidth * 1.0 / reqWidth);
            long hs = Math.round(outHeight * 1.0 / reqHeight);
            sampleSize = (int) Math.max(ws, hs);
        }
        return sampleSize;
    }

    public static Bitmap decodeSampledBitmapFromDescriptor(FileDescriptor fd, int reqWidth, int reqHeight, ImageCache imageCache) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fd,null,options);
        options.inSampleSize = calculateInSampleSize(options,reqWidth,reqHeight);
        options.inJustDecodeBounds = false;
        addInBitmapOptions(options,imageCache);
        return null;
    }

    private static void addInBitmapOptions(BitmapFactory.Options options, ImageCache imageCache) {
        options.inMutable = true;
        if (imageCache != null){
            Bitmap inBitmap = imageCache.getBitmapFromReusableSet(options);
            if (inBitmap != null){
                options.inBitmap = inBitmap;
            }
        }
    }
}
