package com.example.rtmp_demo.image;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.widget.ImageView;

import java.lang.ref.WeakReference;

public abstract class ImageWorker {

    private static final String TAG = "ImageWorker";
    private static final int FADE_IN_TIME = 200;
    private ImageCache imageCache;
    private ImageCache.ImageCacheParam imageCacheParam;
    private Bitmap loadingBitmap;
    private boolean fadeInBitmap = true;
    private boolean exitTaskEarly = false;
    private boolean pauseWork = false;
    private final Object pauseWorkLock = new Object();
    protected Resources resources;
    private static final int MESSAGE_CLEAR = 0;
    private static final int MESSAGE_INIT_DISK_CACHE = 1;
    private static final int MESSAGE_FLUSH = 2;
    private static final int MESSAGE_CLOSE = 3;

    public ImageWorker(Context context) {
        this.resources = context.getResources();
    }

    public void loadImage(Object data, ImageView imageView) {
        loadImage(data, imageView, null);
    }

    private void loadImage(Object data, ImageView imageView, OnImageLoadListener onImageLoadListener) {
        if (data == null) {
            return;
        }
        BitmapDrawable bitmapDrawable = null;
        if (imageCache != null) {
            bitmapDrawable = imageCache.getBitmapFromMemCache(String.valueOf(data));
        }
        if (bitmapDrawable != null) {
            imageView.setImageDrawable(bitmapDrawable);
            if (onImageLoadListener != null) {
                onImageLoadListener.onImageLoaded(true);
            }
        } else {
            if (cancelPotentialWork(data, imageView)) {
                BitmapWorkTask bitmapWorkTask = new BitmapWorkTask(data, imageView, onImageLoadListener);
                AsyncDrawable asyncDrawable = new AsyncDrawable(resources, loadingBitmap, bitmapWorkTask);
                imageView.setImageDrawable(asyncDrawable);
                bitmapWorkTask.executeOnExecutor(AsyncTask.DUAL_THREAD_EXECUTOR);
            }
        }
    }

    public static boolean cancelPotentialWork(Object data, ImageView imageView) {
        BitmapWorkTask bitmapWorkTask = getBitmapWorkTask(imageView);
        if (bitmapWorkTask != null) {
            Object bitmapData = bitmapWorkTask.data;
            if (bitmapData == null || !bitmapData.equals(data)) {
                bitmapWorkTask.cancel(true);
            } else {
                return false;
            }
        }
        return true;
    }

    public ImageCache getImageCache() {
        return imageCache;
    }

    private class BitmapWorkTask extends AsyncTask<Void, Void, BitmapDrawable> {
        private Object data;
        private WeakReference<ImageView> imageViewWeakReference;
        private OnImageLoadListener onImageLoadListener;

        public BitmapWorkTask(Object data, ImageView imageView) {
            this(data, imageView, null);
        }

        public BitmapWorkTask(Object data, ImageView imageView, OnImageLoadListener onImageLoadListener) {
            this.data = data;
            this.imageViewWeakReference = new WeakReference<>(imageView);
            this.onImageLoadListener = onImageLoadListener;
        }

        @Override
        protected BitmapDrawable doInBackground(Void... voids) {
            String data = String.valueOf(this.data);
            Bitmap bitmap = null;
            BitmapDrawable bitmapDrawable = null;

            synchronized (pauseWorkLock) {
                while (pauseWork && !isCancelled()) {
                    try {
                        pauseWorkLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (imageCache != null && !isCancelled() && getAttachedImageView() != null && !exitTaskEarly) {
                bitmap = imageCache.getBitmapFromDiskCache(data);
            }
            if (bitmap == null && !isCancelled() && getAttachedImageView() != null && !exitTaskEarly) {
                bitmap = processBitmap(data);
            }
            if (bitmap != null) {
                bitmapDrawable = new BitmapDrawable(resources, bitmap);
                if (imageCache != null) {
                    imageCache.addBitmapToCache(data, bitmapDrawable);
                }
            }

            return bitmapDrawable;
        }

        @Override
        protected void onPostExecute(BitmapDrawable bitmapDrawable) {
            boolean success = false;
            if (isCancelled() || exitTaskEarly) {
                bitmapDrawable = null;
            }
            ImageView imageView = getAttachedImageView();
            if (bitmapDrawable != null && imageView != null) {
                success = true;
                setImageDrawable(imageView, bitmapDrawable);
            }
            if (onImageLoadListener != null) {
                onImageLoadListener.onImageLoaded(success);
            }
        }

        private void setImageDrawable(ImageView imageView, BitmapDrawable bitmapDrawable) {
            if (fadeInBitmap) {
                TransitionDrawable transitionDrawable = new TransitionDrawable(new Drawable[]{new ColorDrawable(Color.TRANSPARENT), bitmapDrawable});
                imageView.setBackgroundDrawable(new BitmapDrawable(resources, loadingBitmap));
                imageView.setImageDrawable(transitionDrawable);
                transitionDrawable.startTransition(FADE_IN_TIME);
            } else {
                imageView.setImageDrawable(bitmapDrawable);
            }
        }

        @Override
        protected void onCancelled(BitmapDrawable bitmapDrawable) {
            synchronized (pauseWorkLock) {
                pauseWorkLock.notifyAll();
            }
        }

        private ImageView getAttachedImageView() {
            ImageView imageView = imageViewWeakReference.get();
            BitmapWorkTask bitmapWorkTask = getBitmapWorkTask(imageView);
            if (bitmapWorkTask == this) {
                return imageView;
            }
            return null;
        }
    }

    abstract Bitmap processBitmap(Object data);

    protected void initDiskCacheInternal() {
        if (imageCache != null) {
            imageCache.initDiskCache();
        }
    }

    protected void clearCacheInternal() {
        if (imageCache != null) {
            imageCache.clearCache();
        }
    }

    protected void flushCacheInternal() {
        if (imageCache != null) {
            imageCache.flush();
        }
    }

    protected void closeCacheInternal() {
        if (imageCache != null) {
            imageCache.close();
            imageCache = null;
        }
    }

    private static BitmapWorkTask getBitmapWorkTask(ImageView imageView) {
        if (imageView != null) {
            Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                return ((AsyncDrawable) drawable).getBitmapWorkTask();
            }
        }
        return null;
    }

    public interface OnImageLoadListener {
        void onImageLoaded(boolean success);
    }

    private class AsyncDrawable extends BitmapDrawable {

        private final WeakReference<BitmapWorkTask> bitmapWorkTaskWeakReference;

        public AsyncDrawable(Resources resources, Bitmap bitmap, BitmapWorkTask bitmapWorkTask) {
            super(resources, bitmap);
            bitmapWorkTaskWeakReference = new WeakReference<>(bitmapWorkTask);
        }

        public BitmapWorkTask getBitmapWorkTask() {
            return bitmapWorkTaskWeakReference.get();
        }
    }
}
