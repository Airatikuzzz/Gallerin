package com.airatikuzzz.gallerin.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
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
import android.widget.ProgressBar;
import android.widget.Toast;

import com.airatikuzzz.gallerin.EndlessRecyclerOnScrollListener;
import com.airatikuzzz.gallerin.GalleryItem;
import com.airatikuzzz.gallerin.Method;
import com.airatikuzzz.gallerin.activities.PhotoDetailActivity;
import com.airatikuzzz.gallerin.QueryPreferences;
import com.airatikuzzz.gallerin.R;
import com.airatikuzzz.gallerin.activities.SearchActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.kc.unsplash.Unsplash;
import com.kc.unsplash.api.Order;
import com.kc.unsplash.models.Photo;
import com.kc.unsplash.models.SearchResults;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by maira on 12.07.2017.
 */

public class PhotoGalleryFragment extends Fragment {
    private Order order;
    private Method mMethod;
    private String mQuery;


    public static final int DEFAULT_PAGE_NUMBER = 1;
    public static final int DEFAULT_CODE = 0;
    public static final int CODE_REFRESH = 1;
    private static final String RECYCLER_STATE = "receycler_state_gallerin";
    private static final String TAG = "PhotoGalleryFragment";
    private static final String ARG_ORDER = "PhotoGalleryFragment_arg_order";
    private static final String ARG_METHOD = "PhotoGalleryFragment_arg_method";

    boolean isRefreshing = false;
    boolean isInstalledScrollManager = false;

    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout swipeLayout;
    private GridLayoutManager mGridLayoutManager;

    private EndlessRecyclerOnScrollListener scrollListener;
    private Parcelable recyclerViewState;
    private PhotoGalleryAdapter mAdapter;
    private List<GalleryItem> mItems = new ArrayList<>();
    public static LruCache<String, Bitmap> mMemoryCache;
    Unsplash unsplash;

    public static PhotoGalleryFragment newInstance(Order order_, Method method){
        PhotoGalleryFragment fragment = new PhotoGalleryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ORDER, order_.getOrder());
        args.putString(ARG_METHOD, method.getValue());
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        order = Order.valueOf(getArguments().getString(ARG_ORDER).toUpperCase());
        mMethod = Method.valueOf(getArguments().getString(ARG_METHOD));

        if(savedInstanceState!=null)        //Возвращает TRUE при смене ориентации
            recyclerViewState = savedInstanceState.getParcelable(RECYCLER_STATE);

        unsplash = new Unsplash("ebe8195594bd221b1c86bb55d0224f1c439b41d0aa4d3736ba7bda6e57dcfde5");

