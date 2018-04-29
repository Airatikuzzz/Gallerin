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
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.airatikuzzz.gallerin.EndlessRecyclerOnScrollListener;
import com.airatikuzzz.gallerin.GalleryItem;
import com.airatikuzzz.gallerin.activities.PhotoDetailActivity;
import com.airatikuzzz.gallerin.R;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.kc.unsplash.Unsplash;
import com.kc.unsplash.api.Order;
import com.kc.unsplash.models.Photo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by maira on 12.07.2017.
 */

public class PhotoGalleryFragment extends Fragment {
    private Order order;

    private static final int DEFAULT_PAGE_NUMBER = 1;
    private static final String RECYCLER_STATE = "receycler_state_gallerin";
    private static final String TAG = "PhotoGalleryFragment";
    private static final String ARG_ORDER = "PhotoGalleryFragment_arg_order";

    private boolean isRefreshing = false;
    private boolean isInstalledScrollManager = false;

    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout swipeLayout;
    private GridLayoutManager mGridLayoutManager;

    private EndlessRecyclerOnScrollListener scrollListener;
    private Parcelable recyclerViewState;
    private PhotoGalleryAdapter mAdapter;
    private List<GalleryItem> mItems = new ArrayList<>();

    private Unsplash unsplash;

    public static PhotoGalleryFragment newInstance(Order order_){
        PhotoGalleryFragment fragment = new PhotoGalleryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ORDER, order_.getOrder());
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);

        order = Order.valueOf(getArguments().getString(ARG_ORDER).toUpperCase());

        if(savedInstanceState!=null)        //Возвращает TRUE при смене ориентации
            recyclerViewState = savedInstanceState.getParcelable(RECYCLER_STATE);

        unsplash = new Unsplash("ebe8195594bd221b1c86bb55d0224f1c439b41d0aa4d3736ba7bda6e57dcfde5");

        loadData(DEFAULT_PAGE_NUMBER);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_galleryy, menu);
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
                scrollListener.setPreviousTotal();
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
        return (int)widthInDP/150;
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
                if (isRefreshing) {
                    return;
                }
                loadData(current_page);
            }
        };
        mRecyclerView.addOnScrollListener(scrollListener);
    }

    private void setupAdapter() {
        if(mItems.size() == 0){                                 //Установка адаптера с прогрессбаром при начальной загрузке
            mRecyclerView.setAdapter(new LoadingAdapter());
        }
        else{
            if(mAdapter==null){                                 //Загрузка данных произошла
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
                catch (NullPointerException n){   //Не могу объясниь такое явление, но оно было однажды.
                    Toast.makeText(getActivity(), "Произошла ошибка. Попробуйте перезайти в приложение.", Toast.LENGTH_LONG).show();
                }
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
    private class PhotoGalleryHolder extends RecyclerView.ViewHolder {

        private ImageView mImageView;
        private GalleryItem item;

        public PhotoGalleryHolder(View itemView) {
            super(itemView);
            mImageView = (ImageView) itemView.findViewById(R.id.fragment_photo_gallery_image_view);

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
                    Intent i = PhotoDetailActivity.newIntent(getActivity(), Uri.parse(holder.item.getId()));
                    startActivity(i);
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

