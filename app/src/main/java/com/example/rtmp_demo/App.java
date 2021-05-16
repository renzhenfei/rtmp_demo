package com.example.rtmp_demo;

import android.app.Application;

import com.alibaba.android.arouter.launcher.ARouter;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ARouter.init(this);
    }
}
