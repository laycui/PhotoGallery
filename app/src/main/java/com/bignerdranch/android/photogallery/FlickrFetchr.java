package com.bignerdranch.android.photogallery;


import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FlickrFetchr {
  private static final String TAG = "FlickrFetchr";

  private static final String API_KEY = "7f71cae4808f1bdb4ff0bd500d6e2b5a";
  private static final String FETCH_RECENTS_METHOD = "flickr.photos.getRecent";
  private static final String SEARCH_METHOD = "flickr.photos.search";
  private static final Uri ENDPOINT = Uri.parse("https://api.flickr.com/services/rest/")
          .buildUpon()
          .appendQueryParameter("api_key", API_KEY)
          .appendQueryParameter("format", "json")
          .appendQueryParameter("nojsoncallback", "1")
          .appendQueryParameter("extras", "url_s")
          .build();

  public byte[] getUrlBytes(String urlSpec) throws IOException {
    URL url = new URL(urlSpec);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();

    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      InputStream in = connection.getInputStream();

      if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
        throw new IOException(connection.getResponseMessage() + ": with " + urlSpec);
      }

      int bytesRead;
      byte[] buffer = new byte[1024];
      while ((bytesRead = in.read(buffer)) > 0) {
        out.write(buffer, 0, bytesRead);
      }
      out.close();
      return out.toByteArray();
    } finally {
      connection.disconnect();
    }
  }

  public String getUrlString(String urlSpec) throws IOException {
    return new String(getUrlBytes(urlSpec));
  }

  public List<GalleryItem> fetchRecentPhotos() {
    String url = buildUrl(FETCH_RECENTS_METHOD, null);
    return downloadGalleryItems(url);
  }

  public List<GalleryItem> searchPhotos(String query) {
    String url = buildUrl(SEARCH_METHOD, query);
    return downloadGalleryItems(url);
  }

  public List<GalleryItem> downloadGalleryItems(String url) {
    List<GalleryItem> items = new ArrayList<>();

    try {
      String jsonString = getUrlString(url);
      JSONObject jsonBody = new JSONObject(jsonString);
      parseItems(items, jsonBody);
      Log.i(TAG, "Received JSON: " + jsonString);
    } catch (JSONException je) {
      Log.e(TAG, "Failed to parse JSON", je);
    } catch (IOException ioe) {
      Log.e(TAG, "Failed to fetch items", ioe);
    }
    return items;
  }

  private void parseItems(List<GalleryItem> items, JSONObject jsonBody)
          throws IOException, JSONException {
    JSONObject photosJsonObject = jsonBody.getJSONObject("photos");
    JSONArray photoJsonArray = photosJsonObject.getJSONArray("photo");
    for (int i = 0; i < photoJsonArray.length(); i++) {
      JSONObject photoJsonObject = photoJsonArray.getJSONObject(i);

      GalleryItem item = new GalleryItem();
      item.setId(photoJsonObject.getString("id"));
      item.setCaption(photoJsonObject.getString("title"));
      if (!photoJsonObject.has("url_s")) {
        continue;
      }
      item.setUrl(photoJsonObject.getString("url_s"));
      items.add(item);
    }
  }

  private String buildUrl(String method, String query) {
    Uri.Builder uriBuilder = ENDPOINT.buildUpon().appendQueryParameter("method", method);
    if (method.equals(SEARCH_METHOD)) {
      uriBuilder.appendQueryParameter("text", query);
    }
    return uriBuilder.build().toString();
  }
}
