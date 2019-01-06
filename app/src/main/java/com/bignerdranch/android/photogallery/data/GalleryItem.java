package com.bignerdranch.android.photogallery.data;


import android.net.Uri;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GalleryItem {
  private static String FLICKR_URL = "http://www.flickr.com/photos/";

  @SerializedName("title")
  @Expose
  public String mCaption;

  @SerializedName("id")
  @Expose
  public String mId;

  @SerializedName("url_s")
  @Expose
  public String mUrl;

  @SerializedName("owner")
  @Expose
  public String mOwner;

  @Override
  public String toString() {
    return mCaption;
  }

  public void setId(String id) {
    mId = id;
  }

  public String getId() {
    return mId;
  }

  public String getUrl() {
    return mUrl;
  }

  public Uri getPhotoPageUri() {
    return Uri.parse(FLICKR_URL).buildUpon().appendPath(mOwner).appendPath(mId).build();
  }

  static class GalleryItemList {

    @SerializedName("photo")
    @Expose
    List<GalleryItem> mItems;
  }
}
