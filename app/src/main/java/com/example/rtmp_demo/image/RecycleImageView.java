package com.example.rtmp_demo.image;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

public class RecycleImageView extends AppCompatImageView {
    public RecycleImageView(Context context) {
        super(context);
    }

    public RecycleImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RecycleImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDetachedFromWindow() {
        setImageDrawable(null);
        super.onDetachedFromWindow();
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        Drawable previousDrawable = getDrawable();
        super.setImageDrawable(drawable);
        notifyDrawable(drawable,true);
        notifyDrawable(previousDrawable,false);
    }

    private void notifyDrawable(Drawable drawable, boolean display) {
        if (drawable instanceof RecycleBitmapDrawable){
            ((RecycleBitmapDrawable) drawable).setIsDisplayed(display);
        }else if (drawable instanceof LayerDrawable){
            for (int i = 0; i < ((LayerDrawable) drawable).getNumberOfLayers(); i++) {
                Drawable layerDrawable = ((LayerDrawable) drawable).getDrawable(i);
                notifyDrawable(layerDrawable,display);
            }
        }
    }
}
