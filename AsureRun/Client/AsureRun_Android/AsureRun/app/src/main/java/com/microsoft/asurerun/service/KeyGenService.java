// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.asurerun.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.microsoft.asurerun.model.ApplicationState;
import com.microsoft.asurerun.util.Configuration;
import com.microsoft.asurerun.util.Utils;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;

import static com.microsoft.asurerun.util.ServiceUtil.*;


public class KeyGenService extends Service {
    private final static String TAG = "KEYGEN_SER";
    public final static String STATE_KEY = "STKEY";
    public final static int TIMER_PERIOD = 60000; // one minute
    /**
     * Android Timer reference
     */
    private Timer mTimer;
    private int mRemainingTime; // in minutes

    public KeyGenService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        asyncCreateCryptoContext();
        mRemainingTime = 6; // Note: this data is the result of test measurements
        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new mainTask(), TIMER_PERIOD, TIMER_PERIOD);
        return START_NOT_STICKY;
    }

    private class mainTask extends TimerTask {
        public void run() {
            updateHandler.sendEmptyMessage(0);
        }
    }

    private final Handler updateHandler = new UpdateHandler(this);

    private static class UpdateHandler extends Handler {
        private WeakReference<KeyGenService> mServiceRef;

        public UpdateHandler(final KeyGenService service) {
            this.mServiceRef = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            final KeyGenService service = mServiceRef.get();
            service.mRemainingTime--;
            if (service.mRemainingTime >= 0) {
                Log.e(TAG, "update time");
                Intent intent = new Intent();
                intent.setAction(KEY_GEN_UPDATE_ACTION);
                intent.putExtra(KEY_GEN_TIME_KEY, service.mRemainingTime);
                service.sendBroadcast(intent);
            }
        }
    }

    private void asyncCreateCryptoContext() {

        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected void onPreExecute() {
                Log.e(TAG, "Keygen service START");
            }

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    Log.e(TAG, "Keygen service RUN");
                    ApplicationState.createCryptoContext(KeyGenService.this, getFilesDir().getAbsolutePath(), Configuration.getPolyModulus(), Configuration.getInitialScale(), new OnTaskListener() {
                        @Override
                        public void onTaskCompleted(String event) {
                            Intent intent = new Intent();
                            intent.setAction(KEY_GEN_UPDATE_ACTION);
                            intent.putExtra(STATE_KEY, event);
                            Log.e(TAG, event);
                            KeyGenService.this.sendBroadcast(intent);
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error creating crypto context: " + e.getMessage());
                    //send error state
                    sendError();
                    //cancel async task
                    this.cancel(true);
                    KeyGenService.this.stopSelf();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void v) {
                // send broadcast intent when gen key is finished
                ApplicationState.setIsKeysCreated(true);
                Intent intent = new Intent();
                intent.setAction(KEY_GEN_UPDATE_ACTION);
                intent.putExtra(STATE_KEY, STATE_DONE);
                Log.e(TAG, STATE_DONE);
                KeyGenService.this.sendBroadcast(intent);
                KeyGenService.this.mTimer.cancel();
                KeyGenService.this.stopSelf();
            }
        };

        Utils.runAsyncTask(task);
    }

    /**
     * sends a broadcast intent with error
     */
    private void sendError() {
        Intent intent = new Intent();
        intent.setAction(KEY_GEN_UPDATE_ACTION);
        intent.putExtra(STATE_KEY, KEY_GEN_STATE_ERROR);
        Log.e(TAG, KEY_GEN_STATE_ERROR);
        KeyGenService.this.sendBroadcast(intent);
    }
}


