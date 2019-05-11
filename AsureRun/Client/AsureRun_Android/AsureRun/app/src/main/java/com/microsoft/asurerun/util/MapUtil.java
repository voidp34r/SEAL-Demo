// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.asurerun.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.Log;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.microsoft.asurerun.R;

import java.nio.ByteBuffer;


public class MapUtil {
    /**
     * This function allows to customise the styling of the base map using a JSON object defined in a raw resource file.
     * @param TAG the string tag for log
     * @param mMap the google maps object
     * @param context the android context
     */
    public final static int MAP_THUMBNAIL_WIDTH = 64;
    public final static int MAP_THUMBNAIL_HEIGHT = 64;
    public static void changeMapStyle(String TAG,GoogleMap mMap,Context context) {
        try {
            boolean success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.map_run_style));
            if (!success) {
                Log.e(TAG, "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Can't find style. Error: ", e);
        }
    }
}
