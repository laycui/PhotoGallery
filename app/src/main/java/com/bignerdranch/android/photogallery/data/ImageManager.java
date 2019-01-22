package com.bignerdranch.android.photogallery.data;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;

import com.bignerdranch.android.photogallery.customview.LoadingImageView;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ImageManager {

  private Handler mUiHandler;
  private OkHttpClient mOkHttpClient;
  private ConcurrentMap<String, Set<LoadingImageView>> mUrl2Image;
  private ConcurrentMap<LoadingImageView, String> mImage2Url;
  private LruCache<String, Bitmap> mLruCache;

  private static ImageManager sImageManager;

  public static ImageManager getInstance() {
    if (sImageManager == null) {
      sImageManager = new ImageManager();
    }
    return sImageManager;
  }

  private ImageManager() {
    mUrl2Image = new ConcurrentHashMap<>();
    mImage2Url = new ConcurrentHashMap<>();
    mUiHandler = new Handler(Looper.getMainLooper());
    mLruCache = new LruCache<>(100);
    mOkHttpClient = new OkHttpClient();
  }

  public void bindImageWithUrl(String url, LoadingImageView loadingImageView) {
    if (url == null) {
      return;
    }
    // URL to set of ImageViews
    if (mImage2Url.containsKey(loadingImageView)) {
      String oldUrl = mImage2Url.get(loadingImageView);
      if (mUrl2Image.containsKey(oldUrl)) {
        mUrl2Image.get(oldUrl).remove(loadingImageView);
      }
      mImage2Url.remove(loadingImageView);
    }
    if (mLruCache.get(url) != null) {
      loadingImageView.setImageBitmap(mLruCache.get(url));
      return;
    }
    mImage2Url.put(loadingImageView, url);
    if (!mUrl2Image.containsKey(url)) {
      mUrl2Image.put(url, new HashSet<LoadingImageView>());
      mUrl2Image.get(url).add(loadingImageView);
      downloadBitmap(url);
    } else {
      mUrl2Image.get(url).add(loadingImageView);
    }
  }

  private void downloadBitmap(final String url) {
    Request request = new Request.Builder().url(url).build();
    Callback callback =
        new Callback() {
          @Override
          public void onFailure(Call call, IOException e) {
            e.printStackTrace();
          }

          @Override
          public void onResponse(Call call, Response response) {
            InputStream inputStream = response.body().byteStream();
            final Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            mLruCache.put(url, bitmap);
            mUiHandler.post(
                new Runnable() {
                  @Override
                  public void run() {
                    if (mUrl2Image.get(url) != null) {
                      bindImage(url);
                    }
                  }
                });
          }
        };
    mOkHttpClient.newCall(request).enqueue(callback);
  }

  private void bindImage(String url) {
    if (!mUrl2Image.containsKey(url)) {
      return;
    }
    for (LoadingImageView imageView : mUrl2Image.get(url)) {
      imageView.setImageBitmap(mLruCache.get(url));
      mImage2Url.remove(imageView);
    }
    mUrl2Image.remove(url);
  }
}
