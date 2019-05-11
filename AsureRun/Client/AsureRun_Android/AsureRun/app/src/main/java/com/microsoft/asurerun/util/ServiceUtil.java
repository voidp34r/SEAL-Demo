// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.asurerun.util;

public class ServiceUtil {
    /**
     * Broadcast settings constants
     */
    public final static String AIRPLANE_MODE_ACTION = "android.intent.action.AIRPLANE_MODE";
    public final static String GPS_PROVIDER_CHANGED_ACTION = "android.location.PROVIDERS_CHANGED";
    public final static String CONNECTIVITY_CHANGED_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";
    /**
     * RunService constants
     */
    public final static double KILOMETER_METER_CONVERSION_RATE = 0.001;
    public final static double METER_FOOT_CONVERSION_RATE = 3.280839895;
    public final static double MILES_KILOMETERS_CONVERSION_RATE = 1.60934;
    public final static String UPDATE_ACTION = "com.microsoft.asurerun.action.TIME_ACTION";
    public final static String LATITUDE_KEY = "com.microsoft.asurerun.key.LAT";
    public final static String LONGITUDE_KEY = "com.microsoft.asurerun.key.LNG";
    public final static String TIMESTAMP_KEY = "com.microsoft.asurerun.key.TMS";
    public final static String ACCELEROMETER_KEY = "com.microsoft.asurerun.key.ACCLRTR";
    public final static String GYROSCOPE_KEY = "com.microsoft.asurerun.key.GYRSCP";
    public final static String RUN_COORDINATES_EXTRA_KEY = "com.microsoft.asurerun.key.RCOOEXT";
    public final static String ELEVATION_GAIN_KEY = "com.microsoft.asurerun.key.ELG";
    public final static String TIME_KEY = "com.microsoft.asurerun.key.TIME";
    public final static String DISTANCE_KEY = "com.microsoft.asurerun.key.DISTANCE";
    public final static String DISTANCE_SUMMARY_KEY = "com.microsoft.asurerun.key.DISTANCE_SMY_KEY";
    public final static String ELEVATION_GAIN_SUMMARY_KEY = "com.microsoft.asurerun.key.ELEVATION_SMY_KEY";
    public final static String TIME_SUMMARY_KEY = "com.microsoft.asurerun.key.TIME_SMY_KEY";
    public final static String START_TRACKER = "com.microsoft.asurerun.action.START";
    public final static String STOP_TRACKER = "com.microsoft.asurerun.action.STOP";
    /**
     * KeygenService constants
     */
    public final static String KEY_GEN_UPDATE_ACTION = "com.microsoft.asurerun.action.KEY_GEN_STATE_ACTION";
    public final static String STATE_PUBLIC = "com.microsoft.asurerun.state.publicKey";
    public final static String STATE_PRIVATE = "com.microsoft.asurerun.state.privateKey";
    public final static String STATE_CLOUD = "com.microsoft.asurerun.state.cloudKey";
    public final static String STATE_DONE = "com.microsoft.asurerun.state.KeyGenFinish";
    public final static String KEY_GEN_STATE_ERROR = "com.microsoft.asurerun.state.KeyGenError";
    public final static String KEY_GEN_TIME_KEY = "com.microsoft.asurerun.key.keyGenTmKey";
    /**
     * RefreshItemsService constants
     */
    public final static String REFRESH_ITEMS_ACTION = "com.microsoft.asurerun.action.RefreshItems";
    public final static String DELETE_DATA_ON_FIRST_START_ACTION = "com.microsoft.asurerun.action.DeleteDataOnFirstStart";
    public final static String SEND_ITEM_ACTION = "com.microsoft.asurerun.action.SendItemToServer";
    public final static String REFRESH_ITEM_FINISHED_ACTION = "com.microsoft.asurerun.action.RefreshItemFinished";
}