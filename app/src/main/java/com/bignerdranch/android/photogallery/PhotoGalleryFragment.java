package com.bignerdranch.android.photogallery;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

public class PhotoGalleryFragment extends VisibleFragment {

  private static final String TAG = "PhotoGalleryFragment";

  private RecyclerView mPhotoRecyclerView;
  private PhotoAdapter mPhotoAdapter;
  private List<GalleryItem> mItems = new ArrayList<>();
  private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;
  private ImageManager mImageManager;
  private FetchItemsTask mFetchItemsTask;

  private LruCache<String, Bitmap> mLruCache;

  public static PhotoGalleryFragment newInstance() {
    return new PhotoGalleryFragment();
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setRetainInstance(true);
    setHasOptionsMenu(true);

    mLruCache = new LruCache<>(100);
    Handler responseHandler = new Handler();
    mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
    mThumbnailDownloader.setThumbnailDownloadListener(
            new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
      @Override
      public void onThumbnailDownloaded(PhotoHolder photoHolder, Bitmap bitmap) {
        Drawable drawable = new BitmapDrawable(getResources(), bitmap);
        photoHolder.bindDrawable(drawable);
      }
    });
    mThumbnailDownloader.start();
    mThumbnailDownloader.getLooper();
    mImageManager = new ImageManager(getContext(), mLruCache);
    Log.i(TAG, "Background thread destroyed");
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
          savedInstanceState) {
    View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
    mPhotoRecyclerView = (RecyclerView) v.findViewById(R.id.fragment_photo_gallery_recycler_view);
    mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));
    mPhotoAdapter = new PhotoAdapter(mItems);
    mPhotoRecyclerView.setAdapter(mPhotoAdapter);
    mFetchItemsTask = new FetchItemsTask(mItems, mPhotoAdapter);

    updateItems();
    return v;
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
    super.onCreateOptionsMenu(menu, menuInflater);
    menuInflater.inflate(R.menu.fragment_photo_gallery, menu);

    MenuItem searchItem = menu.findItem(R.id.menu_item_search);
    final SearchView searchView = (SearchView) searchItem.getActionView();

    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
      @Override
      public boolean onQueryTextSubmit(String s) {
        Log.d(TAG, "QueryTextSubmit: " + s);
        QueryPreferences.setStoredQuery(getActivity(), s);
        updateItems();
        return true;
      }

      @Override
      public boolean onQueryTextChange(String s) {
        Log.d(TAG, "QueryTextChange: " + s);
        return false;
      }
    });

    searchView.setOnSearchClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        String query = QueryPreferences.getStoredQuery(getActivity());
        searchView.setQuery(query, false);
      }
    });

    MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
    if (PollService.isServiceAlarmOn(getActivity())) {
      toggleItem.setTitle(R.string.stop_polling);
    } else {
      toggleItem.setTitle(R.string.start_polling);
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_item_clear:
        QueryPreferences.setStoredQuery(getActivity(), null);
        updateItems();
        return true;
      case R.id.menu_item_toggle_polling:
        boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
        PollService.setServiceAlarm(getActivity(), shouldStartAlarm);
        getActivity().invalidateOptionsMenu();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private void updateItems() {
    String query = QueryPreferences.getStoredQuery(getActivity());
    mFetchItemsTask.execute(query);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    mThumbnailDownloader.quit();
    Log.i(TAG, "Background thread destroyed");
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    mFetchItemsTask.cancel(true);
    mThumbnailDownloader.clearQueue();
  }

  private static class FetchItemsTask extends AsyncTask<String, Void, List<GalleryItem>> {

    private List<GalleryItem> mItems;
    private PhotoAdapter mPhotoAdapter;

    FetchItemsTask(List<GalleryItem> items, PhotoAdapter photoAdapter) {
      mItems = items;
      mPhotoAdapter = photoAdapter;
    }

    @Override
    protected List<GalleryItem> doInBackground(String... strings) {
      String query = strings[0];
      if (query == null) {
        return new FlickrFetchr().fetchRecentPhotos();
      } else {
        return new FlickrFetchr().searchPhotos(query);
      }
    }

    @Override
    protected void onPostExecute(List<GalleryItem> galleryItems) {
      mItems.clear();
      mItems.addAll(galleryItems);
      mPhotoAdapter.notifyDataSetChanged();
    }
  }

  private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {
    private List<GalleryItem> mGalleryItems;
    public PhotoAdapter(List<GalleryItem> galleryItems) {
      mGalleryItems = galleryItems;
    }

    @Override
    public PhotoHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
      LayoutInflater inflater = LayoutInflater.from(getActivity());
      View view = inflater.inflate(R.layout.gallery_item, viewGroup, false);
      return new PhotoHolder(view, getContext());
    }

    @Override
    public void onBindViewHolder(PhotoHolder photoHolder, int position) {
      GalleryItem galleryItem = mGalleryItems.get(position);
      photoHolder.bindGalleryItem(galleryItem);
      Bitmap bitmap = mLruCache.get(galleryItem.getUrl());
      if (bitmap != null) {
        photoHolder.bindDrawable(new BitmapDrawable(getResources(), bitmap));
        return;
      }
      photoHolder.bindDrawable(getResources().getDrawable(R.mipmap.ic_launcher));
//      Picasso.get().load(galleryItem.getUrl()).into(photoHolder.mItemImageView);
//      mThumbnailDownloader.queueThumbnail(photoHolder, galleryItem.getUrl());
      mImageManager.queueThumbnail(photoHolder, galleryItem.getUrl());
    }

    @Override
    public int getItemCount() {
      return mGalleryItems.size();
    }
  }

  static class PhotoHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    private ImageView mItemImageView;
    private GalleryItem mGalleryItem;
    private Context mContext;

    PhotoHolder(View itemView, Context context) {
      super(itemView);
      mContext = context;
      mItemImageView = (ImageView) itemView.findViewById(R.id.fragment_photo_gallery_item_view);
      itemView.setOnClickListener(this);
    }

    public void bindDrawable(Drawable drawable) {
      mItemImageView.setImageDrawable(drawable);
    }

    public void bindGalleryItem(GalleryItem galleryItem) {
      mGalleryItem = galleryItem;
    }

    String getUrl() {
      return mGalleryItem.getUrl();
    }

    @Override
    public void onClick(View v) {
      Intent i = PhotoPageActivity.newIntent(mContext, mGalleryItem.getPhotoPageUri());
      mContext.startActivity(i);
    }
  }
}
