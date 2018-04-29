package com.airatikuzzz.gallerin;

import android.support.v4.app.Fragment;

import com.airatikuzzz.gallerin.fragments.PhotoGalleryFragment;
import com.kc.unsplash.api.Order;

/**
 * Created by maira on 25.04.2018.
 */

public class FragmentBuilder {
    private Fragment mFragment;
    private int container;
    private Order order;

    public FragmentBuilder() {
        mFragment = null;
        container = 0;
        order = Order.POPULAR;
    }

    public FragmentBuilder setFragment(Fragment fragment) {
        mFragment = fragment;
        return this;
    }

    public FragmentBuilder setContainer(int container) {
        this.container = container;
        return this;
    }

    public FragmentBuilder setOrder(Order order) {
        this.order = order;
        return this;
    }

    public Fragment buildFragment(){
        return null;
    }
}
