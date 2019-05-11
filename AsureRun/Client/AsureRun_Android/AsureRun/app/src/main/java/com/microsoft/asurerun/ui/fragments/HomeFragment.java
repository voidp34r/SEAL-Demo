// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.asurerun.ui.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;

import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.microsoft.asurerun.R;
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
import com.microsoft.asurerun.model.ApplicationState;
import com.microsoft.asurerun.service.KeyGenService;
import com.microsoft.asurerun.ui.RunActivity;
import com.microsoft.asurerun.util.CacheUtil;
import com.microsoft.asurerun.util.Checks;
import com.microsoft.asurerun.util.DialogUtil;

import java.util.List;

import static com.microsoft.asurerun.service.KeyGenService.STATE_KEY;
import static com.microsoft.asurerun.util.DialogUtil.showAndroidNetworkError;
import static com.microsoft.asurerun.util.ServiceUtil.*;


public class HomeFragment extends Fragment implements OnMapReadyCallback {
    // google maps variable
    private GoogleMap mMap; // TODO: we need to investigate if data is being sent to google in plain text.

    // View variable declaration
    private ImageButton mStartButton;
    private SupportMapFragment mapFragment;
    private FragmentActivity myContext;
    private TextView mMessage;

    private LocationRequest mLocationRequest;
    FusedLocationProviderClient mFusedLocationClient;

    public final static String TAG = "HomeFragment";

    private final static int MAP_ZOOM = 18;
    private final static long UPDATE_TIME = 5000;


    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment HomeFragment.
     */
    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public void onResume() {
        super.onResume();
        // check onResume if the keys are created
        if (ApplicationState.isKeysCreated()) {
            //hide message
            mMessage.setVisibility(View.GONE);
        }
        //register broadcast receiver
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(KEY_GEN_UPDATE_ACTION);
        intentFilter.addAction(AIRPLANE_MODE_ACTION);
        intentFilter.addAction(GPS_PROVIDER_CHANGED_ACTION);
        intentFilter.addAction(CONNECTIVITY_CHANGED_ACTION);
        getActivity().registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        //unregister broadcast receiver
        getActivity().unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_home, container, false);
        mStartButton = (ImageButton) v.findViewById(R.id.start);
        mMessage = (TextView) v.findViewById(R.id.textMessage);
        if (ApplicationState.isKeysCreated()) {
            //hide message
            mMessage.setVisibility(View.GONE);
        }
        // activity context
        myContext = this.getActivity();
        // add title to toolbar
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.title_fragment_home);
        if (myContext != null) {
            checkIsLocationEnabled();
        }
        return v;
    }

    /**
     * Method to check whether the Location is enabled
     */
    private void checkIsLocationEnabled() {
        if (Checks.isLocationEnabled(myContext)) {
            //go ahead and start the Location Client
            startLocationClient();
        } else {
            // show Dialog error message
            final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(myContext);
            builder.setMessage(getString(R.string.location_error_message));
            builder.setTitle(getString(R.string.location_error_title));
            builder.setPositiveButton("RETRY", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (Checks.isLocationEnabled(myContext)) {
                        startLocationClient();
                    } else {
                        checkIsLocationEnabled();
                    }
                }
            });
            builder.setCancelable(false);
            builder.setNegativeButton("EXIT", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    HomeFragment.this.myContext.finish();
                }
            });
            builder.create().show();
        }
    }

    private void startLocationClient() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(myContext);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //start run activity
                Intent myIntent = new Intent(myContext, RunActivity.class);
                myContext.startActivity(myIntent);
                stopLocationUpdate();
            }
        });

    }

    /**
     * Register the broadcast receiver to KeyGenService intents
     */
    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(AIRPLANE_MODE_ACTION) || intent.getAction().equals(GPS_PROVIDER_CHANGED_ACTION) ||
                    intent.getAction().equals(CONNECTIVITY_CHANGED_ACTION)) {
                if (!Checks.isLocationEnabled(getActivity()) || !Checks.IsNetworkConnected(getActivity())) {
                    showAndroidNetworkError(context);
                }
            } else if (intent.hasExtra(STATE_KEY)) {
                String event = intent.getStringExtra(STATE_KEY);
                // set UI
                Log.e(TAG, event);
                if (event.equals(KEY_GEN_STATE_ERROR)) {
                    showErrorDialog();
                }
                if (event.equals(STATE_DONE)) {
                    // hide message
                    mMessage.setVisibility(View.GONE);
                }
            }
        }
    };


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_TIME);
        mLocationRequest.setFastestInterval(UPDATE_TIME);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(myContext,
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
     *
     */
    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            List<Location> locationList = locationResult.getLocations();
            if (!locationList.isEmpty()) {
                Location location = locationList.get(locationList.size() - 1);
                Log.i(TAG, "Location: " + location.getLatitude() + " " + location.getLongitude());
                //Place current location marker
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                //move map camera
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, MAP_ZOOM));
            }
        }
    };

    /**
     * //stop location updates when Fragment is no longer active
     */
    private void stopLocationUpdate() {
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    /**
     * Shows the error and asks the user if to exit or try again
     */
    private void showErrorDialog() {
        DialogInterface.OnClickListener positiveListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // start keygenService
                Intent intent = new Intent(getActivity(), KeyGenService.class);
                getActivity().startService(intent);
            }
        };
        DialogInterface.OnClickListener negativeListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // get the activity context
                Context context = HomeFragment.this.getActivity();
                // delete user token and info
                CacheUtil.deleteUserTokenAndInfo(context);
                // finish this activity
                ((Activity) context).finish();
            }
        };
        // show error dialog
        DialogUtil.showDialogPositiveAndNegativeButtons(getActivity(), getString(R.string.set_keys_error_title),
                getString(R.string.set_keys_error_message), positiveListener, getString(R.string.retry_button_error),
                negativeListener, getString(R.string.exit_button_error));
    }


}
