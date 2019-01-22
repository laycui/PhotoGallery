package com.bignerdranch.android.photogallery.data;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;

import com.bignerdranch.android.photogallery.customview.LoadingImageView;

import java.io.IOException;
import java.io.InputStream;
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
  private ConcurrentMap<String, LoadingImageView> mUrl2Image;
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
      mUrl2Image.remove(mImage2Url.get(loadingImageView));
    }
    mImage2Url.put(loadingImageView, url);
    mUrl2Image.put(url, loadingImageView);
    if (mLruCache.get(url) != null) {
      bindImage(url);
      return;
    }
    downloadBitmap(url);
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
    mUrl2Image.get(url).setImageBitmap(mLruCache.get(url));
    mImage2Url.remove(mUrl2Image.get(url));
    mUrl2Image.remove(url);
  }
}
