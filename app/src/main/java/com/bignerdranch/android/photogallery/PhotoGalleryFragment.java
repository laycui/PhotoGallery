package com.bignerdranch.android.photogallery;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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

import com.bignerdranch.android.photogallery.data.FlickrFetchrV2;
import com.bignerdranch.android.photogallery.data.GalleryItem;
import com.bignerdranch.android.photogallery.data.ImageManager;

import java.util.ArrayList;
import java.util.List;

public class PhotoGalleryFragment extends VisibleFragment {

  private static final String TAG = "PhotoGalleryFragment";

  private RecyclerView mPhotoRecyclerView;
  private PhotoAdapter mPhotoAdapter;
  private List<GalleryItem> mItems = new ArrayList<>();
  private ImageManager mImageManager;
  private FlickrFetchrV2 mFlickrFetchrV2;

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
    mImageManager = new ImageManager(getContext(), mLruCache);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
    mPhotoRecyclerView = v.findViewById(R.id.fragment_photo_gallery_recycler_view);
    mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));
    mPhotoAdapter = new PhotoAdapter(mItems);
    mPhotoRecyclerView.setAdapter(mPhotoAdapter);

    mFlickrFetchrV2 = new FlickrFetchrV2();
    mFlickrFetchrV2.setOnReceiveGalleryItemsListener(
        new FlickrFetchrV2.OnReceiveGalleryItemsListener() {
          @Override
          public void onGalleryItemsReceived(List<GalleryItem> galleryItems) {
            mItems.clear();
            mItems.addAll(galleryItems);
            mPhotoAdapter.notifyDataSetChanged();
          }
        });

    updateItems();
    return v;
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
    super.onCreateOptionsMenu(menu, menuInflater);
    menuInflater.inflate(R.menu.fragment_photo_gallery, menu);

    MenuItem searchItem = menu.findItem(R.id.menu_item_search);
    final SearchView searchView = (SearchView) searchItem.getActionView();

    searchView.setOnQueryTextListener(
        new SearchView.OnQueryTextListener() {
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

    searchView.setOnSearchClickListener(
        new View.OnClickListener() {
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
    mFlickrFetchrV2.start(query);
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    mFlickrFetchrV2.stop();
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
      if (galleryItem.getUrl() == null) {
        return;
      }
      Bitmap bitmap = mLruCache.get(galleryItem.getUrl());
      if (bitmap != null) {
        photoHolder.bindDrawable(new BitmapDrawable(getResources(), bitmap));
        return;
      }
      photoHolder.bindDrawable(getResources().getDrawable(R.mipmap.ic_launcher));
      mImageManager.queueThumbnail(photoHolder, galleryItem.getUrl());
    }

    @Override
    public int getItemCount() {
      return mGalleryItems.size();
    }
  }

  public static class PhotoHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    private ImageView mItemImageView;
    private GalleryItem mGalleryItem;
    private Context mContext;

    PhotoHolder(View itemView, Context context) {
      super(itemView);
      mContext = context;
      mItemImageView = itemView.findViewById(R.id.fragment_photo_gallery_item_view);
      itemView.setOnClickListener(this);
    }

    public void bindDrawable(Drawable drawable) {
      mItemImageView.setImageDrawable(drawable);
    }

    public void bindGalleryItem(GalleryItem galleryItem) {
      mGalleryItem = galleryItem;
    }

    public String getUrl() {
      return mGalleryItem.getUrl();
    }

    @Override
    public void onClick(View v) {
      Intent i = PhotoPageActivity.newIntent(mContext, mGalleryItem.getPhotoPageUri());
      mContext.startActivity(i);
    }
  }
}
