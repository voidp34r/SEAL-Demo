// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.asurerun.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import java.util.regex.Pattern;

public class Checks {
    /**
     * Method to check whether the Internet is Connected
     */
    public static boolean IsNetworkConnected(Context context) {
        if (context != null) {
            ConnectivityManager connMgr = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            return (networkInfo != null && networkInfo.isConnected());
        } else {
            return false;
        }
    }

    /**
     * Method to check whether the Location is enabled
     */
    public static boolean isLocationEnabled(Context context) {
        int locationMode = 0;
        try {
            locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);

        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
            return false;
        }

        return locationMode != Settings.Secure.LOCATION_MODE_OFF;
    }

    /**
     * @param stringBase64
     * @return true if input is a valid Base64, false otherwise.
     */
    public static boolean isBase64(String stringBase64) {
        String regex = "([A-Za-z0-9+/]{4})*" + "([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)";
        Pattern patron = Pattern.compile(regex);
        if (stringBase64 == null || !patron.matcher(stringBase64).matches()) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * @param context the activity context
     * @return true if the power save mode is on, false otherwise
     */
    public static boolean isPowerSaveMode(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager != null && powerManager.isPowerSaveMode())
            return true;
        return false;
    }

}
