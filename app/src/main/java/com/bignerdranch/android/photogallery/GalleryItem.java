package com.bignerdranch.android.photogallery;


import android.net.Uri;

public class GalleryItem {
  private static String FLICKR_URL = "http://www.flickr.com/photos/";

  private String mCaption;
  private String mId;
  private String mUrl;
  private String mOwner;

  @Override
  public String toString() {
    return mCaption;
  }

  public void setCaption(String caption) {
    mCaption = caption;
  }

  public void setId(String id) {
    mId = id;
  }

  public void setUrl(String url) {
    mUrl = url;
  }

  public String getCaption() {
    return mCaption;
  }

  public String getId() {
    return mId;
  }

  public String getUrl() {
    return mUrl;
  }

  public String getOwner() {
    return mOwner;
  }

  public void setOwner(String owner) {
    mOwner = owner;
  }

  public Uri getPhotoPageUri() {
    return Uri.parse(FLICKR_URL).buildUpon().appendPath(mOwner).appendPath(mId).build();
  }
}
