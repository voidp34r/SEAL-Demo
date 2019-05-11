// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.asurerun.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.model.LatLngBounds;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.asurerun.model.ApplicationState;
import com.microsoft.asurerun.R;
import com.microsoft.asurerun.model.RunItem;
import com.microsoft.asurerun.model.ServerCalculations;
import com.microsoft.asurerun.service.KeyGenService;
import com.microsoft.asurerun.service.RunService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.microsoft.asurerun.service.SyncDataService;
import com.microsoft.asurerun.util.CacheUtil;
import com.microsoft.asurerun.util.Checks;
import com.microsoft.asurerun.util.DateUtil;
import com.microsoft.asurerun.util.DialogUtil;
import com.microsoft.asurerun.util.MapUtil;
import com.microsoft.asurerun.util.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.view.View.GONE;
import static com.microsoft.asurerun.service.KeyGenService.STATE_KEY;
import static com.microsoft.asurerun.util.CacheUtil.KILOMETERS_VALUE;
import static com.microsoft.asurerun.util.CacheUtil.KM_MILES_KEY;
import static com.microsoft.asurerun.util.CacheUtil.MILES_VALUE;
import static com.microsoft.asurerun.util.CacheUtil.SHAREDPREFFILE;
import static com.microsoft.asurerun.util.CacheUtil.cacheMaxRunNumber;
import static com.microsoft.asurerun.util.CacheUtil.loadMaxRunNumber;
import static com.microsoft.asurerun.util.MapUtil.MAP_THUMBNAIL_HEIGHT;
import static com.microsoft.asurerun.util.MapUtil.MAP_THUMBNAIL_WIDTH;
import static com.microsoft.asurerun.util.ServiceUtil.*;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.toRadians;

public class RunActivity extends AppCompatActivity implements OnMapReadyCallback {
    /**
     * Google maps variable
     */
    private GoogleMap mMap; // TODO: we need to investigate if data is being sent to google in plain text.

    /*
     * View variable declaration
     */
    private ImageButton mControlButton;
    private SupportMapFragment mapFragment;
    private TextView mTimeText, mDistanceText, mElevationGainText, mRunPace, mTextMessage, mDistanceInfo;
    /**
     * Constants declaration
     */
    private final static int MAP_ZOOM = 18;
    private final long UPDATE_TIME = 5000;
    private final int PATH_WIDTH = 20;
    private final int MINIMUM_GPS_THRESHOLD = 30; // in meters
    private final static int MAP_Y_OFFSET = 300; // the offset required to center map
    public final static int POWER_SAVE_SETTING_REQUEST = 1100;
    private final static String TAG = "RunActivity";
    private LatLng mLastLocation;
    // shared crypto context
    private long mCryptoContext;

    /*
     * Indicates the state of the RunActivity:
     */
    public enum State {
        Stopped,
        Running
    }

    public State mCurrentState;
    protected List<LatLng> polylinePoints = new ArrayList<>();
    private Intent backgroundServiceIntent;
    private LocationRequest mLocationRequest;
    private FusedLocationProviderClient mFusedLocationClient;
    private ArrayList<Double> cartesianX;
    private ArrayList<Double> cartesianY;
    private ArrayList<Double> cartesianZ;
    private ArrayList<float[]> accelerometerValues;
    private ArrayList<float[]> gyroscopeValues;
    private ArrayList<Double> timeStamps;
    private ArrayList<Double> elevationGainDeltas;
    private ArrayList<Double> avgPaceDeltas;
    private double mElevationGainSum;
    private double mDistance;
    private String mElapsedTime;
    private String unitMeasure;
    private long mTotalTime;
    private Bitmap mMapSnapshot;

