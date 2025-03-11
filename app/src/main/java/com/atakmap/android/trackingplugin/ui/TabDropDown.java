package com.atakmap.android.trackingplugin.ui;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.TabHost;

import androidx.viewpager.widget.ViewPager;

import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.trackingplugin.plugin.R;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

// TODO-extended: is there any way in god's green earth that we can update this so it's not using
//  horribly deprecated Views/Fragments/etc? Can we ever import google material assets ever?

public class TabDropDown extends DropDownReceiver {
    final Context context;
    final View view;
    final List<String> tabNames = List.of("Tracking", "Devices", "Sensors", "Debug");
    final List<View> tabViews;
//    final ViewPager viewPager;

    protected TabDropDown(MapView mapView, Context context) {
        super(mapView);
        this.context = context;
        this.view = PluginLayoutInflater.inflate(context, R.layout.main_layout);

        tabViews = new ArrayList<>();
//        tabViews.add(PluginLayoutInflater.inflate(context, R.layout.tab_1));
//        tabViews.add(PluginLayoutInflater.inflate(context, R.layout.tab_2));
//        tabViews.add(PluginLayoutInflater.inflate(context, R.layout.tab_3));
        tabViews.add(PluginLayoutInflater.inflate(context, R.layout.debug_layout));
//        viewPager = this.view.findViewById(R.id.viewPager);

        TabLayout layout = new TabLayout(context);
        TabLayout.Tab tab = new TabLayout.Tab();
        tab.setText("Debug");
        tab.setCustomView(R.layout.debug_layout);
        layout.addTab(tab);
//        tabHost = this.view.findViewById(R.id.tabHost);
        // notes:
        // - tabHost is on its last leg, but what else is new, only thing that ATAK supports,
        // again, what else is new.
        // - can't really use viewPager2, forced to use more deprecated code, awesome love that.
        // - need to associate viewPager with the tabViews list.
        // - beyond here it's just coordinating between viewPager and tabHost
//        TabHost.TabSpec firstTab = tabHost.newTabSpec("hi");
//        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
//            @Override
//            public void onPageSelected(int position) {
//                tabHost.setCurrentTab(position); // maybe idk
//            }
//        });
    }

    @Override
    protected void disposeImpl() {

    }

    @Override
    public void onReceive(Context context, Intent intent) {

    }
}
