package com.example.rtmp_demo.channel;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.List;

public class CameraHelper implements Camera.PreviewCallback,SurfaceHolder.Callback {
    private static final String TAG = "CameraHelper";
    private final Activity activity;
    private int width;
    private int height;
    private int cameraId;
    private Camera camera;
    private int rotation;
    private Camera.PreviewCallback previewCallback;
    private OnChangedSizeListener onChangedSizeListener;
    private SurfaceHolder surfaceHolder;
    private byte[] buf;

    public CameraHelper(Activity activity, int width, int height, int cameraId){
        this.activity = activity;
        this.width = width;
        this.height = height;
        this.cameraId = cameraId;
    }

    public void setPreviewCallback(Camera.PreviewCallback previewCallback) {
        this.previewCallback = previewCallback;
    }

    public void setOnChangedSizeListener(OnChangedSizeListener onChangedSizeListener) {
        this.onChangedSizeListener = onChangedSizeListener;
    }

    public void switchCamera(int cameraId) {
        if (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK){
            this.cameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        }else {
            this.cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        stopPreview();
        startPreview();
    }

    private void startPreview(){
        Log.e(TAG, "startPreview: 开始预览" );
        camera = Camera.open(cameraId);
        Camera.Parameters parameters = camera.getParameters();
        parameters.setPreviewFormat(ImageFormat.NV21);
        setPreviewSize(parameters);
        setPreviewOrientation(parameters);
        camera.setParameters(parameters);
        buf = new byte[width * height * 3 / 2];
        //数据缓冲区
        camera.addCallbackBuffer(buf);
        camera.setPreviewCallbackWithBuffer(this);
        //设置预览画面
        try {
            camera.setPreviewDisplay(surfaceHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        onChangedSizeListener.onChanged(width,height);
        camera.startPreview();
    }

    private void setPreviewOrientation(Camera.Parameters parameters) {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId,cameraInfo);
        rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation){
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT){
            // TODO: 2021/4/4  不懂
            result = (cameraInfo.orientation + degrees) % 360;
            result = (360 - result) % 360;
        }else {
            result = (cameraInfo.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    private void setPreviewSize(Camera.Parameters parameters) {
        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        Camera.Size size = supportedPreviewSizes.get(0);
        int m = Math.abs(size.width * size.height - width * height);
        for (Camera.Size supportedPreviewSize : supportedPreviewSizes) {
            int n = Math.abs(size.width * size.height - width * height);
            if (n < m){
                size = supportedPreviewSize;
            }
        }
        width = size.width;
        height = size.height;
        parameters.setPreviewSize(width,height);
        Log.e(TAG, "setPreviewSize: width = " + width + "   height = " + height );
    }

    private void stopPreview(){
        if (camera != null){
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    public void setPreviewDisplay(SurfaceHolder surfaceHolder) {
        this.surfaceHolder = surfaceHolder;
        this.surfaceHolder.addCallback(this);
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        previewCallback.onPreviewFrame(data,camera);
        camera.addCallbackBuffer(buf);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        stopPreview();
        startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopPreview();
    }

    public interface OnChangedSizeListener{
        void onChanged(int width,int height);
    }
}
