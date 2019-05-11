// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.asurerun.ui.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.microsoft.asurerun.R;
import com.microsoft.asurerun.model.ApplicationState;
import com.microsoft.asurerun.model.RunItem;
import com.microsoft.asurerun.model.ServerCalculations;
import com.microsoft.asurerun.ui.fragments.HistoryDetailsFragment;
import com.microsoft.asurerun.util.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import static com.microsoft.asurerun.util.CacheUtil.MILES_VALUE;
import static com.microsoft.asurerun.util.DateUtil.*;
import static com.microsoft.asurerun.util.ServiceUtil.MILES_KILOMETERS_CONVERSION_RATE;


/**
 * {@link RecyclerView.Adapter} that can display a {@link RunItem}
 */
public class RunHistoryItemRecyclerViewAdapter extends RecyclerView.Adapter {

    private List<RunItem> mValues;
    private Context mContext;
    private final static String TAG = "HIST_ADPT";
    private String mUnitMeasure;
    private int lastVisibleItem, totalItemCount;
    private boolean loading;
    private OnLoadMoreListener onLoadMoreListener;
    private final int VIEW_ITEM = 1;
    private final int VIEW_PROG = 0;

    public RunHistoryItemRecyclerViewAdapter(Context context, String unitMeasure, RecyclerView recyclerView) {
        mValues = new ArrayList<>();
        mContext = context;
        mUnitMeasure = unitMeasure;
        if (recyclerView.getLayoutManager() instanceof LinearLayoutManager) {
            final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView
                    .getLayoutManager();
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView,
                                       int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    totalItemCount = getItemCount() - 1;
                    lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition();
                    if (!loading && totalItemCount == lastVisibleItem) {
                        if (onLoadMoreListener != null) {
                            onLoadMoreListener.onLoadMore();
                        }
                        loading = true;
                    }
                }
            });
        }
    }

    public void setItems(List<RunItem> items) {
        // Only add objects that don't already exist in the current list to avoid unnecessary decryption
        for(RunItem serverItem : items) {
            boolean add = true;
            for(RunItem localItem : mValues) {
                if (localItem.getRunNumber() == serverItem.getRunNumber()) {
                    add = false;
                    break;
                }
            }
            if(add) {
                mValues.add(serverItem);
            }
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder vh;
        if (viewType == VIEW_ITEM) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_runhistoryitem, parent, false);
            vh = new RunItemHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.loading_items_layout, parent, false);
            vh = new ProgressViewHolder(view);
        }
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        if (viewHolder instanceof RunItemHolder) {
            final RunItemHolder holder = ((RunItemHolder) viewHolder);
            holder.mItem = mValues.get(position);
            Log.e(TAG, "run number: " + holder.mItem.getRunNumber());
            long avgPace = 0, time = 0;
            double distance = 0.0;
            Bitmap thumbnail = null;
            Calendar calendar = null;
            double mlResult = 0.0;
            // get data from items
            if (holder.mItem.isLocalItem()) {
                Log.e(TAG, "Local item = " + holder.mItem.toString());
                time = holder.mItem.getTotalTime();
                distance = (mUnitMeasure.equals(MILES_VALUE)) ? holder.mItem.getTotalDistance() / MILES_KILOMETERS_CONVERSION_RATE : holder.mItem.getTotalDistance();
                calendar = holder.mItem.getDate();
            } else {
                ServerCalculations sv = null;
                try {
                    sv = holder.mItem.decryptStatsAndSummary();
                    time = (long) sv.getTotalTime();
                    calendar = sv.getDate();
                    thumbnail = sv.getThumbnail();
                    distance = (mUnitMeasure.equals(MILES_VALUE)) ? sv.getTotalDistance() / MILES_KILOMETERS_CONVERSION_RATE : sv.getTotalDistance();
                    mlResult = sv.getMlResult();
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }

            if (holder.mItem.isLocalItem()) {
                holder.mDistance.setVisibility(View.INVISIBLE);
                holder.mAvgPace.setVisibility(View.INVISIBLE);
                holder.mRunMap.setImageBitmap(holder.mItem.getMapSnapshot());
                holder.mTime.setVisibility(View.INVISIBLE);
                holder.mProcessing.setVisibility(View.VISIBLE);
                holder.mView.setBackgroundColor(holder.cardInactive);
                holder.mRunClassification.setVisibility(View.INVISIBLE);
            } else {
                holder.mProcessing.setVisibility(View.INVISIBLE);
                holder.mDistance.setVisibility(View.VISIBLE);
                holder.mAvgPace.setVisibility(View.VISIBLE);
                holder.mTime.setVisibility(View.VISIBLE);
                holder.mRunClassification.setVisibility(View.VISIBLE);
                if (thumbnail != null) {
                    holder.mRunMap.setImageBitmap(thumbnail);
                } else {
                    holder.mRunMap.setImageDrawable(mContext.getDrawable(R.drawable.placeholder));
                }
                holder.mView.setBackgroundColor(holder.cardActive);

                // set distance label
                holder.mDistance.setText(String.format("%.1f ", distance));
                // set pace label
                if (distance >= 0.001) {
                    avgPace = (long) (time / distance);
                    holder.mAvgPace.setText(getFormattedTimeFromSeconds(avgPace));
                    // set pace unit measure
                    holder.mAvgPace.append("/" + (mUnitMeasure.equals(MILES_VALUE) ? "mi" : "km"));
                } else {
                    holder.mAvgPace.setText("00:00");
                    holder.mAvgPace.append("/" + (mUnitMeasure.equals(MILES_VALUE) ? "mi" : "km"));
                }

                // set unit measure label
                if (mUnitMeasure.equals(MILES_VALUE)) {
                    // convert in miles
                    holder.mDistance.append(mContext.getString(R.string.run_summary_distance_unit_miles));
                } else {
                    holder.mDistance.append(mContext.getString(R.string.run_summary_distance_unit_km));
                }
                holder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        HistoryDetailsFragment mHistoryDetailsFragment = HistoryDetailsFragment.newInstance();
                        // set selected RunItem
                        ApplicationState.setSelectedRunItem(holder.mItem);
                        FragmentManager fragmentManager = ((FragmentActivity) mContext).getSupportFragmentManager();
                        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                        fragmentTransaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right, android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                        fragmentTransaction.replace(R.id.frameLayout, mHistoryDetailsFragment);
                        fragmentTransaction.addToBackStack(null);
                        fragmentTransaction.commit();
                        notifyDataSetChanged();
                    }
                });
                holder.mDeleteRun.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mValues.remove(holder.mItem);
                        ApplicationState.deleteRunItem(mContext, holder.mItem);
                        notifyDataSetChanged();
                    }
                });
            }
            holder.mRunClassification.setText(Utils.determineIntensity(mContext.getResources(), mlResult));
            if (calendar != null) {
                holder.mDate.setText(getFormattedDateFromCalendar(calendar));
                holder.mTime.setText(getFormattedTimeFromSeconds(time));
                holder.mTitle.setText(new SimpleDateFormat("EEEE", Locale.ENGLISH)
                        .format(calendar.getTime()) + " " + getDayPhases(mContext, calendar) + " Run");
            }
        } else {
            ((ProgressViewHolder) viewHolder).progressBar.setIndeterminate(true);
        }
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mValues.get(position) != null ? VIEW_ITEM : VIEW_PROG;
    }

    public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
        this.onLoadMoreListener = onLoadMoreListener;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
    }


    public class RunItemHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mDate;
        public final TextView mTime;
        public final TextView mTitle;
        public final TextView mAvgPace;
        public final TextView mDistance;
        public final TextView mProcessing;
        public final TextView mRunClassification;
        public final TextView mDeleteRun;
        public final ImageView mRunMap;
        public RunItem mItem;

        public int cardActive;
        public int cardInactive;

        public RunItemHolder(View view) {
            super(view);
            mView = view;
            mRunMap = (ImageView) view.findViewById(R.id.runMap);
            mDate = (TextView) view.findViewById(R.id.itemDate);
            mTime = (TextView) view.findViewById(R.id.itemTime);
            mTitle = (TextView) view.findViewById(R.id.itemTitle);
            mAvgPace = (TextView) view.findViewById(R.id.itemAvgPace);
            mDistance = (TextView) view.findViewById(R.id.itemDistance);
            mProcessing = (TextView) view.findViewById(R.id.itemProcessing);
            mRunClassification = (TextView) view.findViewById(R.id.itemClassification);
            mDeleteRun = (TextView) view.findViewById(R.id.deleteRun);

            cardActive = view.getResources().getColor(R.color.cardBackgroundColor);
            cardInactive = view.getResources().getColor(R.color.cardInactiveColor);
        }
    }

    public class ProgressViewHolder extends RecyclerView.ViewHolder {
        public ProgressBar progressBar;

        public ProgressViewHolder(View v) {
            super(v);
            progressBar = (ProgressBar) v.findViewById(R.id.loadingItemsBar);
        }
    }

    public interface OnLoadMoreListener {
        void onLoadMore();
    }
}
