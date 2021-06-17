package com.example.rtmp_demo.image.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.rtmp_demo.image.ImageFetcher;
import com.example.rtmp_demo.image.RecycleImageView;
import com.example.rtmp_demo.image.provider.Images;

public class ImageGridFragment extends Fragment {

    private int imageThumbSize;
    private int imageThumbSpacing;
    private ImageFetcher imageFetcher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    private class ImageAdapter extends BaseAdapter {

        private GridView.LayoutParams layoutParams;
        private int itemHeight;

        ImageAdapter() {
            layoutParams = new GridView.LayoutParams(GridView.LayoutParams.MATCH_PARENT, GridView.LayoutParams.WRAP_CONTENT);
        }

        @Override
        public int getCount() {
            return Images.imageUrls.length;
        }

        @Override
        public Object getItem(int position) {
            return Images.imageUrls[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {
                imageView = new RecycleImageView(parent.getContext());
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setLayoutParams(layoutParams);
            }else {
                imageView = (ImageView) convertView;
            }
            if (imageView.getLayoutParams().height != itemHeight){
                imageView.setLayoutParams(layoutParams);
            }

            return imageView;
        }
    }
}
