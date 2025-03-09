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

import java.util.ArrayList;
import java.util.List;

public class TabDropDown extends DropDownReceiver {
    final Context context;
    final View view;
    final TabHost tabHost;
    final List<View> tabViews;
    final ViewPager viewPager;

    protected TabDropDown(MapView mapView, Context context) {
        super(mapView);
        this.context = context;
        this.view = PluginLayoutInflater.inflate(context, R.layout.main_layout);

        tabViews = new ArrayList<>();
//        tabViews.add(PluginLayoutInflater.inflate(context, R.layout.tab_1));
//        tabViews.add(PluginLayoutInflater.inflate(context, R.layout.tab_2));
//        tabViews.add(PluginLayoutInflater.inflate(context, R.layout.tab_3));
        tabViews.add(PluginLayoutInflater.inflate(context, R.layout.debug_layout));
        viewPager = this.view.findViewById(R.id.viewPager);

        tabHost = this.view.findViewById(R.id.tabHost);
        // notes:
        // - tabHost is on its last leg, but what else is new, only thing that ATAK supports,
        // again, what else is new.
        // - can't really use viewPager2, forced to use more deprecated code, awesome love that.
        // - need to associate viewPager with the tabViews list.
        // - beyond here it's just coordinating between viewPager and tabHost
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                tabHost.setCurrentTab(position); // maybe idk
            }
        });
    }

    @Override
    protected void disposeImpl() {

    }

    @Override
    public void onReceive(Context context, Intent intent) {

    }
}
