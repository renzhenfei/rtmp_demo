package com.example.rtmp_demo.image;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.FileDescriptor;

public class ImageResizer extends ImageWorker {

    private static final String TAG = "ImageResizer";

    protected int imageWidth;
    protected int imageHeight;

    public ImageResizer(Context context,int imageWidth,int imageHeight){
        super(context);
        setImageSize(imageWidth,imageHeight);
    }

    public ImageResizer(Context context,int size){
        super(context);
        setImageSize(size,size);
    }

    private void setImageSize(int imageWidth, int imageHeight) {
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
    }

    public void setSize(int size){
        setImageSize(size,size);
    }

    private Bitmap processBitmap(int resId) {
        return decodeSampledBitmapFromResource(resources,resId,imageWidth,imageHeight,getImageCache());
    }

    private Bitmap decodeSampledBitmapFromResource(Resources resources, int resId, int imageWidth, int imageHeight, ImageCache imageCache) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(resources,resId,options);
        options.inSampleSize = calculateInSampleSize(options,imageWidth,imageHeight);
        options.inJustDecodeBounds = false;
        addInBitmapOptions(options,imageCache);
        return BitmapFactory.decodeResource(resources,resId,options);
    }

    @Override
    Bitmap processBitmap(Object data) {
        return processBitmap(Integer.parseInt(String.valueOf(data)));
    }

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
        BitmapFactory.decodeFileDescriptor(fd, null, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        addInBitmapOptions(options, imageCache);
        return null;
    }

    private static void addInBitmapOptions(BitmapFactory.Options options, ImageCache imageCache) {
        options.inMutable = true;
        if (imageCache != null) {
            Bitmap inBitmap = imageCache.getBitmapFromReusableSet(options);
            if (inBitmap != null) {
                options.inBitmap = inBitmap;
            }
        }
    }
}
