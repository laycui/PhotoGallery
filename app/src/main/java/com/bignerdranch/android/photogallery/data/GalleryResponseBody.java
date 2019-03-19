package com.bignerdranch.android.photogallery.data;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

class GalleryResponseBody {

  @SerializedName("photos")
  @Expose
  Data mData;

  static class Data {

    @SerializedName("photo")
    @Expose
    List<GalleryItem> mItems;
  }
}
