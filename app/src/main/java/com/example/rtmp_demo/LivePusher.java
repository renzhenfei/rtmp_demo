package com.example.rtmp_demo;

import android.app.Activity;
import android.view.SurfaceHolder;

import com.example.rtmp_demo.channel.AudioChannel;
import com.example.rtmp_demo.channel.VideoChannel;

/**
 * 推流器
 */
public class LivePusher {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private final VideoChannel videoChannel;
    private final AudioChannel audioChannel;

    public LivePusher(Activity activity, int width, int height, int bitrate, int fps, int cameraId){
        native_init();
        videoChannel = new VideoChannel(this,activity, width, height, bitrate, fps, cameraId);
        audioChannel = new AudioChannel();
    }

    public void setPreviewDisplay(SurfaceHolder surfaceHolder){
        videoChannel.setPreviewDisplay(surfaceHolder);
    }

    public void switchCamera(int cameraId){
        videoChannel.switchCamera(cameraId);
    }

    public void startLive(String path){
        native_start(path);
        videoChannel.startLive();
        audioChannel.startLive();
    }

    public void stopLive(){
        videoChannel.stopLive();
        audioChannel.stopLive();
    }

    public native void native_init();

    public native void native_start(String path);

    public native void native_SetVideoEnvInfo(int width,int height,int bitrate,int fps);

    public native void native_PushVideo(byte[] bits);

    public native void native_Stop();

    private native void native_Release();

}
