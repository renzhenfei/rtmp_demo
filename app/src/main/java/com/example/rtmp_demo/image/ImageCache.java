package com.example.rtmp_demo.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.util.LruCache;

import androidx.annotation.Nullable;
import androidx.core.graphics.BitmapCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ImageCache {

    private static final String TAG = "ImageCache";
    private static final int DEFAULT_MEM_CACHE_SIZE = 5 * 1024;
    private static final int DEFAULT_DISK_CACHE_SIZE = 1024 * 1024 * 10;
    private static final Bitmap.CompressFormat DEFAULT_COMPRESS_FORMAT = Bitmap.CompressFormat.JPEG;
    private static final int DEFAULT_COMPRESS_QUALITY = 70;
    private static final boolean DEFAULT_MEM_CACHE_ENABLE = true;
    private static final boolean DEFAULT_DISK_CACHE_ENABLE = true;

    private ImageCacheParam imageCacheParam;
    private Set<SoftReference<Bitmap>> reusableBitmaps;
    private LruCache<String, BitmapDrawable> memCache;
    private final Object diskCacheLock = new Object();
    private DiskLruCache diskLruCache;

    public static ImageCache getInstance(FragmentManager fragmentManager,ImageCacheParam imageCacheParam) {
        RetainFragment retainFragment = findOrCreateRetainFragment(fragmentManager);
        ImageCache imageCache = (ImageCache) retainFragment.getObject();
        if (imageCache == null){
            imageCache = new ImageCache(imageCacheParam);
            retainFragment.setObject(imageCache);
        }
        return imageCache;
    }

    private ImageCache(ImageCacheParam imageCacheParam){
        init(imageCacheParam);
    }

    private void init(ImageCacheParam imageCacheParam) {
        this.imageCacheParam = imageCacheParam;
        if (imageCacheParam.memCacheEnable){
            reusableBitmaps = Collections.synchronizedSet(new HashSet<>());
            memCache = new LruCache<String, BitmapDrawable>(imageCacheParam.memCacheSize){
                @Override
                protected void entryRemoved(boolean evicted, String key, BitmapDrawable oldValue, BitmapDrawable newValue) {
                    if (oldValue instanceof RecycleBitmapDrawable){
                        ((RecycleBitmapDrawable) oldValue).setIsCached(false);
                    }else {
                        reusableBitmaps.add(new SoftReference<>(oldValue.getBitmap()));
                    }
                }

                @Override
                protected int sizeOf(String key, BitmapDrawable value) {
                    int size = BitmapCompat.getAllocationByteCount(value.getBitmap()) / 1024;
                    return size == 0 ? 1 : size;
                }
            };
        }
        if (imageCacheParam.diskCacheEnable){
            initDiskCache();
        }
    }

    private void initDiskCache() {
        synchronized (diskCacheLock){
            if (diskLruCache == null || diskLruCache.isClosed()){
                File diskCacheDir = imageCacheParam.diskCacheDir;
                if (imageCacheParam.diskCacheEnable && diskCacheDir != null){
                    if (!diskCacheDir.exists()){
                        diskCacheDir.mkdirs();
                    }
                    try {
                        diskLruCache = DiskLruCache.open(diskCacheDir, 1, 1, imageCacheParam.diskCacheSize);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private static RetainFragment findOrCreateRetainFragment(FragmentManager fragmentManager) {
        RetainFragment retainFragment = (RetainFragment) fragmentManager.findFragmentByTag(TAG);
        if (retainFragment == null){
            retainFragment = new RetainFragment();
            fragmentManager.beginTransaction().add(retainFragment,TAG).commitAllowingStateLoss();
        }

        return retainFragment;
    }


    public static class ImageCacheParam {
        public int memCacheSize = DEFAULT_MEM_CACHE_SIZE;
        public int diskCacheSize = DEFAULT_DISK_CACHE_SIZE;
        public File diskCacheDir;
        public Bitmap.CompressFormat compressFormat = DEFAULT_COMPRESS_FORMAT;
        public int compressQuality = DEFAULT_COMPRESS_QUALITY;
        public boolean memCacheEnable = DEFAULT_MEM_CACHE_ENABLE;
        public boolean diskCacheEnable = DEFAULT_DISK_CACHE_ENABLE;

        ImageCacheParam(Context context, String diskCacheDirectoryName) {
            diskCacheDir = getDiskCacheDir(context, diskCacheDirectoryName);
        }

        private File getDiskCacheDir(Context context, String uniqueName) {
            String cachePath = Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
                    !Environment.isExternalStorageRemovable() ? context.getExternalCacheDir().getPath() :
                    context.getCacheDir().getPath();
            return new File(cachePath + File.separator + uniqueName);
        }

        public void setMemCacheSizePercent(float percent){
            if (percent < 0.01f || percent > 0.8f){
                throw new IllegalStateException("setMemCacheSizePercent percent must be between 0.01 and 0.8");
            }
            memCacheSize = Math.round(Runtime.getRuntime().maxMemory() * percent / 1024);
        }
    }

    public static class RetainFragment extends Fragment {
        private Object object;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }

        public void setObject(Object object) {
            this.object = object;
        }

        public Object getObject() {
            return object;
        }
    }

}
