// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.asurerun.ui.fragments;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;
import com.microsoft.asurerun.R;
import com.microsoft.asurerun.model.ApplicationState;
import com.microsoft.asurerun.model.RunItem;
import com.microsoft.asurerun.model.ServerCalculations;
import com.microsoft.asurerun.util.MapUtil;
import com.microsoft.asurerun.util.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import static com.microsoft.asurerun.util.CacheUtil.KM_MILES_KEY;
import static com.microsoft.asurerun.util.CacheUtil.MILES_VALUE;
import static com.microsoft.asurerun.util.CacheUtil.SHAREDPREFFILE;
import static com.microsoft.asurerun.util.DateUtil.*;
import static com.microsoft.asurerun.util.ServiceUtil.*;

public class HistoryDetailsFragment extends Fragment implements OnMapReadyCallback {
    private LineChart mChart;
    private TextView mElevationGain, mDistance, mElapsedTime, mPace, mRunTitle, mRunClassification;
    private final int PATH_WIDTH = 20;
    private RunItem mSelectedRunItem;
    private List<LatLng> mPolylinePoints;
    private List<Double> mAvgPaceDeltas;
    private List<Double> mElevationGainDeltas;
    private MapView mMapView;
    private String unitMeasure;
    private final static String TAG = "HIS_DET";
    /**
     * Google maps variable
     */
    private GoogleMap mMap;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public HistoryDetailsFragment() {
    }