    private static int FRAME_UPDATE_INTERVAL = 2;
    private int mFrameUpdateInterval = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_run_layout);
        ApplicationState.initializeRunData();
        // retrieve crypto context from arguments
        Intent i = getIntent();
        mCryptoContext = ApplicationState.getCryptoContext();
        // set UI component
        mControlButton = (ImageButton) findViewById(R.id.stop);
        mTimeText = (TextView) findViewById(R.id.time);
        mDistanceText = (TextView) findViewById(R.id.distance);
        mElevationGainText = (TextView) findViewById(R.id.elevation);
        mRunPace = (TextView) findViewById(R.id.avg);
        mTextMessage = (TextView) findViewById(R.id.textMessageRun);
        //set distance label value
        mDistanceInfo = (TextView) findViewById(R.id.distanceInfo);
        SharedPreferences mSharedPref = getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
        unitMeasure = mSharedPref.getString(KM_MILES_KEY, MILES_VALUE);
        mDistanceInfo.setText((unitMeasure.equals(MILES_VALUE) ? getString(R.string.run_activity_distance_miles) : getString(R.string.run_activity_distance_km)));
        if (ApplicationState.isKeysCreated()) {
            mTextMessage.setVisibility(GONE);
        }
        // check if power save mode is on
        if (Checks.isPowerSaveMode(this)) {
            Analytics.trackEvent(TAG + " power save mode is on");
            showBatterySaveModeWarning();
        } else {
            startRunService();
        }
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // Get the SupportMapFragment and request notification
        // when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_run);
        mapFragment.getMapAsync(this);
        mControlButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View view) {
                if (mCurrentState == State.Running) {
                    stopRunService();
                    if (!ApplicationState.isKeysCreated()) { // checks if the keys are created
                        DialogUtil.showWarningDialog(RunActivity.this,getString(R.string.run_activity_keys_warning));
                    } else if (!Checks.IsNetworkConnected(RunActivity.this)) {
                        startRunSummaryActivity();
                        sendRunData();
                    } else {
                        createMapSnapshot();
                    }
                }
            }
        });
        cartesianX = new ArrayList<Double>();
        cartesianY = new ArrayList<Double>();
        cartesianZ = new ArrayList<Double>();
        timeStamps = new ArrayList<Double>();
        gyroscopeValues = new ArrayList<>();
        accelerometerValues = new ArrayList<>();
        elevationGainDeltas = new ArrayList<>();
        avgPaceDeltas = new ArrayList<>();
    }

    private void startRunSummaryActivity() {
        unregisterReceiver(mBroadcastReceiver);
        ApplicationState.setAvgPaceValList(avgPaceDeltas);
        Intent myIntent = new Intent(RunActivity.this, RunSummaryActivity.class);
        myIntent.putExtra(DISTANCE_SUMMARY_KEY, mDistance);
        myIntent.putExtra(TIME_SUMMARY_KEY, mTotalTime);
        myIntent.putExtra(ELEVATION_GAIN_SUMMARY_KEY, mElevationGainSum);
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(RUN_COORDINATES_EXTRA_KEY, (ArrayList<LatLng>) polylinePoints);
        myIntent.putExtras(bundle);
        RunActivity.this.startActivity(myIntent);
        RunActivity.this.finish();
    }

    @SuppressLint("MissingPermission")
    private void createMapSnapshot() {
        mMap.setMyLocationEnabled(false);
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng latLng : polylinePoints) {
            builder.include(latLng);
        }
        final GoogleMap.SnapshotReadyCallback snapshotCallback = new GoogleMap.SnapshotReadyCallback() {
            @Override
            public void onSnapshotReady(Bitmap snapshot) {
                mMapSnapshot = Bitmap.createScaledBitmap(snapshot, MAP_THUMBNAIL_WIDTH, MAP_THUMBNAIL_HEIGHT, true); // scale bitmap
                startRunSummaryActivity();
                sendRunData();
            }
        };
        //Sometimes the user has not moved. Causes a crash in builder.build.
        if (polylinePoints.size() > 0) {
            // resize the map
            final View mMapView = mapFragment.getView();
            final LatLngBounds bounds = builder.build();
            mMapView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    //At this point the layout is complete and the
                    //dimensions of map view and any child views are known.
                    if (mMapView != null) {
                        int width = mMapView.getWidth();
                        int height = mMapView.getHeight() / 2;
                        Log.d(TAG, "h: " + height + " w: " + width);
                        int padding = (int) (width * 0.12); // offset from edges of the map 12% of screen
                        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding);
                        mMap.moveCamera(cu);
                        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                            @Override
                            public void onMapLoaded() {
                                mMap.snapshot(snapshotCallback);
                            }
                        });
                    }
                }
            });
        } else {
            // take the snapshot even when the map is empty
            mMap.snapshot(snapshotCallback);
        }
    }

    private void startRunService() {
        // set running state
        mCurrentState = State.Running;
        // start RunService
        backgroundServiceIntent = new Intent(RunActivity.this, RunService.class);
        backgroundServiceIntent.setAction(START_TRACKER);
        startService(backgroundServiceIntent);
    }

    /**
     * Sends the runItem on server
     */
    private void sendRunData() {
        SendRunItemsAsyncTask task = new SendRunItemsAsyncTask();
        Utils.runAsyncTask(task);
    }

    private void stopRunService() {
        if (mCurrentState != State.Stopped) {
            removeLocationListener();
            mCurrentState = State.Stopped;
            //stop background service
            backgroundServiceIntent = new Intent(RunActivity.this, RunService.class);
            backgroundServiceIntent.setAction(STOP_TRACKER);
            stopService(backgroundServiceIntent);
        }
    }

    /**
     * Register the broadcast receiver to Service intents
     */
    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(CONNECTIVITY_CHANGED_ACTION) || intent.getAction().equals(AIRPLANE_MODE_ACTION)){
                if(Checks.IsNetworkConnected(RunActivity.this)){
                    // hide error label
                    mTextMessage.setVisibility(View.GONE);
                }else if (mCurrentState == State.Running) {
                    // show error label
                    mTextMessage.setText(R.string.run_activity_internet_connection_warning);
                    mTextMessage.setVisibility(View.VISIBLE);
                }
            }else if ( (intent.getAction().equals(GPS_PROVIDER_CHANGED_ACTION))
                    && !Checks.isLocationEnabled(RunActivity.this) ) {
                    Analytics.trackEvent(TAG + " GPS error");
                     showGpsAccuracyWarning();
            } else if (intent.getAction().equals(UPDATE_ACTION)) {
                updateUI(intent);
                updateSensorsData(intent);
            } else if (intent.getAction().equals(KEY_GEN_UPDATE_ACTION)) {
                if (intent.hasExtra(STATE_KEY)) {
                    String event = intent.getStringExtra(STATE_KEY);
                    if (event.equals(STATE_DONE)) {
                        mTextMessage.setVisibility(GONE);
                        // if run is stopped
                        if (mCurrentState == State.Stopped) {
                            createMapSnapshot();
                        }
                    } else if (event.equals(KEY_GEN_STATE_ERROR)) {
                        Analytics.trackEvent(TAG + " Keygen error");
                        showErrorDialog();
                    }
                }
            }
        }
    };

    /**
     * Clears all the variables that are used to encrypt the GPS data
     */
    private void clearGPSVariables() {
        cartesianX.clear();
        cartesianY.clear();
        cartesianZ.clear();
        timeStamps.clear();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        MapUtil.changeMapStyle(TAG, mMap, this);
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_TIME);
        mLocationRequest.setFastestInterval(UPDATE_TIME);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                mMap.setMyLocationEnabled(true);
            }
        } else {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            mMap.setMyLocationEnabled(true);
        }
    }

    /**
     * updates sensors data
     * @param intent the update intent from Service
     */
    private void updateSensorsData(Intent intent){
        float accelerometerMatrix[] = intent.getFloatArrayExtra(ACCELEROMETER_KEY);
        Log.d(TAG,"Accelerometer x = "+accelerometerMatrix[0]+" y = "+accelerometerMatrix[1]+" z = "+accelerometerMatrix[2]);
        accelerometerValues.add(accelerometerMatrix);
        float gyroscopeMatrix[] = intent.getFloatArrayExtra(GYROSCOPE_KEY);
        Log.d(TAG,"Gyroscope x = "+gyroscopeMatrix[0]+" y = "+gyroscopeMatrix[1]+" z = "+gyroscopeMatrix[2]);
        gyroscopeValues.add(gyroscopeMatrix);
    }

    /**
     * This function allows to update UI
     * @param intent the update intent from Service
     */
    private void updateUI(Intent intent) {
        // set elapsed time label
        mTotalTime = intent.getLongExtra(TIME_KEY, 0);
        mElapsedTime = String.format("%02d:%02d",
                TimeUnit.SECONDS.toMinutes(mTotalTime),
                TimeUnit.SECONDS.toSeconds(mTotalTime -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(mTotalTime))
                ));
        if (mElapsedTime != null) {
            // shows the elapsed time on the screen
            mTimeText.setText(mElapsedTime);
        }
        double latitude = intent.getDoubleExtra(LATITUDE_KEY, 0);
        double longitude = intent.getDoubleExtra(LONGITUDE_KEY, 0);

        LatLng latLng = new LatLng(latitude, longitude);
        // update map every second
        if (latitude != 0 && longitude != 0) {
            mLastLocation = latLng;
            Log.e(TAG, latLng.toString());
            updateMap(latLng);
        } else if (mLastLocation != null) {
            Log.e(TAG, mLastLocation.toString());
            updateMap(mLastLocation);
        }
        if(++mFrameUpdateInterval != FRAME_UPDATE_INTERVAL) {
            return;
        }
        mFrameUpdateInterval = 0;
        if (latitude != 0 && longitude != 0) {
            polylinePoints.add(latLng);
            // converting coordinates
            convertCartesian(latLng.latitude, latLng.longitude);
            mLastLocation = latLng;
            Log.e(TAG, latLng.toString());
            updateMap(latLng);
        }
        // add new elevation gain delta
        mDistance = intent.getDoubleExtra(DISTANCE_KEY, 0.0); // return distance in km
        mElevationGainSum += intent.getDoubleExtra(ELEVATION_GAIN_KEY, 0.0);
        elevationGainDeltas.add(intent.getDoubleExtra(ELEVATION_GAIN_KEY, 0.0));
        if (unitMeasure.equals(KILOMETERS_VALUE)) {
            // shows distance in miles
            mDistanceText.setText(String.format("%.1f", mDistance));
            mElevationGainText.setText(String.format("%.1f ", mElevationGainSum));
            mElevationGainText.append(getString(R.string.run_settings_distance_unit_meter));
            // save elevation gain deltas to draw graph
            ApplicationState.getElevationGainDeltas().add(intent.getDoubleExtra(ELEVATION_GAIN_KEY, 0.0));
        } else {
            // shows distance in miles
            mDistanceText.setText(String.format("%.1f", mDistance / MILES_KILOMETERS_CONVERSION_RATE));
            mElevationGainText.setText(String.format("%.1f ", mElevationGainSum / METER_FOOT_CONVERSION_RATE));
            mElevationGainText.append(getString(R.string.run_settings_distance_unit_foot));
            // save elevation gain deltas to draw graph
            ApplicationState.getElevationGainDeltas().add(intent.getDoubleExtra(ELEVATION_GAIN_KEY, 0.0) / METER_FOOT_CONVERSION_RATE);
        }
        if (mDistance > 0.01) {
            double avgPace = 0;
            if (unitMeasure.equals(MILES_VALUE)) {
                double distance = mDistance / MILES_KILOMETERS_CONVERSION_RATE; // convert in Miles
                avgPace = (long) (mTotalTime / distance);
            } else {
                avgPace = (long) (mTotalTime / mDistance);
            }
            // shows the avgPace on the screen
            mRunPace.setText(DateUtil.getFormattedTimeFromSeconds((long) avgPace));
            // save avg pace deltas to draw graph
            avgPaceDeltas.add(avgPace);
        } else {
            avgPaceDeltas.add(0d);
        }

        // Adding timestamp
        if (intent.hasExtra(TIME_KEY)) {
            double timestamp = intent.getDoubleExtra(TIMESTAMP_KEY, 0);
            Log.e(TAG, "TIME_ENTRY : " + timestamp);
            timeStamps.add(timestamp);
        }
    }

    private void updateMap(LatLng latLng) {
        // center map to current location
        autoCenterMap(latLng);
        drawRoute();
    }

    private void autoCenterMap(LatLng latLng) {
        //move map camera
        Point mMapPoint = mMap.getProjection().toScreenLocation(latLng);
        // add y offset to avoid overlay with mDistanceText
        mMapPoint.set(mMapPoint.x, mMapPoint.y + MAP_Y_OFFSET);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(mMap.getProjection().fromScreenLocation(mMapPoint)));
    }

    /**
     * @param latitudeDegrees  latitude in degrees
     * @param longitudeDegrees longitude in degrees
     */
    private void convertCartesian(double latitudeDegrees, double longitudeDegrees) {
        Log.e(TAG, "add coordinates");
        double latitudeRadians = toRadians(latitudeDegrees);
        double longitudeRadians = toRadians(longitudeDegrees);
        cartesianX.add(ServerCalculations.EARTH_RADIUS * sin(longitudeRadians) * cos(latitudeRadians));
        cartesianY.add(ServerCalculations.EARTH_RADIUS * sin(latitudeRadians));
        cartesianZ.add(ServerCalculations.EARTH_RADIUS * cos(longitudeRadians) * cos(latitudeRadians));
    }


    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UPDATE_ACTION);
        intentFilter.addAction(AIRPLANE_MODE_ACTION);
        intentFilter.addAction(GPS_PROVIDER_CHANGED_ACTION);
        intentFilter.addAction(KEY_GEN_UPDATE_ACTION);
        intentFilter.addAction(CONNECTIVITY_CHANGED_ACTION);
        registerReceiver(mBroadcastReceiver, intentFilter);
    }

    /**
     * This function allows to draw a route on a map
     */
    private void drawRoute() {
        if (polylinePoints.size() > 1) {
            mMap.addPolyline(new PolylineOptions()
                    .addAll(polylinePoints)
                    .width(PATH_WIDTH)
                    .color(Color.argb(255, 255, 140, 0))
            );

        }
    }

    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            List<Location> locationList = locationResult.getLocations();
            if (locationList.size() > 0) {
                Location location = locationList.get(locationList.size() - 1);
                if (location.hasAccuracy() && location.getAccuracy() >= MINIMUM_GPS_THRESHOLD) {
                    showGpsAccuracyWarning();
                }
                Log.i(TAG, "Location: " + location.getLatitude() + " " + location.getLongitude());
                //Place current location marker
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                //move map camera
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, MAP_ZOOM));
                Point mMapPoint = mMap.getProjection().toScreenLocation(latLng);
                mMapPoint.set(mMapPoint.x, mMapPoint.y + MAP_Y_OFFSET);
                mMap.moveCamera(CameraUpdateFactory.newLatLng(mMap.getProjection().fromScreenLocation(mMapPoint)));
                // remove GPS listener
                removeLocationListener();
                //}
            }
        }
    };

    /**
     * Stop location updates when Activity is no longer active
     */
    private void removeLocationListener() {
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        removeLocationListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, RunService.class));
        removeLocationListener();
    }

    /**
     * Shows the error and asks the user if to exit or try again
     */
    private void showErrorDialog() {
        DialogInterface.OnClickListener positiveListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // start keygenService
                Intent intent = new Intent(RunActivity.this, KeyGenService.class);
                RunActivity.this.startService(intent);
            }
        };
        DialogInterface.OnClickListener negativeListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // delete user token and info
                CacheUtil.deleteUserTokenAndInfo(RunActivity.this);
                finishActivity(true);
            }
        };
        // show error dialog
        DialogUtil.showDialogPositiveAndNegativeButtons(RunActivity.this, getString(R.string.set_keys_error_title),
                getString(R.string.set_keys_error_message), positiveListener, getString(R.string.retry_button_error),
                negativeListener, getString(R.string.exit_button_error));
    }

    /**
     * Shows the gps warning and asks the user if to exit or continue
     */
    private void showGpsAccuracyWarning() {
        DialogInterface.OnClickListener positiveListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        };
        DialogInterface.OnClickListener negativeListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finishActivity(false);
            }
        };
        // show error dialog
        DialogUtil.showDialogPositiveAndNegativeButtons(RunActivity.this, RunActivity.this.getString(R.string.run_activity_warning_title),
                RunActivity.this.getString(R.string.run_activity_warning_gps_message), positiveListener, "ok",
                negativeListener, RunActivity.this.getString(R.string.exit_button_error));
    }

    /**
     * Shows the gps warning and asks the user if to exit or continue
     */
    private void showBatterySaveModeWarning() {
        DialogInterface.OnClickListener positiveListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // show Android settings
                RunActivity.this.startActivityForResult(new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS), POWER_SAVE_SETTING_REQUEST);
            }
        };
        DialogInterface.OnClickListener negativeListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finishActivity(false);
            }
        };
        // show error dialog
        DialogUtil.showDialogPositiveAndNegativeButtons(RunActivity.this,
                RunActivity.this.getString(R.string.run_activity_warning_title),
                RunActivity.this.getString(R.string.run_activity_warning_battery_save_mode_message),
                positiveListener,
                RunActivity.this.getString(R.string.run_activity_positive_battery_save_mode_text),
                negativeListener, RunActivity.this.getString(R.string.exit_button_error));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == POWER_SAVE_SETTING_REQUEST) {
            if (Checks.isPowerSaveMode(this)) {
                showBatterySaveModeWarning();
            } else {
                startRunService();
            }
        }
    }

    @Override
    public void onBackPressed() {
        // we want to disable back button in this activity
    }

    @Override
    public void onResume() {
        super.onResume();
        // check onResume if the keys are created
        if (ApplicationState.isKeysCreated()) {
            //hide message
            mTextMessage.setVisibility(View.GONE);
        }
    }

    /**
     * @param finishAll if true finish all activities, if false finish only RunActivity
     */
    private void finishActivity(boolean finishAll) {
        // get the activity context
        Context context = RunActivity.this;
        //unregister receiver
        unregisterReceiver(mBroadcastReceiver);
        if (finishAll) {
            // finish this activity
            ((Activity) context).finish();
        } else {
            // finish this activity
            ((Activity) context).finishAffinity();
        }
    }

    private class SendRunItemsAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                // Create a new item
                final RunItem item = new RunItem();

                ServerCalculations calculations = new ServerCalculations(item);
                calculations.processAndEncrypt(cartesianX, cartesianY, cartesianZ, timeStamps, gyroscopeValues,
                        accelerometerValues, elevationGainDeltas, avgPaceDeltas, mElevationGainSum, mMapSnapshot);
                clearGPSVariables();
                Log.e(TAG, "Inserting run data on Server ");
                Analytics.trackEvent("Inserting run data on Server ");
                // Get the encrypted cipher texts as base64 strings
                int runNumber = loadMaxRunNumber(RunActivity.this);
                runNumber++;
                cacheMaxRunNumber(RunActivity.this, runNumber);
                item.setKeyId(ApplicationState.getInsertedKeyItem().getId());
                item.setRunNumber(runNumber);

                //These are fields used to temporarily show the item not yet loaded on the server
                item.setIsLocalItem(true);
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(new Date());
                item.setDate(calendar);
                //item.setCoordinates(polylinePoints);
                item.setTotalDistance(mDistance);
                item.setElevationGain(mElevationGainSum);
                item.setTotalTime(mTotalTime);
                if (mMapSnapshot != null)
                    item.setMapSnapshot(mMapSnapshot);

                // add the current run item in the local memory
                ApplicationState.getLocalRunItems().add(0, item);
                ApplicationState.storeLocalRunItem(item);
                // send data on server
                SyncDataService.startSendDataOnServer(RunActivity.this);
            } catch (final Exception e) {
                Log.e(TAG, "Error inserting run data on Server " + e.getLocalizedMessage());
            }
            return null;
        }
    }
}
