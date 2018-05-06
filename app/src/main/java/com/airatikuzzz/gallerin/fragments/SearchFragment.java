package com.airatikuzzz.gallerin.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.airatikuzzz.gallerin.EndlessRecyclerOnScrollListener;
import com.airatikuzzz.gallerin.GalleryItem;
import com.airatikuzzz.gallerin.activities.PhotoDetailActivity;
import com.airatikuzzz.gallerin.R;
import com.airatikuzzz.gallerin.activities.UnsplashClient;
import com.bumptech.glide.Glide;
import com.kc.unsplash.Unsplash;
import com.kc.unsplash.models.Photo;
import com.kc.unsplash.models.SearchResults;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by maira on 12.07.2017.
 */

public class SearchFragment extends Fragment {
    private static final String QUERY_LAST = "last_query_gallerin";
    private static final int SPAN_COUNT_SINGLE = 1;
    private String mQuery;

    private static final int DEFAULT_PAGE_NUMBER = 1;

    private boolean isInstalledScrollManager = false;
    private boolean isSearching = false;

    private Toolbar mToolbar;
    private RecyclerView mRecyclerView;
    private GridLayoutManager mGridLayoutManager;

    private EndlessRecyclerOnScrollListener scrollListener;
    private SearchAdapter mAdapter;
    private List<GalleryItem> mItems = new ArrayList<>();
    private Unsplash unsplash;

    private SearchView searchView;

    public static SearchFragment newInstance(){
        return new SearchFragment();
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        unsplash = UnsplashClient.getCurrentUnsplash();
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.search_fragment_menu, menu);

        final MenuItem searchItem = menu.findItem(R.id.menu_search_item);
        searchView = (SearchView) searchItem.getActionView();

        searchView.setIconified(false);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if(!isSearching) {
                    isSearching = true;
                    mItems.clear();
                    EndlessRecyclerOnScrollListener.setCurrent_page(1);
                    mRecyclerView.removeOnScrollListener(scrollListener);
                    setupLayoutManager(SPAN_COUNT_SINGLE);
                    mRecyclerView.setAdapter(new LoadingAdapter());
                    mQuery = query;
                    loadData(DEFAULT_PAGE_NUMBER);
                    isInstalledScrollManager = false;
                    collapseSearchView();
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    private void collapseSearchView(){
        mToolbar.setTitle(mQuery);
        searchView.setIconified(true);
        searchView.onActionViewCollapsed();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mRecyclerView.removeOnScrollListener(scrollListener);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        
        View v = inflater.inflate(R.layout.fragment_search, container, false);
        mRecyclerView = v.findViewById(R.id.fragment_search_recycler_view);
        mToolbar = getActivity().findViewById(R.id.toolbar_search);

        EndlessRecyclerOnScrollListener.setCurrent_page(1);
        isInstalledScrollManager = false;
        
        if(savedInstanceState!=null){
            mQuery = savedInstanceState.getString(QUERY_LAST);
            collapseSearchView();
            setupAdapter();
            setupLayoutManager(getSpanCount());
        }
        else setupLayoutManager(SPAN_COUNT_SINGLE);

        return v;
    }

    //Определение необходимого количества столбцов для gridlayout
    private int getSpanCount(){
        float densityDpi = getResources().getDisplayMetrics().densityDpi;
        float displayWidth = getResources().getDisplayMetrics().widthPixels;
        float widthInDP = displayWidth/(densityDpi/160.0f);
        return (int)widthInDP/150;
    }

    private void onDataLoaded(){
        isSearching = false;
        if(mGridLayoutManager.getSpanCount()==SPAN_COUNT_SINGLE){       //Данные загружены, меняем manager
            setupLayoutManager(getSpanCount());
        }
        setupAdapter();
        setupScrollManager();
    }

    //Установка правильного менеджера:
    //SPAN_COUNT_SINGLE  -  ставится тогда, когда на экране висит прогрессбар
    //В остальных случаях, менеджер ставится с вычисляемым SPANCOUNT
    private void setupLayoutManager(int spanCount){
        mGridLayoutManager = new GridLayoutManager(getActivity(), spanCount);
        mRecyclerView.setLayoutManager(mGridLayoutManager);
    }
    
    private void setupScrollManager(){
        if(isInstalledScrollManager){        //Не стоит вешать несколько слушателей на recyclerview
            return;
        }
        isInstalledScrollManager = true;
        scrollListener = new EndlessRecyclerOnScrollListener(mGridLayoutManager) {
            @Override
            public void onLoadMore(int current_page) {
                loadData(current_page);
            }
        };
        mRecyclerView.addOnScrollListener(scrollListener);
    }

    private void setupAdapter() {
        if (mAdapter == null) {                                 //Загрузка данных произошла
            mAdapter = new SearchAdapter(mItems);     //Передача данных адаптеру
            mRecyclerView.setAdapter(mAdapter);             //Установка адаптера
            return;
        }
        mAdapter.setGalleryItems(mItems);                   //При скролле передаем новые данные адаптеру
        if (mRecyclerView.getAdapter() == null) {               //Возвращает TRUE при смене ориентации
            mRecyclerView.setAdapter(mAdapter);
            setupScrollManager();
            return;
        }
        if (mRecyclerView.getAdapter() instanceof LoadingAdapter) {
            mRecyclerView.setAdapter(mAdapter);
            return;
        }
        mAdapter.notifyDataSetChanged();
    }


    //Сохранение состояния при смене ориентации
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(QUERY_LAST, mQuery);
    }

