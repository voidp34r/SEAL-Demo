// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.asurerun.ui;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.microsoft.asurerun.R;
import com.microsoft.asurerun.model.ApplicationState;
import com.microsoft.asurerun.ui.login.providers.FacebookProvider;
import com.microsoft.asurerun.ui.login.providers.GoogleProvider;
import com.microsoft.asurerun.ui.login.providers.LoginProvider;
import com.microsoft.asurerun.ui.login.providers.MicrosoftProvider;
import com.microsoft.asurerun.util.Configuration;
import com.microsoft.windowsazure.mobileservices.*;
import com.microsoft.windowsazure.mobileservices.http.OkHttpClientFactory;
import com.squareup.okhttp.OkHttpClient;

import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;

/**
 * Activity for handling signing into the application.
 * Supports:
 * - Microsoft accounts
 * - Facebook accounts
 * - Google accounts
 */
public class SignInActivity extends AppCompatActivity {
    /*
     * View variable declaration
     */
    private ProgressBar mProgressBar;
    private TextView mAppVersion;

    public static final String TAG = "SIGN_IN";

    /**
     * Mobile Service Client reference
     */
    private MobileServiceClient mClient;

    /**
     * Login providers
     */
    private LoginProvider mMicrosoftLoginProvider = new MicrosoftProvider();
    private LoginProvider mGoogleLoginProvider = new GoogleProvider();
    private LoginProvider mFacebookLoginProvider = new FacebookProvider();
    private LoginProvider mCurrentProvider = null;

    /**
     * Handle Activity creation. Initializes login providers.
     *
     * @param savedInstanceState instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in_layout);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mAppVersion = (TextView) findViewById(R.id.appVersion);
        try {
            mClient = new MobileServiceClient(Configuration.getWebHost(), this);
            ApplicationState.setMobileClient(mClient);
            // Extend timeout from default of 10s to 200s
            mClient.setAndroidHttpClientFactory(new OkHttpClientFactory() {
                @Override
                public OkHttpClient createOkHttpClient() {
                    OkHttpClient client = new OkHttpClient();
                    client.setReadTimeout(200, TimeUnit.SECONDS);
                    client.setWriteTimeout(200, TimeUnit.SECONDS);
                    return client;
                }
            });
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        mMicrosoftLoginProvider.configureLogin(this, mClient);
        mGoogleLoginProvider.configureLogin(this, mClient);
        mFacebookLoginProvider.configureLogin(this, mClient);

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            mAppVersion.append(version);

        } catch (PackageManager.NameNotFoundException e) {
            mAppVersion.setText("Failed to get app version");
        }
    }

    /**
     * Get progress bar
     */
    public ProgressBar getProgressBar() {
        return mProgressBar;
    }

    /**
     * Set current login provider. Called from the buttons when pressed by the user.
     */
    public void setCurrentProvider(LoginProvider provider) {
        mCurrentProvider = provider;
    }

    /**
     * Handle activity result. Handles login result from the different providers.
     *
     * @param requestCode result request code
     * @param resultCode actual result code
     * @param data additional result data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e(TAG, "Result code := " + resultCode);
        if (mCurrentProvider != null) {
            mCurrentProvider.onActivityResult(requestCode, resultCode, data);
        } else {
            Log.e(TAG, "Current provider not set");
        }
    }

    /**
     * Handle pressing of back button
     */
    @Override
    public void onBackPressed() {
        // we want to disable back button in this activity
    }
}
