package com.atakmap.android.trackingplugin.ui;

import android.content.Context;
import android.graphics.Color;
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
    private final int[] debugTabColors = new int[]{Color.parseColor("#FFCCCC"), Color.parseColor(
            "#CCFFCC"), Color.parseColor("#CCCCFF"), Color.parseColor("#FFFFCC")};

    public TabViewPagerAdapter(Context context, List<Pair<String, Integer>> tabNameLayoutIdPairs) {
        this.context = context;
        this.tabInfo = tabNameLayoutIdPairs;
    }

    @NonNull
    @Override
    public TabViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int position) {
        Log.d(TAG, "onCreateViewHolder: " + tabInfo.get(position).first);
//        View tabLayout = PluginLayoutInflater.inflate(this.context, tabInfo.get(position)
//        .second, parent, false);
        View tabLayout = LayoutInflater.from(this.context)
                .inflate(tabInfo.get(position).second, parent, false);
//        TextView tabLayout = new TextView(parent.getContext());
//        tabLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams
//        .MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
//        tabLayout.setForegroundGravity(Gravity.CENTER);
//        tabLayout.setText("EXAMPLE");
//        tabLayout.setBackgroundColor(debugTabColors[position]);
        return new TabViewHolder(tabLayout, tabInfo.get(position).first);
    }

    @Override
    public void onBindViewHolder(@NonNull TabViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder: " + tabInfo.get(position).first);
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