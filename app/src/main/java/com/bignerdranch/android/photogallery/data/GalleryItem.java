package com.bignerdranch.android.photogallery.data;

import android.net.Uri;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class GalleryItem {
  private static final String FLICKR_URL = "http://www.flickr.com/photos/";

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
}
