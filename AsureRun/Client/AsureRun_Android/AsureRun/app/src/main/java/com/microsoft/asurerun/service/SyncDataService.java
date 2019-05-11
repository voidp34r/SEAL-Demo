// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.asurerun.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.asurerun.R;
import com.microsoft.asurerun.model.ApplicationState;
import com.microsoft.asurerun.model.RunItem;
import com.microsoft.asurerun.util.Utils;
import com.microsoft.windowsazure.mobileservices.MobileServiceException;
import com.microsoft.windowsazure.mobileservices.MobileServiceList;
import com.microsoft.windowsazure.mobileservices.table.query.QueryOrder;

import static com.microsoft.asurerun.model.ApplicationState.INITIAL_ITEMS_OFFSET;
import static com.microsoft.asurerun.util.ServiceUtil.*;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * helper methods.
 */
public class SyncDataService extends IntentService {
    private final static String TAG = "RFS_SERV";
    private static final int NOTIFICATION_ID = 1088;
    private static final String NOTIFICATION_CHANNEL_ID = "com.microsoft.asurerun.notification.SyncDataId";
    private static final String NOTIFICATION_CHANNEL_NAME = "com.microsoft.asurerun.notification.channel.SyncDataChannel";
    private static final String NOTIFICATION_CHANNEL_DESCRIPTION = "Notification to sync data service";

    public SyncDataService() {
        super("RefreshItemsService");
    }

    /**
     * Starts this service to perform action REFRESH_ITEMS_ACTION. If
     * the service is already performing a task this action will be queued.
     *
     * @param context the activity context
     * @see IntentService
     */
    public static void startRefreshItems(Context context) {
        Intent intent = new Intent(context, SyncDataService.class);
        intent.setAction(REFRESH_ITEMS_ACTION);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action DELETE_DATA_ON_FIRST_START_ACTION. If
     * the service is already performing a task this action will be queued.
     *
     * @param context the activity context
     * @see IntentService
     */
    public static void startDeleteAllDataOnFirstStart(Context context) {
        Intent intent = new Intent(context, SyncDataService.class);
        intent.setAction(DELETE_DATA_ON_FIRST_START_ACTION);
        context.startService(intent);
    }


    /**
     * Starts this service to perform action REFRESH_ITEMS_AND_RUN_NUMBER_ACTION. If
     * the service is already performing a task this action will be queued.
     *
     * @param context the activity context
     * @see IntentService
     */
    public static void startSendDataOnServer(Context context) {
        Intent intent = new Intent(context, SyncDataService.class);
        intent.setAction(SEND_ITEM_ACTION);
        context.startService(intent);
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (REFRESH_ITEMS_ACTION.equals(action)) {
                Log.d(TAG, "Refresh items START");
                Analytics.trackEvent(TAG + " Refresh run items START");
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
                notificationManager.notify(NOTIFICATION_ID, buildNotification("AsureRun", "Syncing data...", R.drawable.ic_tracking, null));
                handleRefreshItems(notificationManager, false);
            } else if (DELETE_DATA_ON_FIRST_START_ACTION.equals(action)) {
                Log.d(TAG, "Delete data on first start");
                Analytics.trackEvent(TAG + " Delete data on first start");
                handleDeleteDataOnFirstStart();
            } else if (SEND_ITEM_ACTION.equals(action)) {
                Log.d(TAG, "Sending data on server");
                handleSendDataOnServer();
            }
        }
    }