        if(mMethod.equals(Method.LIST_PHOTOS)) loadData(DEFAULT_PAGE_NUMBER);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_galleryy, menu);

        /*final MenuItem searchItem = (MenuItem) menu.findItem(R.id.menu_search_item);
        final SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "query is "+ query);
                loadData(DEFAULT_PAGE_NUMBER, query);
                searchView.setVisibility(View.INVISIBLE);
                searchView.setVisibility(View.VISIBLE);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG, " text changed "+newText);
                return false;
            }
        });
        searchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), SearchActivity.class));
            }
        });*/
    }


    private void updateItems() {
        String query = QueryPreferences.getStoredQuery(getActivity());
        Log.d(TAG, "in updateitems taken query = " + query);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mRecyclerView.removeOnScrollListener(scrollListener);
        Log.d(TAG, "Background Thread destroyed ");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        mRecyclerView = v.findViewById(R.id.fragment_photo_gallery_recycler_view);

        setupLayoutManager();
        setupAdapter();
        swipeLayout = v.findViewById(R.id.swipeContainer);


        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                isRefreshing = true;
                EndlessRecyclerOnScrollListener.setCurrent_page(0);
                scrollListener.setPreviousTotal(0);
                refresh();
            }
        });

        swipeLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        return v;
    }
    //Определение необходимого количества столбцов для gridlayout
    private int getSpanCount(){
        float densityDpi = getResources().getDisplayMetrics().densityDpi;
        float displayWidth = getResources().getDisplayMetrics().widthPixels;
        float widthInDP = displayWidth/(densityDpi/160.0f);
        Log.d(TAG, " widthindp"+widthInDP);
        int spanCount = (int)widthInDP/150;
        return spanCount;
    }

    private void onDataLoaded(){
        if(mGridLayoutManager.getSpanCount()==1){       //Данные загружены, меняем адаптер
            mGridLayoutManager = new GridLayoutManager(getActivity(), getSpanCount());
            mRecyclerView.setLayoutManager(mGridLayoutManager);
        }
        setupAdapter();
        setupScrollManager();
    }

    //Установка правильного менеджера:
    //при изначальной загрузке нужен spancount=1, потому что ставим LoaderFragment c прогрессбаром

    private void setupLayoutManager(){
        if(mAdapter==null) {                //при смене ориентации условие игнорируется
            mGridLayoutManager = new GridLayoutManager(getActivity(), 1);
            mRecyclerView.setLayoutManager(mGridLayoutManager);
            return;
        }
        mGridLayoutManager = new GridLayoutManager(getActivity(), getSpanCount());
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
                Log.d("spl", "scrolling current page " + current_page);
                if (isRefreshing) {
                    return;
                }
                loadData(current_page);
            }
        };
        mRecyclerView.addOnScrollListener(scrollListener);
    }

    private void setupAdapter() {
        Log.d("kek", "in setup "+ mItems.size());
        if(mItems.size() == 0){                                 //Установка адаптера с прогрессбаром при начальной загрузке
            mRecyclerView.setAdapter(new LoadingAdapter());
            Log.d("kek", "load adapter inst");
        }
        else{
            if(mAdapter==null){                                 //Загрузка данных произошла
                Log.d("kek", "gallery adapter inst");
                mAdapter = new PhotoGalleryAdapter(mItems);     //Передача данных адаптеру
                mRecyclerView.setAdapter(mAdapter);             //Установка адаптера
                return;
            }
            mAdapter.setGalleryItems(mItems);                   //При скролле передаем новые данные адаптеру
            if(mRecyclerView.getAdapter()==null){               //Возвращает TRUE при смене ориентации
                mRecyclerView.setAdapter(mAdapter);
                mRecyclerView.getLayoutManager().onRestoreInstanceState(recyclerViewState);
                setupScrollManager();
                return;
            }
            mAdapter.notifyDataSetChanged();                     //Обновление адаптера
        }
    }


    //Сохранение состояния при смене ориентации
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(RECYCLER_STATE, mRecyclerView.getLayoutManager().onSaveInstanceState());
    }

    private void loadData(int page){
        loadListPhotos(page);
    }
    private void loadData(int page, String query){
        searchPhotos(page, query);
    }

    private void loadListPhotos(int page){
        unsplash.getPhotos(page, 16, order, new Unsplash.OnPhotosLoadedListener() {
            @Override
            public void onComplete(List<Photo> photos) {
                mItems = convert(photos);
                onDataLoaded();
                if (!swipeLayout.isRefreshing()) {
                    return;
                }
                swipeLayout.setRefreshing(false);
                isRefreshing = false;
            }

            @Override
            public void onError(String error) {
                try {
                    Toast.makeText(getActivity(), "Невозможно загрузить данные. Проверьте подключение к интернету", Toast.LENGTH_LONG).show();
                }
                catch (NullPointerException n){
                    Toast.makeText(getActivity(), "Произошла ошибка. Попробуйте перезайти в приложение.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void searchPhotos(int page, String query){
        unsplash.searchPhotos(query, page, 16, new Unsplash.OnSearchCompleteListener() {
            @Override
            public void onComplete(SearchResults results) {
                Log.d("Photos", "Total Results Found " + results.getTotal());
                List<Photo> photos = results.getResults();
                mItems = convert(photos);
                onDataLoaded();
                if (!swipeLayout.isRefreshing()) {
                    return;
                }
                swipeLayout.setRefreshing(false);
                isRefreshing = false;
            }

            @Override
            public void onError(String error) {
                Log.d("Unsplash", error);
                Toast.makeText(getActivity(), "Невозможно загрузить данные. Проверьте подключение к интернету", Toast.LENGTH_LONG).show();
            }
        });
    }

    //Обновление фрагмента свайпом сверху вниз
    private void refresh(){
        mAdapter.mGalleryItems.clear();
        mAdapter.notifyDataSetChanged();
        loadData(DEFAULT_PAGE_NUMBER);
    }

    //Конвертирование загруженных объектов в модель GalleryItem
    public List<GalleryItem> convert(List<Photo> list){
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
    private class PhotoGalleryHolder extends RecyclerView.ViewHolder {

        private ImageView mImageView;
        private GalleryItem item;

        public PhotoGalleryHolder(View itemView) {
            super(itemView);
            mImageView = (ImageView) itemView.findViewById(R.id.fragment_photo_gallery_image_view);

        }

        public void bindDrawable(Drawable drawable){
            mImageView.setImageDrawable(drawable);
        }

        public void bindGalleryItem(GalleryItem galleryItem){
            item = galleryItem;
        }


    }

    private class PhotoGalleryAdapter extends RecyclerView.Adapter<PhotoGalleryHolder>{

        public void setGalleryItems(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }
        private GalleryItem item;

        List<GalleryItem> mGalleryItems;

        public PhotoGalleryAdapter(List<GalleryItem> items) {
            mGalleryItems = items;
        }

        @Override
        public PhotoGalleryHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View v = inflater.inflate(R.layout.list_item_image_view, parent, false);
            return new PhotoGalleryHolder(v);
        }

        @Override
        public void onBindViewHolder(final PhotoGalleryHolder holder, int position) {
            holder.mImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    transition(holder);
                }
            });
            item = mGalleryItems.get(position);
            holder.bindGalleryItem(item);
            Drawable placeHolder = new ColorDrawable(Color.parseColor(item.getOwner()));
            Glide.with(getActivity())
                    .load(item.getUrl())
                    .placeholder(placeHolder)
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(holder.mImageView);
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }

        private void transition(PhotoGalleryHolder holder) {
            Intent i = PhotoDetailActivity.newIntent(getActivity(), Uri.parse(holder.item.getUrlFull()));

            if (Build.VERSION.SDK_INT < 21) {
                startActivity(i);
            } else {
                ActivityOptionsCompat options = ActivityOptionsCompat.
                        makeSceneTransitionAnimation(getActivity(), holder.mImageView, getString(R.string.transition_test));
                startActivity(i, options.toBundle());
                getActivity().getWindow().setExitTransition(null);
            }
        }
    }

    //Адаптер, устанавливаемый при начальной загрузке данных, представляет собой прогрессбар.
    private class LoadingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(R.layout.progress_bar, parent, false);
            ProgressBar progressBar = view.findViewById(R.id.load_progress_bar);
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

