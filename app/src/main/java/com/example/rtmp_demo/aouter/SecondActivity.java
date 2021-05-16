package com.example.rtmp_demo.aouter;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;
import com.bumptech.glide.Glide;
import com.example.rtmp_demo.R;

import org.greenrobot.eventbus.EventBus;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Route(path = "/app/SecondActivity")
public class SecondActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

    }

    void testARouter(){
        ARouter.getInstance().build("/app/SecondActivity").navigation();
    }

    void testGlide(){
        ImageView img = findViewById(R.id.img);
        Glide.with(this).load("").into(img);
    }

    void testEventBus(){
        EventBus.getDefault().post(new Object());
    }

    void testRetrofit(){
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        API api = retrofit.create(API.class);

    }
}
