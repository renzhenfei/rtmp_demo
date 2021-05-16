package com.example.rtmp_demo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private LivePusher livePusher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.INTERNET,Manifest.permission.CAMERA},10000);
        SurfaceView sv = findViewById(R.id.sv);

        livePusher = new LivePusher(this, 800, 480, 800000, 10, Camera.CameraInfo.CAMERA_FACING_BACK);
        livePusher.setPreviewDisplay(sv.getHolder());
    }

    public void startLive(View view) {
        livePusher.startLive("rtmp://192.168.0.101:8899/live/huyalive");
    }
}