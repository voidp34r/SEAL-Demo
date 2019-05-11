// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.asurerun.ui.fragments;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TabHost;
import android.widget.TextView;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.microsoft.asurerun.model.ApplicationState;
import com.microsoft.asurerun.R;
import com.microsoft.asurerun.model.RunItem;
import com.microsoft.asurerun.model.ServerCalculations;
import com.microsoft.asurerun.ui.adapter.RunHistoryItemRecyclerViewAdapter;
import com.microsoft.asurerun.util.CacheUtil;
import com.microsoft.asurerun.util.Utils;
import com.microsoft.windowsazure.mobileservices.MobileServiceException;
import com.microsoft.windowsazure.mobileservices.MobileServiceList;
import com.microsoft.windowsazure.mobileservices.table.query.QueryOrder;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.microsoft.asurerun.model.ApplicationState.INITIAL_ITEMS_OFFSET;
import static com.microsoft.asurerun.util.DateUtil.getFormattedTimeFromSeconds;
import static com.microsoft.asurerun.util.DialogUtil.*;
import static com.microsoft.asurerun.util.CacheUtil.KM_MILES_KEY;
import static com.microsoft.asurerun.util.CacheUtil.MILES_VALUE;
import static com.microsoft.asurerun.util.CacheUtil.SHAREDPREFFILE;
import static com.microsoft.asurerun.util.ServiceUtil.*;

/**
 * A fragment representing a list of  RunItems.
 */
public class RunHistoryFragment extends Fragment {

    List<RunItem> mRunItems;
    private RunHistoryItemRecyclerViewAdapter mAdapter;
    protected TextView mTotalTime, mTotalAvgPace, mTotalDistace, mTotalRun, mUnitMeasureText, mLoadListText;
    private RecyclerView mRecyclerView;
    private SwipeRefreshLayout mSwipeRefresh;
    private View mFragmentView;
    public final static String TAG = "RUN_HISTORY";
    private String unitMeasure;
    private ProgressBar mProgressListBar;
    private Handler handler;
    // the number of items that are loaded each time new items are paginated
    private final int ITEMS_TO_LOAD = 5;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public RunHistoryFragment() {
    }

