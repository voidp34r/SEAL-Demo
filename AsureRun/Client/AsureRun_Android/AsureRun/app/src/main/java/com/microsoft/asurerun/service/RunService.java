// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.asurerun.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.microsoft.asurerun.R;
import com.microsoft.asurerun.ui.RunActivity;
import com.microsoft.asurerun.util.ServiceUtil;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.hardware.Sensor.TYPE_ACCELEROMETER;
import static android.hardware.Sensor.TYPE_GYROSCOPE;
import static com.microsoft.asurerun.util.ServiceUtil.*;

public class RunService extends Service implements SensorEventListener {
    /**
     * Constants declaration
     */
    public final static double MIN_DISTANCE_UPDATE = 1.75; // in meters
    public final static double MIN_ACCURACY_GPS = 21; // in meters
    public final static double MIN_ELEVATION_GAIN_DELTA_UPDATE = 3; // in meters
    private final long UPDATE_TIME = 2000; // in milliseconds
    private static final String LOG_TAG = "RunTracker";
    private static final int FOREGROUND_ID = 1338;
    private static final String NOTIFICATION_CHANNEL_ID = "Asure_run_Bg_notification";
    private static final String NOTIFICATION_CHANNEL_NAME = "Bg_notification";
    private static final String NOTIFICATION_CHANNEL_DESCRIPTION = "Notification to startForeground service";
    private double currentAltitude;
    private double maxAltitude;
    private SensorManager mSensorManager;
    private float[] mAccelerometerMatrix = new float[3];
    private float[] mGyroscopeMatrix = new float[3];


    /**
     * Android Timer reference
     */
    private Timer timer;
    /**
     * Elapsed time reference
     */
    private long mElapsedTime;
    /**
     * User current location and last location reference
     */
    private Location currentLocation, mLastLocation;
    private double distance;
    private LocationRequest mLocationRequest;

    /**
     * Android Location Provider Client reference
     */
    private FusedLocationProviderClient mFusedLocationClient;

