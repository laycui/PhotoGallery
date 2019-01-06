package com.bignerdranch.android.photogallery.data;

import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class FlickrFetchrV2 implements Callback<JsonElement> {

  private static final String BASE_URL = "https://api.flickr.com";
  private static final String API_KEY = "7f71cae4808f1bdb4ff0bd500d6e2b5a";
  private static final String FETCH_RECENTS_METHOD = "flickr.photos.getRecent";
  private static final String SEARCH_METHOD = "flickr.photos.search";
  private static final Gson GSON = new Gson();

  private OnReceiveGalleryItemsListener mOnReceiveGalleryItemsListener;
  private Call<JsonElement> mAsyncCall;

  public void start(@Nullable String query) {
    mAsyncCall = call(query);
    mAsyncCall.enqueue(this);
  }

  public List<GalleryItem> requestSync(@Nullable String query) {
    Call<JsonElement> call = call(query);
    try {
      Response<JsonElement> response = call.execute();
      return parseResponse(response);
    } catch (IOException ioe) {
      ioe.printStackTrace();
      return null;
    }
  }

  public void stop() {
    if (mAsyncCall != null && !mAsyncCall.isCanceled()) {
      mAsyncCall.cancel();
    }
  }

  public void setOnReceiveGalleryItemsListener(
      OnReceiveGalleryItemsListener onReceiveGalleryItemsListener) {
    mOnReceiveGalleryItemsListener = onReceiveGalleryItemsListener;
  }

  @Override
  public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
    mOnReceiveGalleryItemsListener.onGalleryItemsReceived(parseResponse(response));
  }

  @Override
  public void onFailure(Call<JsonElement> call, Throwable t) {
    t.printStackTrace();
  }

  private Call<JsonElement> call(@Nullable String query) {
    Gson gson = new GsonBuilder().setLenient().create();

    Retrofit retrofit =
        new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build();

    FlickrFetchrService flickrFetchrService = retrofit.create(FlickrFetchrService.class);

    Call<JsonElement> call;
    if (query == null) {
      call =
          flickrFetchrService.getPhotosMetadatas(
              API_KEY, "json", "1", "url_s", FETCH_RECENTS_METHOD, null);
    } else {
      call =
          flickrFetchrService.getPhotosMetadatas(
              API_KEY, "json", "1", "url_s", SEARCH_METHOD, query);
    }
    return call;
  }

  private List<GalleryItem> parseResponse(Response<JsonElement> response) {
    if (response == null || !response.isSuccessful()) {
      return null;
    }

    JsonElement jsonElement = response.body().getAsJsonObject().get("photos");
    GalleryItem.GalleryItemList galleryItemList =
        GSON.fromJson(jsonElement, GalleryItem.GalleryItemList.class);
    return galleryItemList.mItems;
  }

  public interface OnReceiveGalleryItemsListener {

    void onGalleryItemsReceived(List<GalleryItem> galleryItems);
  }
}