    public static RunHistoryFragment newInstance() {
        return new RunHistoryFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (mFragmentView != null) {
            // to prevent view reload after onBackPressed from HistoryDetail
            return mFragmentView;
        }
        mFragmentView = inflater.inflate(R.layout.fragment_runhistoryitem_list, container, false);
        FragmentActivity mFragmentActivity = (AppCompatActivity) getActivity();
        // add title to toolbar
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.title_fragment_history_list);
        // Set the adapter
        Context context = mFragmentView.getContext();
        mRecyclerView = (RecyclerView) mFragmentView.findViewById(R.id.list);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mTotalDistace = (TextView) mFragmentView.findViewById(R.id.totalDistanceText);
        mTotalAvgPace = (TextView) mFragmentView.findViewById(R.id.tot_avg_pace);
        mTotalTime = (TextView) mFragmentView.findViewById(R.id.total_time);
        mTotalRun = (TextView) mFragmentView.findViewById(R.id.total_run);
        mSwipeRefresh = (SwipeRefreshLayout) mFragmentView.findViewById(R.id.swipeRefresh);
        mLoadListText = (TextView) mFragmentView.findViewById(R.id.loadListText);
        mProgressListBar = (ProgressBar) mFragmentView.findViewById(R.id.progressLoadList);
        mUnitMeasureText = (TextView) mFragmentView.findViewById(R.id.unitMeasure);
        unitMeasure = getActivity().getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE).getString(KM_MILES_KEY, MILES_VALUE);
        mUnitMeasureText.setText((unitMeasure.equals(MILES_VALUE) ? getString(R.string.run_history_unit_label_miles) : getString(R.string.run_history_unit_label_km)));
        mRunItems = Collections.synchronizedList(new ArrayList<RunItem>());
        mAdapter = new RunHistoryItemRecyclerViewAdapter(this.getActivity(), unitMeasure, mRecyclerView);
        handler = new Handler();
        mAdapter.setOnLoadMoreListener(new RunHistoryItemRecyclerViewAdapter.OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                Log.e(TAG, "onLoadMore");
                //add null , so the adapter will check view_type and show progress bar at bottom
                mRunItems.add(null);
                mAdapter.notifyItemInserted(mRunItems.size() - 1);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ListenableFuture<MobileServiceList<RunItem>> loadMoreItems = null;
                        loadMoreItems = ApplicationState.getRunTable().skip(mRunItems.size()).top(ITEMS_TO_LOAD).orderBy("runNumber", QueryOrder.Descending).execute();
                        Futures.addCallback(loadMoreItems, new FutureCallback<MobileServiceList<RunItem>>() {
                            @Override
                            public void onSuccess(MobileServiceList<RunItem> result) {
                                //   remove progress item
                                mRunItems.remove(mRunItems.size() - 1);
                                mAdapter.notifyItemRemoved(mRunItems.size());
                                ApplicationState.getLocalRunItems().addAll(result);
                                for (RunItem item : result) {
                                    mRunItems.add(item);
                                    mAdapter.notifyItemInserted(mRunItems.size());
                                }
                                mAdapter.setLoading(false);
                                refreshUi();
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                //   remove progress item
                                mRunItems.remove(mRunItems.size() - 1);
                                mAdapter.notifyItemRemoved(mRunItems.size());
                                mAdapter.setLoading(false);
                            }
                        });
                    }
                }, 2000);
            }
        });
        mRecyclerView.setAdapter(mAdapter);
        if (ApplicationState.getLocalRunItems().isEmpty()) {
            refreshItemsFromTable(false);
        } else {
            mRunItems.addAll(ApplicationState.getLocalRunItems());
            refreshUi();
        }
        mSwipeRefresh.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        Log.d(TAG, "onRefresh called from SwipeRefreshLayout");
                        refreshItemsFromTable(true);
                    }
                }
        );
        return mFragmentView;
    }

    /**
     * Refresh the list with the items in the Mobile Service Table
     */

    private List<RunItem> refreshItemsFromMobileServiceTable(int offset) throws MobileServiceException, ExecutionException, InterruptedException {
        List<RunItem> list = Collections.synchronizedList(ApplicationState.getRunTable().top(offset).orderBy("runNumber", QueryOrder.Descending).execute().get());
        mRunItems.clear();
        return list;
    }

    /**
     * Refresh the list with the items in the Table
     */
    private void refreshItemsFromTable(boolean fromSwipeToRefresh) {
        AsyncTask<Void, Void, Void> task = new RefreshItemsFromServer(this, fromSwipeToRefresh);
        ApplicationState.getLocalRunItems().clear();
        ApplicationState.getLocalRunItems().addAll(mRunItems);
        Utils.runAsyncTask(task);
    }

    private synchronized void showList() {
        mSwipeRefresh.setVisibility(View.VISIBLE);
        mLoadListText.setVisibility(View.INVISIBLE);
        mProgressListBar.setVisibility(View.INVISIBLE);
    }

    private synchronized void refreshUi() {
        RefreshHeaderTask task = new RefreshHeaderTask(this);
        Utils.runAsyncTask(task);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    private static class RefreshHeaderTask extends AsyncTask<Void, Void, Void> {
        WeakReference<RunHistoryFragment> mWeakFragment;
        RunHistoryFragment historyFragment;
        long mSumTime = 0;
        double mSumDistance = 0;
        long mSumAvgPace = 0;

        public RefreshHeaderTask(RunHistoryFragment fragment) {
            mWeakFragment = new WeakReference<RunHistoryFragment>(fragment);
            historyFragment = mWeakFragment.get();
        }

        @Override
        protected void onPreExecute() {
            Log.e(TAG, "On Refresh Header");
            historyFragment.mAdapter.setLoading(true);
        }

        @Override
        protected Void doInBackground(Void... params) {
            for (RunItem item : historyFragment.mRunItems) {
                if (!item.isLocalItem()) {
                    ServerCalculations sv = null;
                    try {
                        sv = item.decryptStatsAndSummary();
                        Log.d(TAG, sv.toString());
                        mSumTime += sv.getTotalTime();
                        mSumDistance += (historyFragment.unitMeasure.equals(MILES_VALUE)) ? sv.getTotalDistance() / MILES_KILOMETERS_CONVERSION_RATE : sv.getTotalDistance();
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage());
                        // remove item locally
                        ApplicationState.getLocalRunItems().remove(item);
                        ApplicationState.deleteLocalRunItem(item);
                        this.cancel(true);
                    }
                }
            }
            if (mSumDistance > 0.001) {
                mSumAvgPace = (long) (mSumTime / mSumDistance);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            historyFragment.mTotalTime.setText(getFormattedTimeFromSeconds(mSumTime));
            historyFragment.mTotalDistace.setText(String.format("%.1f", mSumDistance));
            historyFragment.mTotalAvgPace.setText(getFormattedTimeFromSeconds(mSumAvgPace));
            historyFragment.mSwipeRefresh.setRefreshing(false);
            historyFragment.showList();
            historyFragment.refreshListItem();
            historyFragment.mAdapter.setLoading(false);
            Log.e(TAG, "refresh UI completed");
        }
    }

    private static class RefreshItemsFromServer extends AsyncTask<Void, Void, Void> {
        WeakReference<RunHistoryFragment> mWeakFragment;
        RunHistoryFragment historyFragment;
        boolean mFromSwipeToRefresh;

        public RefreshItemsFromServer(RunHistoryFragment fragment, boolean fromSwipeToRefresh) {
            mWeakFragment = new WeakReference<RunHistoryFragment>(fragment);
            historyFragment = mWeakFragment.get();
            mFromSwipeToRefresh = fromSwipeToRefresh;
        }

        @Override
        protected void onPreExecute() {
            historyFragment.mLoadListText.setText(R.string.run_history_sync_text);
        }

        @Override
        protected Void doInBackground(Void... params) {

            try {
                int offset = 0;
                if (mFromSwipeToRefresh) {
                    offset = CacheUtil.loadMaxRunNumber(historyFragment.getActivity());
                } else {
                    offset = INITIAL_ITEMS_OFFSET;
                }
                historyFragment.mRunItems.addAll(historyFragment.refreshItemsFromMobileServiceTable(offset));
                ApplicationState.getLocalRunItems().clear();
                ApplicationState.getLocalRunItems().addAll(historyFragment.mRunItems);
            } catch (final Exception e) {
                createAndShowDialogFromTask(e, "Refresh items error ", historyFragment.getActivity());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Log.e(TAG, "RUN_ITEMS_SIZE " + historyFragment.mRunItems.size());
            historyFragment.mLoadListText.setText(R.string.run_history_decrypt_text);
            historyFragment.refreshUi();
            historyFragment.refreshListItem();
            historyFragment.mSwipeRefresh.setRefreshing(false);
            historyFragment.showList();
        }
    }

    /**
     * Register the broadcast receiver to KeyGenService intents
     */
    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(REFRESH_ITEM_FINISHED_ACTION)) {
                Log.e(TAG, "refresh UI after data sync");
                // get updated run items
                mRunItems.clear();
                mRunItems.addAll(Collections.synchronizedList(ApplicationState.getLocalRunItems()));
                Log.e(TAG, String.valueOf(mRunItems.size()));
                mSwipeRefresh.setRefreshing(true);
                refreshUi();
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        //register broadcast receiver
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(REFRESH_ITEM_FINISHED_ACTION);
        getActivity().registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        //unregister broadcast receiver
        getActivity().unregisterReceiver(mBroadcastReceiver);
    }

    private synchronized void refreshListItem() {
        mAdapter.setItems(mRunItems);
        mAdapter.notifyDataSetChanged();
        mTotalRun.setText(String.valueOf(mRunItems.size()));
    }
}
