package com.example.rtmp_demo.image;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

public class RecycleBitmapDrawable extends BitmapDrawable {

    private static final String TAG = "RecycleBitmapDrawable";
    private int cacheRefCount;
    private int displayRefCount;
    private boolean hasBeenDisplayed;

    RecycleBitmapDrawable(Resources resources, Bitmap bitmap){
        super(resources,bitmap);
    }

    public void setIsDisplayed(boolean isDisplayed){
        synchronized (this){
            if (isDisplayed){
                displayRefCount++;
                hasBeenDisplayed = true;
            }else {
                displayRefCount--;
            }
        }
        checkState();
    }

    public void setIsCached(boolean isCached){
        synchronized (this){
            if (isCached){
                cacheRefCount++;
            }else {
                cacheRefCount--;
            }
        }
        checkState();
    }

    private void checkState() {
        if (cacheRefCount <=0 && displayRefCount <= 0 && hasBeenDisplayed && hasValidBitmap()){
            getBitmap().recycle();
        }
    }

    private boolean hasValidBitmap() {
        Bitmap bitmap = getBitmap();
        return bitmap != null && !bitmap.isRecycled();
    }

}
