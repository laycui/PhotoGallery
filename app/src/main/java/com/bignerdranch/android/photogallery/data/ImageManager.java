package com.bignerdranch.android.photogallery.data;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.WorkerThread;
import android.util.LruCache;

import com.bignerdranch.android.photogallery.customview.LoadingImageView;

import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ImageManager {

  private static final String TAG = "ImageManager";

  private Handler mRequestHandler;
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

  public ImageManager() {
    mUrl2Image = new ConcurrentHashMap<>();
    mImage2Url = new ConcurrentHashMap<>();
    mUiHandler = new Handler(Looper.getMainLooper());
    mLruCache = new LruCache<>(100);
    mOkHttpClient = new OkHttpClient();
    HandlerThread handlerThread = new HandlerThread(TAG);
    handlerThread.start();
    mRequestHandler =
        new Handler(
            handlerThread.getLooper(),
            new Handler.Callback() {
              @Override
              public boolean handleMessage(Message message) {
                handleRequest((String) message.obj);
                return false;
              }
            });
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
      loadingImageView.setImageBitmap(mLruCache.get(url));
      mImage2Url.remove(loadingImageView);
      mUrl2Image.remove(url);
      return;
    }
    httpRequest(url);
  }

  private void httpRequest(String url) {
    Message message = Message.obtain();
    message.obj = url;
    mRequestHandler.sendMessage(message);
  }

  private void handleRequest(final String url) {
    final Bitmap bitmap = downloadBitMap(url);

    mLruCache.put(url, bitmap);

    mUiHandler.post(
        new Runnable() {
          @Override
          public void run() {
            if (mUrl2Image.get(url) != null) {
              mUrl2Image.get(url).setImageBitmap(bitmap);
              mImage2Url.remove(mUrl2Image.get(url));
              mUrl2Image.remove(url);
            }
          }
        });
  }

  @WorkerThread
  private Bitmap downloadBitMap(String url) {
    Request request = new Request.Builder().url(url).build();
    try {
      Response response = mOkHttpClient.newCall(request).execute();
      InputStream inputStream = response.body().byteStream();
      return BitmapFactory.decodeStream(inputStream);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return null;
  }
}
