package com.example.rtmp_demo.channel;

import android.app.Activity;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;

import com.example.rtmp_demo.LivePusher;

public class VideoChannel implements IChannel, Camera.PreviewCallback,CameraHelper.OnChangedSizeListener {
    private static final String TAG = "VideoChannel";
    private final LivePusher livePusher;
    /**
     * 码率
     */
    private final int bitrate;
    private final int fps;
    private final CameraHelper cameraHelper;
    private boolean isLiving = false;

    public VideoChannel(LivePusher livePusher, Activity activity, int width, int height, int bitrate, int fps, int cameraId) {
        this.livePusher = livePusher;
        this.bitrate = bitrate;
        this.fps = fps;
        this.cameraHelper = new CameraHelper(activity, width, height, cameraId);
        this.cameraHelper.setPreviewCallback(this);
        this.cameraHelper.setOnChangedSizeListener(this);
    }

    public void setPreviewDisplay(SurfaceHolder surfaceHolder) {
        cameraHelper.setPreviewDisplay(surfaceHolder);
    }

    public void switchCamera(int cameraId) {
        cameraHelper.switchCamera(cameraId);
    }

    private void startPreview() {

    }

    private void stopPreview() {

    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (isLiving){
            Log.e(TAG, "onPreviewFrame: =======================" );
            livePusher.native_PushVideo(data);
        }
    }

    @Override
    public void startLive() {
        isLiving = true;
    }

    @Override
    public void stopLive() {
        isLiving = false;
    }

    @Override
    public void release() {

    }

    @Override
    public void onChanged(int width, int height) {
        Log.e(TAG, "onChanged: ----------------------" );
        livePusher.native_SetVideoEnvInfo(width,height,bitrate,fps);
    }
}