    @Override
    public void onCreate() {
        super.onCreate();
        timer = new Timer();
        mElapsedTime = 0;
        distance = 0.0;
        maxAltitude = 0;
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);

    }

    private void startService() {
        startForeground(FOREGROUND_ID, buildForegroundNotification());
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_GAME);
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
            }
        } else {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        }
        timer.scheduleAtFixedRate(new mainTask(), 0, 1000);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return Service.START_NOT_STICKY;
        }
        final String requestAction = intent.getAction();
        if (ServiceUtil.START_TRACKER.equals(requestAction)) {
            startService();
        } else if (ServiceUtil.STOP_TRACKER.equals(requestAction)) {
            this.stopSelf();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor sensor = sensorEvent.sensor;
        if (sensor.getType() == TYPE_ACCELEROMETER) {
            mAccelerometerMatrix = sensorEvent.values;
        } else if (sensor.getType() == TYPE_GYROSCOPE) {
            mGyroscopeMatrix = sensorEvent.values;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private class mainTask extends TimerTask {
        public void run() {
            updateHandler.sendEmptyMessage(0);
        }
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "stopped");
        super.onDestroy();
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
        mSensorManager.unregisterListener(this);
        timer.cancel();
    }

    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            List<Location> locationList = locationResult.getLocations();
            if (locationList.size() > 0) {
                if (currentLocation == null) {
                    currentLocation = locationList.get(locationList.size() - 1);
                } else {
                    mLastLocation = currentLocation;
                    currentLocation = locationList.get(locationList.size() - 1);
                    if (mLastLocation.distanceTo(currentLocation) > MIN_DISTANCE_UPDATE && currentLocation.getAccuracy() < MIN_ACCURACY_GPS)
                        distance += mLastLocation.distanceTo(currentLocation) * KILOMETER_METER_CONVERSION_RATE; // return distance in meter and then it is converted in kilometers
                }
            }
        }
    };

    /**
     * @return the Notification required for the startForeground method
     */
    private Notification buildForegroundNotification() {
        Intent notificationIntent = new Intent(this, RunActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_tracking)
                .setContentTitle("Asurerun").setContentIntent(pendingIntent)
                .setContentText("Asurerun is tracking your run in background");
        Notification notification = builder.build();
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(NOTIFICATION_CHANNEL_DESCRIPTION);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
            notification = new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setContentTitle("Asurerun")
                    .setContentText("Asurerun is tracking your run in background").setContentIntent(pendingIntent)
                    .setSmallIcon(R.drawable.ic_tracking)
                    .build();
        }
        return notification;
    }

    private final Handler updateHandler = new UpdateHandler(this);

    /**
     *
     */
    private static class UpdateHandler extends Handler {
        private WeakReference<RunService> mServiceRef;

        public UpdateHandler(final RunService service) {
            this.mServiceRef = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            final RunService service = mServiceRef.get();
            // increment elapsed time
            // TODO: this shouldn't be the way to keep track of time, need to use the system clock to be more accurate
            service.mElapsedTime++;

            Intent intent = new Intent();
            intent.setAction(UPDATE_ACTION);

            // send location changes
            if (service.currentLocation != null && service.mLastLocation == null) {
                // added first location
                intent.putExtra(LATITUDE_KEY, service.currentLocation.getLatitude());
                intent.putExtra(LONGITUDE_KEY, service.currentLocation.getLongitude());
                if (service.currentLocation.hasAltitude()) {
                    service.currentAltitude = service.currentLocation.getAltitude();
                }
            } else if (service.currentLocation != null &&
                    service.mLastLocation != null && service.mLastLocation.distanceTo(service.currentLocation) > MIN_DISTANCE_UPDATE
                    && service.currentLocation.getAccuracy() < MIN_ACCURACY_GPS) {
                Log.d(LOG_TAG, service.currentLocation.toString());
                intent.putExtra(LATITUDE_KEY, service.currentLocation.getLatitude());
                intent.putExtra(LONGITUDE_KEY, service.currentLocation.getLongitude());
                // send elevation delta
                if (service.currentLocation.hasAltitude()) {
                    double currentAltitude = (service.currentLocation.getAltitude() - service.currentAltitude);
                    service.currentAltitude = service.currentLocation.getAltitude();
                    if(currentAltitude > service.maxAltitude){
                        service.maxAltitude = currentAltitude;
                    }
                    double elevationGain = currentAltitude - service.maxAltitude;
                    if (elevationGain <= MIN_ELEVATION_GAIN_DELTA_UPDATE) {
                        intent.putExtra(ELEVATION_GAIN_KEY, Math.max(0.0, elevationGain));
                    }
                } else {
                    intent.putExtra(ELEVATION_GAIN_KEY, 0.0);
                }
            }
            // send accelerometer values
            intent.putExtra(ACCELEROMETER_KEY, service.mAccelerometerMatrix);
            // send gyroscope values
            intent.putExtra(GYROSCOPE_KEY, service.mGyroscopeMatrix);
            // send location timestamp
            intent.putExtra(TIMESTAMP_KEY, getTimeSinceMidnight(Calendar.getInstance().getTimeInMillis()));
            // update UI with time elapsed and distance traveled
            intent.putExtra(DISTANCE_KEY, service.distance);
            intent.putExtra(TIME_KEY, service.mElapsedTime);
            service.sendBroadcast(intent);
        }

        /**
         * Returns the timestamp  from the beginning of the day in order to keep
         * the numbers small enough for the cipher calculations.
         *
         * @param timestamp current timestamp
         * @return the time in seconds
         */
        private double getTimeSinceMidnight(long timestamp) {
            Calendar midnight = Calendar.getInstance();

            midnight.set(Calendar.HOUR_OF_DAY, 0);
            midnight.set(Calendar.MINUTE, 0);
            midnight.set(Calendar.SECOND, 0);
            midnight.set(Calendar.MILLISECOND, 0);
            long time = Math.round((timestamp - midnight.getTimeInMillis()) / 1000);
            return time;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

