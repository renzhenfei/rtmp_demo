package com.example.rtmp_demo.image;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

import androidx.core.net.ConnectivityManagerCompat;

import java.io.File;

public class ImageFetcher extends ImageResizer {
    private static final String HTTP_CACHE_DIR = "http";
    private File httpCacheDir;

    public ImageFetcher(Context context, int imageWidth, int imageHeight) {
        super(context, imageWidth, imageHeight);
        init(context);
    }

    public ImageFetcher(Context context,int size){
        super(context,size);
        init(context);
    }
    private void init(Context context) {
        checkConnection(context);
        httpCacheDir = ImageCache.getDiskCacheDir(context,HTTP_CACHE_DIR);
    }

    @Override
    protected void initDiskCacheInternal() {
        super.initDiskCacheInternal();
        if (httpCacheDir.exists()){
            httpCacheDir.delete();
        }
        // TODO: 2021/6/17
    }

    private void checkConnection(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetworkInfo == null || !activeNetworkInfo.isConnectedOrConnecting()){
            Toast.makeText(context,"无网络",Toast.LENGTH_SHORT).show();
        }
    }
}
