// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.asurerun.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.microsoft.asurerun.R;

import java.io.File;
import static com.microsoft.asurerun.model.ApplicationState.getKeyIdFile;

public class Utils {
    private final static String TAG = Utils.class.getSimpleName();
    /**
     * Run an ASync task on the corresponding executor
     *
     * @param task
     * @return
     */
    public static AsyncTask<Void, Void, Void> runAsyncTask(AsyncTask<Void, Void, Void> task) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            return task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            return task.execute();
        }
    }

    /**
     *
     * @param context the view context
     * @return the size of key
     */
    public static long getKeyDimension (Context context){
        File file = new File( context.getFilesDir().getAbsolutePath(), getKeyIdFile());
        long size = 0;
        if (file.exists()) {
            try {
                size = file.length();
            } catch (Exception e) {
                Log.e(TAG,e.getMessage());
            }
        }
        return size;
    }

    /**
     *
     * @param context the view context
     * @return
     */
    public static String getBuildNumber(Context context){
        String version;
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            version = pInfo.versionName;

        } catch (PackageManager.NameNotFoundException e) {
           version = "Failed to get app version";
        }
        return version;
    }

    /**
     *
     * @param intensity The intensity of the run.
     * @return The intensity as a string as defined in the app resources.
     */
    public static String determineIntensity(Resources resources, double intensity) {
        // The values are from actual run trials. These are subject to change based on updated models.
        if(intensity > 0.3)
            return resources.getString(R.string.classification_low);
        else if(intensity < 0.1)
            return resources.getString(R.string.classification_high);
        else
            return resources.getString(R.string.classification_medium);
    }
}
