package com.airatikuzzz.gallerin.activities;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.airatikuzzz.gallerin.Method;
import com.airatikuzzz.gallerin.R;
import com.airatikuzzz.gallerin.fragments.DataFragment;
import com.airatikuzzz.gallerin.fragments.PhotoGalleryFragment;
import com.airatikuzzz.gallerin.fragments.SearchFragment;
import com.kc.unsplash.api.Order;

public class SearchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragmentContainer_search);

        if(fragment == null){
            fragment = SearchFragment.newInstance();
            fm.beginTransaction().replace(R.id.fragmentContainer_search,fragment).commit();
        }

        Toolbar toolbar = findViewById(R.id.toolbar_search);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setTitle("Поиск");

    }




}