    private void loadData(int page){
        searchPhotos(page, mQuery);
    }



    private void searchPhotos(int page, String query){
        unsplash.searchPhotos(query, page, 16, new Unsplash.OnSearchCompleteListener() {
            @Override
            public void onComplete(SearchResults results) {
                List<Photo> photos = results.getResults();
                mItems = convert(photos);
                onDataLoaded();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getActivity(), "Невозможно загрузить данные. Проверьте подключение к интернету", Toast.LENGTH_LONG).show();
            }
        });
    }

    //Конвертирование загруженных объектов в модель GalleryItem
    private List<GalleryItem> convert(List<Photo> list){
        for(int i = 0;i<list.size();i++){
            GalleryItem item = new GalleryItem();
            item.setCaption(list.get(i).getUser().getName());
            item.setId(list.get(i).getId());
            item.setOwner(list.get(i).getColor());
            item.setUrl(list.get(i).getUrls().getSmall());
            item.setUrlFull(list.get(i).getUrls().getRegular());
            mItems.add(item);
        }
        return mItems;
    }

    private class SearchHolder extends RecyclerView.ViewHolder {

        private final ImageView mImageView;
        private GalleryItem item;

        public SearchHolder(View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.fragment_photo_gallery_image_view);
        }

        public void bindGalleryItem(GalleryItem galleryItem){
            item = galleryItem;
        }
    }

    private class SearchAdapter extends RecyclerView.Adapter<SearchHolder>{

        public void setGalleryItems(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }
        private GalleryItem item;

        List<GalleryItem> mGalleryItems;

        public SearchAdapter(List<GalleryItem> items) {
            mGalleryItems = items;
        }

        @Override
        public SearchHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View v = inflater.inflate(R.layout.list_item_image_view, parent, false);
            return new SearchHolder(v);
        }

        @Override
        public void onBindViewHolder(final SearchHolder holder, int position) {
            holder.mImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent i = PhotoDetailActivity.newIntent(getActivity(), holder.item);
                    startActivity(i);
                }
            });
            item = mGalleryItems.get(position);
            holder.bindGalleryItem(item);
            Drawable placeHolder = new ColorDrawable(Color.parseColor(item.getOwner()));
            Glide.with(getActivity())
                    .load(item.getUrl())
                    .placeholder(placeHolder)
                    .into(holder.mImageView);
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }

    //Адаптер, устанавливаемый при начальной загрузке данных, представляет собой прогрессбар.
    private class LoadingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(R.layout.progress_bar, parent, false);
            return new RecyclerView.ViewHolder(view) {};
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        }
        @Override
        public int getItemCount() {
            return 1;
        }
    }

}