    public static HistoryDetailsFragment newInstance() {
        HistoryDetailsFragment fragment = new HistoryDetailsFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history_details_layout, container, false);
        // Get the SupportMapFragment and request notification
        // when the map is ready to be used.
        mMapView = (MapView) view.findViewById(R.id.detailMap);
        mMapView.onCreate(savedInstanceState);
        mMapView.getMapAsync(this);
        FragmentActivity mFragmentActivity = (FragmentActivity) getActivity();
        // add back arrow to toolbar
        if (mFragmentActivity.getActionBar() != null) {
            mFragmentActivity.getActionBar().setDisplayHomeAsUpEnabled(true);
            mFragmentActivity.getActionBar().setDisplayShowHomeEnabled(true);
        }
        unitMeasure = getActivity().getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE).getString(KM_MILES_KEY, MILES_VALUE);
        // add title to toolbar
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.title_fragment_history_detail);
        // set selected run item from application state
        mSelectedRunItem = ApplicationState.getSelectedRunItem();
        mElevationGain = (TextView) view.findViewById(R.id.elevationGain);
        mDistance = (TextView) view.findViewById(R.id.totalDistance);
        mElapsedTime = (TextView) view.findViewById(R.id.totalTime);
        mPace = (TextView) view.findViewById(R.id.avgPace);
        mRunTitle = (TextView) view.findViewById(R.id.runTitle);
        mChart = (LineChart) view.findViewById(R.id.chart);
        mRunClassification = (TextView) view.findViewById(R.id.itemClassification);
        // set UI values
        long avgPace = 0, time = 0;
        double distance = 0.0, elevationGain = 0.0;
        Calendar calendar = null;
        if (mSelectedRunItem.isLocalItem()) {
            time = mSelectedRunItem.getTotalTime();
            elevationGain = mSelectedRunItem.getElevationGain();
            distance = (unitMeasure.equals(MILES_VALUE)) ? mSelectedRunItem.getTotalDistance() / MILES_KILOMETERS_CONVERSION_RATE : mSelectedRunItem.getTotalDistance();
            calendar = mSelectedRunItem.getDate();
        } else {
            ServerCalculations sv = null;
            try {
                sv = mSelectedRunItem.decryptStatsAndSummary();
                time = (long) sv.getTotalTime();
                mElevationGainDeltas = sv.getElevationGainDeltas();
                mAvgPaceDeltas = sv.getAvgPaceDeltas();
                if (sv.getElevationGain() > 0)
                    elevationGain = sv.getElevationGain();
                calendar = sv.getDate();
                distance = (unitMeasure.equals(MILES_VALUE)) ? sv.getTotalDistance() / MILES_KILOMETERS_CONVERSION_RATE : sv.getTotalDistance();
                Log.e(TAG, "distance" + distance);
                mPolylinePoints = Arrays.asList(sv.getCoordinates());
                mRunClassification.setText(Utils.determineIntensity(getResources(), sv.getMlResult()));
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }
        // set pace label
        if (distance >= 0.001) {
            avgPace = (long) (time / distance);
            mPace.setText(getFormattedTimeFromSeconds(avgPace));
        }
        if (calendar != null)
            mRunTitle.setText(new SimpleDateFormat("EEEE", Locale.ENGLISH).format(calendar.getTime())
                    + " " + getDayPhases(getActivity(), calendar) + " Run");
        if (unitMeasure.equals(MILES_VALUE)) {
            mDistance.setText(String.format("%.1f mi", distance));
            mElevationGain.setText(String.format("%.1f ft", elevationGain / METER_FOOT_CONVERSION_RATE));
        } else {
            mDistance.setText(String.format("%.1f km", distance));
            mElevationGain.setText(String.format("%.1f m", elevationGain));
        }
        mElapsedTime.setText(getFormattedTimeFromSeconds(time));
        // no description text
        mChart.getDescription().setEnabled(false);

        // enable touch gestures
        mChart.setTouchEnabled(true);

        mChart.setDragDecelerationFrictionCoef(0.9f);
        // enable scaling and dragging
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setDrawGridBackground(false);
        mChart.setHighlightPerDragEnabled(true);
        // if disabled, scaling can be done on x- and y-axis separately
        mChart.setPinchZoom(true);
        // set an alternative background color
        mChart.setBackgroundColor(ColorTemplate.rgb("#303030"));
        // add data
        setData();
        mChart.animateX(2500);
        // get the legend (only possible after setting data)
        Legend mPaceLegend = mChart.getLegend();
        Legend l = mChart.getLegend();
        // modify the legend
        mPaceLegend.setForm(Legend.LegendForm.CIRCLE);
        mPaceLegend.setTextSize(20f);
        mPaceLegend.setTextColor(Color.WHITE);
        mPaceLegend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        mPaceLegend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        mPaceLegend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        mPaceLegend.setDrawInside(false);
        mPaceLegend.setYOffset(5f);
        l.setForm(Legend.LegendForm.CIRCLE);
        l.setTextSize(18f);
        l.setXEntrySpace(100f);
        l.setTextColor(Color.WHITE);
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(false);
        l.setYOffset(5f);
        XAxis xAxis = mChart.getXAxis();
        xAxis.setTextColor(ColorTemplate.rgb("#303030"));
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setGranularityEnabled(false);
        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setTextColor(ColorTemplate.rgb("#303030"));
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(false);
        leftAxis.setGranularityEnabled(false);
        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setTextColor(ColorTemplate.rgb("#303030"));
        rightAxis.setDrawGridLines(false);
        rightAxis.setDrawZeroLine(false);
        rightAxis.setGranularityEnabled(false);

        return view;
    }

    private void setData() {
        ArrayList<Entry> mAvgPaceEntries = new ArrayList<Entry>();
        ArrayList<Entry> mElevationGainEntries = new ArrayList<Entry>();
        LineDataSet set1, set2;
        ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
        if (mAvgPaceDeltas != null && !mAvgPaceDeltas.isEmpty()) {
            for (int i = 0; i < mAvgPaceDeltas.size(); i++) {
                mAvgPaceEntries.add(new Entry(i, Math.round(mAvgPaceDeltas.get(i))));
            }
            // create a dataset and give it a type
            set1 = new LineDataSet(mAvgPaceEntries, getString(R.string.pace_set_name));
            set1.setDrawIcons(false);
            set1.setColor(ColorTemplate.rgb("#FF8C00"));
            set1.setLineWidth(2f);
            set1.setDrawCircles(false);
            set1.setDrawCircleHole(false);
            set1.setValueTextSize(0f);
            set1.setFormLineWidth(5f);
            set1.setFormSize(15f);

            dataSets.add(set1);
        }
        if (mElevationGainDeltas != null && !mElevationGainDeltas.isEmpty()) {
            for (int i = 0; i < mElevationGainDeltas.size(); i++) {
                mElevationGainEntries.add(new Entry(i, Math.round(mElevationGainDeltas.get(i)) * 100)); // multiply by 100 to make the chart uniform
            }
            // create a dataset and give it a type
            set2 = new LineDataSet(mElevationGainEntries, getString(R.string.elevation_gain_set_name));
            set2.setDrawIcons(false);
            set2.setColor(Color.WHITE);
            set2.setLineWidth(2f);
            set2.setDrawCircles(false);
            set2.setDrawCircleHole(false);
            set2.setValueTextSize(0f);
            set2.setFormLineWidth(5f);
            set2.setFormSize(15f);

            dataSets.add(set2);
        }
        if (dataSets.size() > 1) {
            // set data
            mChart.setData(new LineData(dataSets));
            mChart.invalidate();
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        MapUtil.changeMapStyle(TAG, mMap, getActivity());
        if (mPolylinePoints != null && mPolylinePoints.size() > 1) {
            mMap.addPolyline(new PolylineOptions()
                    .addAll(mPolylinePoints)
                    .width(PATH_WIDTH)
                    .color(Color.argb(255, 255, 140, 0))
            );
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (LatLng latLng : mPolylinePoints) {
                builder.include(latLng);
            }
            final LatLngBounds bounds = builder.build();
            int width = mMapView.getResources().getDisplayMetrics().widthPixels;
            int height = mMapView.getResources().getDisplayMetrics().heightPixels;
            int padding = (int) (width * 0.12); // offset from edges of the map 12% of screen
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding);
            mMap.moveCamera(cu);
            // resize map after layout loading
            mMapView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    //At this point the layout is complete and the
                    //dimensions of myView and any child views are known.
                    if (mMapView != null) {
                        int width = mMapView.getWidth();
                        int height = mMapView.getHeight();
                        Log.d(TAG, "h: " + height + " w: " + width);
                        int padding = (int) (width * 0.12); // offset from edges of the map 12% of screen
                        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding);
                        mMap.moveCamera(cu);
                    }
                }
            });
        }
    }

    @Override
    public void onResume() {
        mMapView.onResume();
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }
}