    /**
     * Handle action handleDeleteDataOnFirstStart in the provided background thread
     */
    private void handleDeleteDataOnFirstStart() {
        ListenableFuture<MobileServiceList<RunItem>> items = null;
        try {
            items = ApplicationState.getRunTable().execute();
            Futures.addCallback(items, new FutureCallback<MobileServiceList<RunItem>>() {
                @Override
                public void onSuccess(MobileServiceList<RunItem> result) {
                    for (RunItem item : result) {
                        ApplicationState.getRunTable().delete(item);
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    Log.e(TAG, t.getMessage());
                }
            });
        } catch (MobileServiceException e) {
            e.printStackTrace();
        }

    }

    /**
     * Handle action SendDataOnServer in the provided background thread
     */
    private void handleSendDataOnServer() {
        RunItem item = ApplicationState.getLocalRunItems().get(0);
        sendRunItem(item);
    }

    /**
     * The real worker function that sends the run item.
     *
     * @param item The RunItem to send
     */
    private void sendRunItem(RunItem item) {
        // send item on server
        ListenableFuture<RunItem> listenableInsert = ApplicationState.getRunTable().insert(item);
        Futures.addCallback(listenableInsert, new FutureCallback<RunItem>() {
            @Override
            public void onSuccess(RunItem result) {
                Log.e(TAG, "Inserting item ok");
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(SyncDataService.this);
                notificationManager.notify(NOTIFICATION_ID, buildNotification("AsureRun", "Syncing data...", R.drawable.ic_tracking, null));
                handleRefreshItems(notificationManager, true);
                ApplicationState.deleteLocalRunItem(result);
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Error inserting run data on Server " + t.getMessage());
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(SyncDataService.this);
                //todo add retry action
                notificationManager.notify(NOTIFICATION_ID,
                        buildNotification("AsureRun", "An error occurred while sending data.", R.drawable.ic_tracking, null));
            }
        });
    }

    /**
     * Handle action RefreshItems in the provided background thread
     */
    private void handleRefreshItems(NotificationManagerCompat notificationManager, boolean loadFirstTopItem) {
        RefreshItemsAsync task = new RefreshItemsAsync(notificationManager, loadFirstTopItem);
        Utils.runAsyncTask(task);
    }

    private Notification buildNotification(String title, String content, int icon, NotificationCompat.Action action) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_tracking)
                .setContentTitle(title)
                .setContentText(content);
        if (action != null) {
            builder.addAction(action);
        }
        Notification notification = builder.build();
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(NOTIFICATION_CHANNEL_DESCRIPTION);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannel(channel);
            notification = new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setSmallIcon(icon)
                    .build();
        }
        return notification;
    }


    private void sendRefreshItemsFinished() {
        Intent intent = new Intent();
        intent.setAction(REFRESH_ITEM_FINISHED_ACTION);
        sendBroadcast(intent);
    }

    private class RefreshItemsAsync extends AsyncTask<Void, Void, Void> {
        private NotificationManagerCompat mNotificationManager;
        private List<RunItem> mRunItems;
        private boolean mLoadFirstTopItem;

        public RefreshItemsAsync(final NotificationManagerCompat notificationManager, boolean loadFirstTopItem) {
            mNotificationManager = notificationManager;
            mLoadFirstTopItem = loadFirstTopItem;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Log.e(TAG, "Start refresh items");
                int offset;
                if (mLoadFirstTopItem) {
                    offset = 1; // we refresh only first top item
                } else {
                    offset = INITIAL_ITEMS_OFFSET; // load the initial number of items
                }
                mRunItems = ApplicationState.getRunTable().top(offset).orderBy("runNumber", QueryOrder.Descending).execute().get();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, e.getMessage());
                mNotificationManager.cancel(NOTIFICATION_ID);
                mNotificationManager = null;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (mRunItems != null) {
                if (mLoadFirstTopItem) {
                    // remove local item
                    ApplicationState.getLocalRunItems().remove(0);
                    // add remote item
                    ApplicationState.getLocalRunItems().add(0,mRunItems.get(0));
                } else {
                    ApplicationState.getLocalRunItems().clear();
                    ApplicationState.getLocalRunItems().addAll(mRunItems);
                }
                sendUnsentRunItems();
                sendRefreshItemsFinished();
                Map<String, String> properties = new HashMap<>();
                properties.put("Items", mRunItems.toString());
                Analytics.trackEvent(TAG + " Refresh run items FINISH", properties);
                if (mNotificationManager != null) {
                    mNotificationManager.cancel(NOTIFICATION_ID);
                }
            }

            mNotificationManager = null;
        }

        private void sendUnsentRunItems() {
            if (ApplicationState.getInsertedKeyItem() != null) {
                RunItem[] localDbItems = ApplicationState.getLocalRunItemsDb();
                for (RunItem localItem : localDbItems) {
                    boolean match = false;
                    for (RunItem serverItem : mRunItems) {
                        if (localItem.getRunNumber() == serverItem.getRunNumber()) {
                            match = true;
                            ApplicationState.deleteLocalRunItem(localItem);
                            break;
                        }
                    }
                    if (!match) {
                        localItem.setKeyId(ApplicationState.getInsertedKeyItem().getId());
                        sendRunItem(localItem);
                    }
                }
            }
        }
    }
}
