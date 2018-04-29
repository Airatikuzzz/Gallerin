package com.airatikuzzz.gallerin.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.airatikuzzz.gallerin.IntegerEvent;
import com.airatikuzzz.gallerin.Method;
import com.airatikuzzz.gallerin.R;
import com.kc.unsplash.api.Order;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by maira on 24.04.2018.
 */


public class DataFragment extends Fragment {


    private TabLayout tabLayout;
    private ViewPager viewPager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.tabs, container, false);
        viewPager = v.findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        tabLayout = v.findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.getTabAt(2).select();
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        return v;
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getChildFragmentManager());
        adapter.addFragment(new CategoriesFragment(), "Категории");
        adapter.addFragment(PhotoGalleryFragment.newInstance(Order.POPULAR, Method.LIST_PHOTOS), "Популярное");
        adapter.addFragment(
                PhotoGalleryFragment.newInstance(Order.LATEST, Method.LIST_PHOTOS), "Новые");
        adapter.addFragment(
                PhotoGalleryFragment.newInstance(Order.OLDEST, Method.LIST_PHOTOS), "Старые");
        viewPager.setAdapter(adapter);
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();


        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe
    public void onEvent(IntegerEvent menuItem){
        int id = menuItem.getValue();
        Log.d("kek1", "onevent");
        switch(id){
            case R.id.nav_categories:
                tabLayout.getTabAt(0).select();
                break;
            case R.id.nav_popular:
                tabLayout.getTabAt(1).select();
                break;
            case R.id.nav_new_photos:
                tabLayout.getTabAt(2).select();
                break;
        }
    }
}
