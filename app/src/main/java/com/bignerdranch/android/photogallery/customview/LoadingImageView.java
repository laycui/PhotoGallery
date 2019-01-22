package com.bignerdranch.android.photogallery.customview;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.bignerdranch.android.photogallery.R;

public class LoadingImageView extends FrameLayout {

  private ImageView mImageView;
  private ProgressBar mProgressBar;

  public LoadingImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);

    LayoutInflater.from(context).inflate(R.layout.view_loading_image, this);
    mImageView = findViewById(R.id.image_view);
    mProgressBar = findViewById(R.id.progress_circular);

    reset();
  }

  public void setImageBitmap(Bitmap bitmap) {
    mImageView.setImageBitmap(bitmap);

    mImageView.setVisibility(VISIBLE);
    mProgressBar.setVisibility(GONE);
  }

  public void reset() {
    mImageView.setVisibility(GONE);
    mProgressBar.setVisibility(VISIBLE);
  }
}
