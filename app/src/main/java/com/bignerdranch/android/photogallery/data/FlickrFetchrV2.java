package com.bignerdranch.android.photogallery.data;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class FlickrFetchrV2 {

  private static final String BASE_URL = "https://api.flickr.com";
  private static final String API_KEY = "7f71cae4808f1bdb4ff0bd500d6e2b5a";
  private static final String FETCH_RECENTS_METHOD = "flickr.photos.getRecent";
  private static final String SEARCH_METHOD = "flickr.photos.search";

  private OnReceiveGalleryItemsListener mOnReceiveGalleryItemsListener;
  private Call<GalleryResponseBody> mAsyncCall;

  public void start(@Nullable String query) {
    mAsyncCall = call(query);
    mAsyncCall.enqueue(
        new Callback<GalleryResponseBody>() {
          @Override
          public void onResponse(
              @NonNull Call<GalleryResponseBody> call,
              @NonNull Response<GalleryResponseBody> response) {
            if (mOnReceiveGalleryItemsListener == null || response.body() == null) {
              return;
            }
            mOnReceiveGalleryItemsListener.onGalleryItemsReceived(response.body().mData.mItems);
          }

          @Override
          public void onFailure(@NonNull Call<GalleryResponseBody> call, @NonNull Throwable t) {
            t.printStackTrace();
          }
        });
  }

  @WorkerThread
  public List<GalleryItem> requestSync(@Nullable String query) {
    Call<GalleryResponseBody> call = call(query);
    try {
      Response<GalleryResponseBody> response = call.execute();
      if (response.body() == null) {
        return null;
      }
      return response.body().mData.mItems;
    } catch (IOException ioe) {
      ioe.printStackTrace();
      return null;
    }
  }

  public void stop() {
    if (mAsyncCall != null && !mAsyncCall.isCanceled()) {
      mAsyncCall.cancel();
    }
    mOnReceiveGalleryItemsListener = null;
  }

  public void setOnReceiveGalleryItemsListener(
      OnReceiveGalleryItemsListener onReceiveGalleryItemsListener) {
    mOnReceiveGalleryItemsListener = onReceiveGalleryItemsListener;
  }

  private Call<GalleryResponseBody> call(@Nullable String query) {
    Gson gson = new GsonBuilder().setLenient().create();

    Retrofit retrofit =
        new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build();

    FlickrFetchrService flickrFetchrService = retrofit.create(FlickrFetchrService.class);

    String method = query == null ? FETCH_RECENTS_METHOD : SEARCH_METHOD;
    return flickrFetchrService.getPhotosMetadatas(API_KEY, "json", "1", "url_s", method, null);
  }

  public interface OnReceiveGalleryItemsListener {

    void onGalleryItemsReceived(List<GalleryItem> galleryItems);
  }
}
