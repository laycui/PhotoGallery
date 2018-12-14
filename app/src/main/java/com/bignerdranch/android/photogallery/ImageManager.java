package com.bignerdranch.android.photogallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.LruCache;

import com.bignerdranch.android.photogallery.PhotoGalleryFragment.PhotoHolder;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class ImageManager {

  private static final String TAG = "ImageManager";

  private Handler mRequestHandler;
  private Handler mUiHandler;
  private ConcurrentMap<PhotoHolder, String> mRequestMap;
  private Context mContext;
  private LruCache<String, Bitmap> mLruCache;

  ImageManager(Context context, LruCache<String, Bitmap> lruCache) {
    mRequestMap = new ConcurrentHashMap<>();
    mUiHandler = new Handler(Looper.getMainLooper());
    mContext = context;
    mLruCache = lruCache;

    HandlerThread handlerThread = new HandlerThread(TAG);
    handlerThread.start();
    mRequestHandler =
        new Handler(
            handlerThread.getLooper(),
            new Handler.Callback() {
              @Override
              public boolean handleMessage(Message message) {
                handleRequest((PhotoHolder) message.obj);
                return false;
              }
            });
  }

  void queueThumbnail(PhotoHolder photoHolder, String url) {
    mRequestMap.put(photoHolder, url);
    Message message = Message.obtain();
    message.obj = photoHolder;
    mRequestHandler.sendMessage(message);
  }

  private void handleRequest(final PhotoHolder target) {
    try {
      final String url = mRequestMap.get(target);
      if (url == null) {
        return;
      }
      byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
      final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
      Log.i(TAG, "Bitmap created");
      if (!url.equals(target.getUrl())) {
        Log.e("laycui", "stale URL");
        return;
      }
      mRequestMap.remove(target);
      mLruCache.put(url, bitmap);
      onPostExecution(target, bitmap);
    } catch (IOException ioe) {
      Log.e(TAG, "Error downloading image", ioe);
    }
  }

  private void onPostExecution(final PhotoHolder photoHolder, final Bitmap bitmap) {
    mUiHandler.post(
        new Runnable() {
          @Override
          public void run() {
            Drawable drawable = new BitmapDrawable(mContext.getResources(), bitmap);
            photoHolder.bindDrawable(drawable);
          }
        });
  }
}
