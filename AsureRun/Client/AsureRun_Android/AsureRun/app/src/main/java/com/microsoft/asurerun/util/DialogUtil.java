// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.asurerun.util;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;

import com.microsoft.asurerun.R;


import static com.microsoft.appcenter.utils.HandlerUtils.runOnUiThread;

public class DialogUtil {

    /**
     * shows dialog with the error message with positive and negative buttons
     *
     * @param context            the activity context
     * @param title              the dialog title
     * @param message            the dialog message
     * @param positiveListener   the OnClickListener for positive button
     * @param positiveButtonText the text for positive button
     * @param negativeListener   the OnClickListener for negative button
     * @param negativeButtonText the text for negative button
     */
    public static void showDialogPositiveAndNegativeButtons(final Context context, String title, String message, DialogInterface.OnClickListener positiveListener,
                                                            String positiveButtonText, DialogInterface.OnClickListener negativeListener, String negativeButtonText) {
        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setMessage(message);
        builder.setTitle(title);
        builder.setPositiveButton(positiveButtonText, positiveListener);
        builder.setNegativeButton(negativeButtonText, negativeListener);
        builder.setCancelable(false);
        builder.create().show();
    }

    /**
     * Creates a dialog and shows it
     *
     * @param message The dialog message
     * @param title   The dialog title
     * @param context The activity context
     */
    public static void createAndShowDialog(final String message, final String title, @NonNull Context context) {
        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);

        builder.setMessage(message);
        builder.setTitle(title);
        builder.create().show();
    }

    /**
     * Creates a dialog and shows it
     *
     * @param exception The exception to show in the dialog
     * @param title     The dialog title
     * @param context   The activity context
     */
    public static void createAndShowDialog(Exception exception, String title, @NonNull Context context) {
        Throwable ex = exception;
        if (exception.getCause() != null) {
            ex = exception.getCause();
        }
        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setMessage(ex.getMessage());
        builder.setTitle(title);
        builder.create().show();
    }

    /**
     * Creates a dialog and shows it
     *
     * @param exception The exception to show in the dialog
     * @param title     The dialog title
     */
    public static void createAndShowDialogFromTask(final Exception exception, final String title, @NonNull final Context context) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                createAndShowDialog(exception, title, context);
            }
        });
    }

    /**
     * Creates a dialog and shows it
     *
     * @param context the activity context
     */
    public static void showAndroidNetworkError(final Context context) {

        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setMessage(((Activity) context).getString(R.string.network_error_message));
        builder.setTitle(((Activity) context).getString(R.string.network_error_title));
        builder.setPositiveButton("RETRY", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (!Checks.IsNetworkConnected(context))
                    showAndroidNetworkError(context);
            }
        });
        builder.setCancelable(false);
        builder.setNegativeButton("EXIT", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ((Activity) context).finish();
            }
        });
        builder.create().show();
    }
    /**
     * Creates a dialog with warning message and shows it
     *
     * @param message The dialog message
     * @param context The activity context
     */
    public static void showWarningDialog(final Context context, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Warning");
        builder.setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        // Create the AlertDialog object and show it
        builder.create().show();
    }
}
