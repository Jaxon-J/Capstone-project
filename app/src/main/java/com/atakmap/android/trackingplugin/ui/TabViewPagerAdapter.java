package com.atakmap.android.trackingplugin.ui;

import android.content.Context;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.atakmap.android.trackingplugin.Constants;

import java.util.List;

public class TabViewPagerAdapter extends RecyclerView.Adapter<TabViewPagerAdapter.TabViewHolder> {
    private static final String TAG = Constants.createTag(TabViewPagerAdapter.class);
    private final Context context;
    private final List<Pair<String, Integer>> tabInfo;

    public TabViewPagerAdapter(Context context, List<Pair<String, Integer>> tabNameLayoutIdPairs) {
        this.context = context;
        this.tabInfo = tabNameLayoutIdPairs;
    }

    @NonNull
    @Override
    public TabViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int position) {
        Log.d(TAG, "onCreateViewHolder: " + tabInfo.get(position).first);
        View tabLayout = LayoutInflater.from(this.context)
                .inflate(tabInfo.get(position).second, parent, false);
        return new TabViewHolder(tabLayout, tabInfo.get(position).first);
    }

    @Override
    public void onBindViewHolder(@NonNull TabViewHolder holder, int position) {
        // switch (holder.tabName) {
            // update tabs here, case [TabName]: [behavior]; break;
        // }
    }

    @Override
    public int getItemCount() {
        return tabInfo.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position; // passes position to onCreateViewHolder instead of default (0)
    }

    public static class TabViewHolder extends RecyclerView.ViewHolder {
        public String tabName;
        public TabViewHolder(@NonNull View itemView, String name) {
            super(itemView);
            this.tabName = name;
        }
    }
}