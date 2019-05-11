// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.asurerun.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.microsoft.asurerun.R;
import com.microsoft.asurerun.model.ApplicationState;
import com.microsoft.asurerun.util.DateUtil;
import com.microsoft.asurerun.util.MapUtil;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.microsoft.asurerun.util.CacheUtil.KM_MILES_KEY;
import static com.microsoft.asurerun.util.CacheUtil.MILES_VALUE;
import static com.microsoft.asurerun.util.CacheUtil.SHAREDPREFFILE;
import static com.microsoft.asurerun.util.DateUtil.getFormattedTimeFromSeconds;
import static com.microsoft.asurerun.util.ServiceUtil.*;
import static com.microsoft.asurerun.util.DateUtil.getDayPhases;


public class RunSummaryActivity extends AppCompatActivity implements OnMapReadyCallback {
    private LineChart mChart;
    private SupportMapFragment mapFragment;
    private TextView mElevationGain, mDistance, mElapsedTime, mPace, mRunTitle;
    private final int PATH_WIDTH = 20;
    private final String TAG = "RUN_SUMM";
    private List<LatLng> mPolylinePoints;
    private String unitMeasure;
    /**
     * Google maps variable
     */
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_run_summary);
        // add back arrow to toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        SharedPreferences mSharedPref = getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
        unitMeasure = mSharedPref.getString(KM_MILES_KEY, MILES_VALUE);
        // Get the SupportMapFragment and request notification
        // when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.summaryMap);
        mapFragment.getMapAsync(this);
        // set UI elements
        mElevationGain = (TextView) findViewById(R.id.elevationGain);
        mDistance = (TextView) findViewById(R.id.totalDistance);
        mElapsedTime = (TextView) findViewById(R.id.totalTime);
        mPace = (TextView) findViewById(R.id.avgPace);
        mRunTitle = (TextView) findViewById(R.id.runTitle);
        // set run title
        Date date = new Date();
        mRunTitle.setText(new SimpleDateFormat("EEEE", Locale.ENGLISH).format(date) + " " + getDayPhases(this) + " Run");
        // get intent and set UI data
        Intent intent = getIntent();
        double distance = intent.getDoubleExtra(DISTANCE_SUMMARY_KEY, 0.0);
        if (unitMeasure.equals(MILES_VALUE)) {
            mDistance.setText(String.format("%.1f ", distance / MILES_KILOMETERS_CONVERSION_RATE));
            mDistance.append(getString(R.string.run_summary_distance_unit_miles));
            mElevationGain.setText(String.format("%.1f ",distance / METER_FOOT_CONVERSION_RATE));
            mElevationGain.append(getString(R.string.run_settings_distance_unit_foot));
        } else {
            // convert in km
            mDistance.setText(String.format("%.1f ", intent.getDoubleExtra(DISTANCE_SUMMARY_KEY, 0.0)));
            mDistance.append(getString(R.string.run_summary_distance_unit_km));
            mElevationGain.setText(String.format("%.1f ", intent.getDoubleExtra(ELEVATION_GAIN_SUMMARY_KEY, 0.0)));
            mElevationGain.append(getString(R.string.run_settings_distance_unit_meter));
        }
        long elapsedTime = intent.getLongExtra(TIME_SUMMARY_KEY,0);
        mElapsedTime.setText(DateUtil.getFormattedTimeFromSeconds(elapsedTime));
        if (distance > 0.001) {
            long avgPace = 0;
            if(unitMeasure.equals(MILES_VALUE)){
                distance = distance / MILES_KILOMETERS_CONVERSION_RATE ; // convert in Miles
                avgPace = (long)(elapsedTime/distance);
            }else{
                avgPace = (long)(elapsedTime/distance);
            }
            // shows the avgPace on the screen
            mPace.setText(getFormattedTimeFromSeconds(avgPace));
        }
        mPolylinePoints = intent.getParcelableArrayListExtra(RUN_COORDINATES_EXTRA_KEY);
        mChart = (LineChart) findViewById(R.id.chart);
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
    }

    private void setData() {
        ArrayList<Entry> mAvgPaceEntries = new ArrayList<Entry>();
        ArrayList<Entry> mElevationGainEntries = new ArrayList<Entry>();
        List<Double> mAvgPaceValues = ApplicationState.getAvgPaceValList();
        List<Double> mElevationGainValues = ApplicationState.getElevationGainDeltas();
        Log.d(TAG,"ElevationGains: "+mElevationGainValues.toString());
        Log.d(TAG,"AvgPaceDeltas: "+mAvgPaceValues.toString());
        LineDataSet set1, set2;
        ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
        if (mAvgPaceValues != null && !mAvgPaceValues.isEmpty()) {
            for (int i = 0; i < mAvgPaceValues.size(); i++) {
                mAvgPaceEntries.add(new Entry(i, Math.round(mAvgPaceValues.get(i))));
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
        if (mElevationGainValues != null && !mElevationGainValues.isEmpty()) {
            for (int i = 0; i < mElevationGainValues.size(); i++) {
                mElevationGainEntries.add(new Entry(i, Math.round(mElevationGainValues.get(i))*100)); // multiply by 100 to make the chart uniform
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
        MapUtil.changeMapStyle(TAG, mMap, this);
        if (mPolylinePoints != null && mPolylinePoints.size() > 1) {
            mMap.addPolyline(new PolylineOptions()
                    .addAll(mPolylinePoints)
                    .width(PATH_WIDTH)
                    .color(Color.argb(255, 255, 140, 0))
            );
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (LatLng latLng: mPolylinePoints) {
                builder.include(latLng);
            }
            final LatLngBounds bounds = builder.build();
            final View mMapView = mapFragment.getView();
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
                        Log.e(TAG, "h: " + height + " w: " + width);
                        int padding = (int) (width * 0.12); // offset from edges of the map 12% of screen
                        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding);
                        mMap.moveCamera(cu);
                    }
                }
            });
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
