// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.asurerun.ui.fragments;


import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.MPPointF;

import com.microsoft.asurerun.R;
import com.microsoft.asurerun.model.ApplicationState;
import com.microsoft.asurerun.model.RunItem;
import com.microsoft.asurerun.model.ServerCalculations;
import com.microsoft.asurerun.util.CacheUtil;
import com.microsoft.asurerun.util.Utils;
import com.microsoft.windowsazure.mobileservices.MobileServiceException;
import com.microsoft.windowsazure.mobileservices.table.query.QueryOrder;

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.microsoft.asurerun.model.ApplicationState.INITIAL_ITEMS_OFFSET;
import static com.microsoft.asurerun.util.DialogUtil.createAndShowDialogFromTask;

public class PredictionsFragment extends Fragment {
    public final static String TAG = PredictionsFragment.class.getCanonicalName();
    private PieChart mChart;
    private TextView mHighPercentage, mMediumPercentage, mLowPercentage;
    private ProgressBar mProgressBar;
    private int mLowRuns = 0;
    private int mMedRuns = 0;
    private int mHighRuns = 0;

    public PredictionsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment PredictionsFragment.
     */
    public static PredictionsFragment newIstance() {
        return new PredictionsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_predictions_layout, container, false);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.title_fragment_predictions);
        mChart = (PieChart) v.findViewById(R.id.chart);
        mHighPercentage = (TextView) v.findViewById(R.id.highClassificationValue);
        mMediumPercentage = (TextView) v.findViewById(R.id.mediumClassificationValue);
        mLowPercentage = (TextView) v.findViewById(R.id.lowClassificationValue);
        mProgressBar = (ProgressBar) v.findViewById(R.id.predictionProgress);
        // chart settings
        mChart.setUsePercentValues(true);
        mChart.getDescription().setEnabled(false);
        mChart.setExtraOffsets(10, 10, 10, 10);
        mChart.setDragDecelerationFrictionCoef(0.95f);
        mChart.setDrawHoleEnabled(false);
        mChart.setDrawCenterText(false);
        mChart.setRotationAngle(0);
        mChart.animateY(400, Easing.EasingOption.EaseInOutQuad);
        // enable rotation of the mChart by touch
        mChart.setRotationEnabled(true);
        mChart.setHighlightPerTapEnabled(true);
        mChart.getLegend().setEnabled(false);
        asyncGetIntensities();
        return v;
    }

    /**
     * Creates a pie chart with a distribution of all the runs.
     */
    private void setData() {
        updatePieChart();
        updateLegend();
    }

    /**
     * Updates the data in the pie chart with all the runs.
     */
    private void updatePieChart() {
        ArrayList<PieEntry> entries = filterPieEntries();
        PieDataSet dataSet = new PieDataSet(entries, "Predictions");
        dataSet.setDrawIcons(false);
        dataSet.setSliceSpace(2f);
        dataSet.setIconsOffset(new MPPointF(0, 40));
        dataSet.setSelectionShift(5f);
        dataSet.setColors(new int[]{R.color.classification_low_intensity, R.color.classification_medium_intensity, R.color.classification_high_intensity}, getActivity());
        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter());
        data.setValueTextSize(22f);
        data.setValueTextColor(Color.WHITE);
        mChart.setData(data);
        mChart.invalidate();
    }

    /**
     * Return a list of all the entries in the pie chart excluding the intensities that don't exist
     * since the pie chart doesn't detect empty entries.
     * @return A list of all the entries in the pie chart.
     */
    private ArrayList<PieEntry> filterPieEntries() {
        ArrayList<PieEntry> entries = new ArrayList<>();
        String lowIntensityStr = getResources().getString(R.string.classification_low);
        String medIntensityStr = getResources().getString(R.string.classification_medium);
        String highIntensityStr = getResources().getString(R.string.classification_high);
        if(mLowRuns > 0) {
            entries.add(new PieEntry(mLowRuns, lowIntensityStr));
        }
        if(mMedRuns > 0) {
            entries.add(new PieEntry(mMedRuns, medIntensityStr));
        }
        if(mHighRuns > 0) {
            entries.add(new PieEntry(mHighRuns, highIntensityStr));
        }
        return entries;
    }

    /**
     * Updates the legend for the pie chart.
     */
    private void updateLegend() {
        List<RunItem> runItems = ApplicationState.getLocalRunItems();
        // If there are no runs at all show the legend at zero percent.
        if(runItems.isEmpty()) {
            String zeroPercent = "0%";
            mHighPercentage.setText(zeroPercent);
            mMediumPercentage.setText(zeroPercent);
            mLowPercentage.setText(zeroPercent);
        } else {
            DecimalFormat mFormat = new DecimalFormat("###,###,##0.0");
            mHighPercentage.setText(String.valueOf(mFormat.format(100.0 * mHighRuns / runItems.size())) + "%");
            mMediumPercentage.setText(String.valueOf(mFormat.format(100.0 * mMedRuns / runItems.size())) + "%");
            mLowPercentage.setText(String.valueOf(mFormat.format(100.0 * mLowRuns / runItems.size())) + "%");
        }
    }

    /**
     * Initially, the pie chart is hidden and is replaced by a progress bar since decrypting the
     * prediction takes a while, so this should only be called after the decryption is done.
     */
    private void revealPieChart() {
        mProgressBar.setVisibility(View.GONE);
        mChart.setVisibility(View.VISIBLE);
    }

    /**
     * Decrypts the prediction results. It takes a while, so this is done asynchronously.
     */
    private void asyncGetIntensities() {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                // Determine how much of each run there is based on the output string of the prediction.
                String medIntensityStr = getResources().getString(R.string.classification_medium);
                String highIntensityStr = getResources().getString(R.string.classification_high);
                // No need for low since it's the only other option if medium and high aren't it.
                List<RunItem> runItems = ApplicationState.getLocalRunItems();
                for(int i = 0; i < runItems.size(); ++i) {
                    try {
                        ServerCalculations calc = runItems.get(i).decryptStatsAndSummary();
                        String mlResult = Utils.determineIntensity(getResources(), calc.getMlResult());
                        if(mlResult == highIntensityStr)
                            ++mHighRuns;
                        else if(mlResult == medIntensityStr)
                            ++mMedRuns;
                        else
                            ++mLowRuns;
                    } catch(Exception e) {
                        Log.e(TAG, "Error calculating prediction: " + e.getMessage());
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                setData();
                revealPieChart();
            }
        };
        Utils.runAsyncTask(task);
    }

    /**
     * Refresh the list with the items in the Mobile Service Table
     */
    private List<RunItem> refreshItemsFromMobileServiceTable() throws MobileServiceException, ExecutionException, InterruptedException {
        List<RunItem> list = Collections.synchronizedList(ApplicationState.getRunTable().execute().get());
        return list;
    }

    /**
     * Refresh the list with the items in the Table
     */
    private void refreshItemsFromTable() {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            List<RunItem> mRunItems;

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    mRunItems = refreshItemsFromMobileServiceTable();
                } catch (final Exception e) {
                    createAndShowDialogFromTask(e, "Refresh items error ", getActivity());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                ApplicationState.getLocalRunItems().clear();
                ApplicationState.getLocalRunItems().addAll(mRunItems);
                setData();
            }
        };
        Utils.runAsyncTask(task);
    }
}
