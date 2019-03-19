package com.bignerdranch.android.photogallery.data;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface FlickrFetchrService {

  @GET("services/rest/")
  Call<GalleryResponseBody> getPhotosMetadatas(
      @Query("api_key") String apiKey,
      @Query("format") String format,
      @Query("nojsoncallback") String nojsoncallback,
      @Query("extras") String extras,
      @Query("method") String method,
      @Query("text") String query);
}
