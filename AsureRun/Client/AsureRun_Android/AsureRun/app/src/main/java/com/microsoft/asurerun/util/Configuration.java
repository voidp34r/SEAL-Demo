// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.asurerun.util;

import android.content.SharedPreferences;

import com.microsoft.asurerun.BuildConfig;

/**
 * This helper class loads all configuration values from the config.ini file in the assets
 * directory, globally accessible.
 */
public class Configuration {
    // Default values for config file
    public static final String DEFAULT_WEB_HOST = BuildConfig.SERVICE_URL;
    public static final int DEFAULT_POLY_MODULUS = 8192;
    // This is the scale used by SEAL. It was the default value used in all the SEAL samples.
    public static final int DEFAULT_INITIAL_SCALE = 60;

    // Keys for config file lookups
    private static final String WEB_HOST_KEY = "host";
    private static final String POLY_MODULUS_KEY = "poly_modulus";
    private static final String INITIAL_SCALE_KEY = "initial_scale";

    private static String mWebHost;
    private static int mPolyModulus;
    private static int mInitialScale;

    // Getters for the config file
    public static String getWebHost() { return mWebHost; }
    public static int getPolyModulus() { return mPolyModulus; }
    public static int getInitialScale() { return mInitialScale; }

    public static void loadConfigFile(SharedPreferences sharedPreferences) {
        mWebHost = sharedPreferences.getString(WEB_HOST_KEY, DEFAULT_WEB_HOST);
        mPolyModulus = sharedPreferences.getInt(POLY_MODULUS_KEY, DEFAULT_POLY_MODULUS);
        mInitialScale = sharedPreferences.getInt(INITIAL_SCALE_KEY, DEFAULT_INITIAL_SCALE);
    }
}
