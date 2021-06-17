package com.example.rtmp_demo.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;

import androidx.collection.LruCache;

import androidx.annotation.Nullable;
import androidx.core.graphics.BitmapCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ImageCache {

    private static final String TAG = "ImageCache";
    private static final int DEFAULT_MEM_CACHE_SIZE = 5 * 1024;
    private static final int DEFAULT_DISK_CACHE_SIZE = 1024 * 1024 * 10;
    private static final Bitmap.CompressFormat DEFAULT_COMPRESS_FORMAT = Bitmap.CompressFormat.JPEG;
    private static final int DEFAULT_COMPRESS_QUALITY = 70;
    private static final boolean DEFAULT_MEM_CACHE_ENABLE = true;
    private static final boolean DEFAULT_DISK_CACHE_ENABLE = true;
    private static final int DISK_CACHE_INDEX = 0;

    private ImageCacheParam imageCacheParam;
    private Set<SoftReference<Bitmap>> reusableBitmaps;
    private LruCache<String, BitmapDrawable> memCache;
    private final Object diskCacheLock = new Object();
    private DiskLruCache diskLruCache;
    private boolean diskCacheStarting = true;

    public static ImageCache getInstance(FragmentManager fragmentManager, ImageCacheParam imageCacheParam) {
        RetainFragment retainFragment = findOrCreateRetainFragment(fragmentManager);
        ImageCache imageCache = (ImageCache) retainFragment.getObject();
        if (imageCache == null) {
            imageCache = new ImageCache(imageCacheParam);
            retainFragment.setObject(imageCache);
        }
        return imageCache;
    }

    private ImageCache(ImageCacheParam imageCacheParam) {
        init(imageCacheParam);
    }

    public static File getDiskCacheDir(Context context, String uniqueName) {
        String path = Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
                !Environment.isExternalStorageRemovable() ?
                context.getExternalCacheDir().getPath() :
                context.getCacheDir().getPath();
        return new File(path + File.separator + uniqueName);
    }

    private void init(ImageCacheParam imageCacheParam) {
        this.imageCacheParam = imageCacheParam;
        if (imageCacheParam.memCacheEnable) {
            reusableBitmaps = Collections.synchronizedSet(new HashSet<>());
            memCache = new LruCache<String, BitmapDrawable>(imageCacheParam.memCacheSize) {
                @Override
                protected void entryRemoved(boolean evicted, String key, BitmapDrawable oldValue, BitmapDrawable newValue) {
                    if (oldValue instanceof RecycleBitmapDrawable) {
                        ((RecycleBitmapDrawable) oldValue).setIsCached(false);
                    } else {
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
        if (imageCacheParam.diskCacheEnable) {
            initDiskCache();
        }
    }

    void initDiskCache() {
        synchronized (diskCacheLock) {
            if (diskLruCache == null || diskLruCache.isClosed()) {
                File diskCacheDir = imageCacheParam.diskCacheDir;
                if (imageCacheParam.diskCacheEnable && diskCacheDir != null) {
                    if (!diskCacheDir.exists()) {
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
        if (retainFragment == null) {
            retainFragment = new RetainFragment();
            fragmentManager.beginTransaction().add(retainFragment, TAG).commitAllowingStateLoss();
        }

        return retainFragment;
    }

    public BitmapDrawable getBitmapFromMemCache(String data) {
        BitmapDrawable memDrawable = null;
        if (memCache != null) {
            memDrawable = memCache.get(data);
        }
        return memDrawable;
    }

    public Bitmap getBitmapFromDiskCache(String data) {
        String key = hasKeyForDisk(data);
        Bitmap bitmap = null;
        synchronized (diskCacheLock) {
            while (diskCacheStarting) {
                try {
                    diskCacheLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (diskLruCache != null) {
                InputStream inputStream = null;
                try {
                    DiskLruCache.Snapshot snapshot = diskLruCache.get(key);
                    if (snapshot != null) {
                        inputStream = snapshot.getInputStream(DISK_CACHE_INDEX);
                        if (inputStream != null) {
                            FileDescriptor fd = ((FileInputStream) inputStream).getFD();
                            bitmap = ImageResizer.decodeSampledBitmapFromDescriptor(fd, Integer.MAX_VALUE, Integer.MAX_VALUE, this);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return bitmap;
    }

    private String hasKeyForDisk(String data) {
        String cacheKey;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(data.getBytes());
            cacheKey = bytesToHexString(md5.digest());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            cacheKey = String.valueOf(data.hashCode());
        }
        return cacheKey;
    }

    private static String bytesToHexString(byte[] digest) {
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(b);
        }
        return sb.toString();
    }

    public Bitmap getBitmapFromReusableSet(BitmapFactory.Options options) {
        Bitmap bitmap = null;
        if (reusableBitmaps != null && !reusableBitmaps.isEmpty()) {
            synchronized (reusableBitmaps) {
                Iterator<SoftReference<Bitmap>> iterator = reusableBitmaps.iterator();
                Bitmap item;
                while (iterator.hasNext()) {
                    item = iterator.next().get();
                    if (item != null && item.isMutable()) {
                        if (canUseForInBitmap(item, options)) {
                            bitmap = item;
                            iterator.remove();
                            break;
                        }
                    } else {
                        iterator.remove();
                    }
                }
            }
        }
        return bitmap;
    }

    private boolean canUseForInBitmap(Bitmap bitmap, BitmapFactory.Options options) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return bitmap.getWidth() == options.outWidth && bitmap.getHeight() == options.outHeight && options.inSampleSize == 1;
        }
        int width = options.outWidth / options.inSampleSize;
        int height = options.outHeight / options.inSampleSize;
        int byteCount = width * height * getBytesPerPixel(bitmap.getConfig());
        return byteCount <= bitmap.getAllocationByteCount();
    }

    private int getBytesPerPixel(Bitmap.Config config) {
        if (config == Bitmap.Config.ARGB_8888) {
            return 4;
        } else if (config == Bitmap.Config.RGB_565) {
            return 2;
        } else if (config == Bitmap.Config.ARGB_4444) {
            return 2;
        } else if (config == Bitmap.Config.ALPHA_8) {
            return 1;
        }
        return 1;
    }

    public void addBitmapToCache(String data, BitmapDrawable bitmapDrawable) {
        if (data == null || bitmapDrawable == null) {
            return;
        }
        if (memCache != null) {
            if (bitmapDrawable instanceof RecycleBitmapDrawable) {
                ((RecycleBitmapDrawable) bitmapDrawable).setIsCached(true);
            }
            memCache.put(data, bitmapDrawable);
        }
        synchronized (diskCacheLock) {
            if (diskLruCache != null) {
                String key = hasKeyForDisk(data);
                OutputStream outputStream;
                try {
                    DiskLruCache.Snapshot snapshot = diskLruCache.get(key);
                    if (snapshot == null) {
                        DiskLruCache.Editor edit = diskLruCache.edit(key);
                        if (edit != null) {
                            outputStream = edit.newOutputStream(DISK_CACHE_INDEX);
                            bitmapDrawable.getBitmap().compress(imageCacheParam.compressFormat, imageCacheParam.compressQuality, outputStream);
                            edit.commit();
                            outputStream.close();
                        }
                    } else {
                        snapshot.getInputStream(DISK_CACHE_INDEX).close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void clearCache() {
        if (memCache != null) {
            memCache.evictAll();
        }
        synchronized (diskCacheLock) {
            if (diskCacheLock != null && !diskLruCache.isClosed()) {
                try {
                    diskLruCache.delete();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                diskLruCache = null;
                initDiskCache();
            }
        }
    }

    public void flush() {
        synchronized (diskCacheLock){
            if (diskLruCache != null){
                try {
                    diskLruCache.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void close() {
        synchronized (diskCacheLock){
            if (diskLruCache != null){
                if (!diskLruCache.isClosed()){
                    try {
                        diskLruCache.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
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

        public void setMemCacheSizePercent(float percent) {
            if (percent < 0.01f || percent > 0.8f) {
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
