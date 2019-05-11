// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.asurerun.ui;


import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.util.concurrent.ListenableFuture;
import com.microsoft.asurerun.BuildConfig;
import com.microsoft.asurerun.model.ApplicationState;
import com.microsoft.asurerun.R;
import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.crashes.Crashes;
import com.microsoft.appcenter.distribute.Distribute;

import com.microsoft.asurerun.R;
import com.microsoft.asurerun.model.RunItem;
import com.microsoft.asurerun.service.KeyGenService;
import com.microsoft.asurerun.ui.fragments.AboutUsPageFragment;
import com.microsoft.asurerun.ui.fragments.AdvancedSettingsFragment;
import com.microsoft.asurerun.ui.fragments.DeleteDataSettingsFragment;
import com.microsoft.asurerun.ui.fragments.HomeFragment;
import com.microsoft.asurerun.ui.fragments.LogoutFragment;
import com.microsoft.asurerun.ui.fragments.PredictionsFragment;
import com.microsoft.asurerun.ui.fragments.RunHistoryFragment;
import com.microsoft.asurerun.ui.fragments.RunSettingsFragment;
import com.microsoft.asurerun.ui.fragments.SetKeysFragment;
import com.microsoft.asurerun.ui.fragments.SettingsFragment;
import com.microsoft.asurerun.util.CacheUtil;
import com.microsoft.asurerun.util.Checks;
import com.microsoft.asurerun.util.Configuration;
import com.microsoft.asurerun.util.ImageCircleTrasform;
import com.microsoft.windowsazure.mobileservices.MobileServiceActivityResult;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.MobileServiceException;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceUser;
import com.microsoft.windowsazure.mobileservices.http.NextServiceFilterCallback;
import com.microsoft.windowsazure.mobileservices.http.OkHttpClientFactory;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilter;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterRequest;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.picasso.Picasso;

import java.net.MalformedURLException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.microsoft.asurerun.service.SyncDataService.startRefreshItems;
import static com.microsoft.asurerun.util.DialogUtil.*;
import static com.microsoft.asurerun.util.CacheUtil.*;
import static com.microsoft.asurerun.util.AuthUtil.*;


public class HomeActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private final static int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private final static int REFRESH_TOKEN_REQUEST_CODE = 30;
    private final static String TAG = "HOME_ATY";
    public static final int SIGN_IN_ACTIVITY_REQUEST_CODE = 2;
    public boolean bAuthenticating = false;
    public final Object mAuthenticationLock = new Object();
    private TextView mUserName, mUserMail, mUserLogoText;
    private ImageView mImageDrawer;
    /**
     * Mobile Service Client reference
     */
    private MobileServiceClient mClient;

    // Used to load the 'cryptoadapter' library on application startup.
    static {
        System.loadLibrary("cryptoadapter");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_layout);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (!BuildConfig.DEBUG) {
            //let's avoid sending AppCenter data for dev workflows
            AppCenter.start(getApplication(), getString(R.string.app_center_secret_key),
                    Analytics.class, Crashes.class, Distribute.class);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        View headerView = navigationView.getHeaderView(0);
        mUserName = (TextView) headerView.findViewById(R.id.username_drawer);
        mUserMail = (TextView) headerView.findViewById(R.id.email_drawer);
        View userLogo = headerView.findViewById(R.id.userLogo);
        mUserLogoText = (TextView) headerView.findViewById(R.id.user_logo_text);
        mImageDrawer = (ImageView) headerView.findViewById(R.id.user_logo_image);

        Configuration.loadConfigFile(getSharedPreferences("config", 0));
        // check the network connection
        checkNetworkConnection();
    }


    /**
     * Method to check whether the Internet is Connected
     */
    private void checkNetworkConnection() {
        if (Checks.IsNetworkConnected(this)) {
            // go ahead and start Mobile Service Client
            startServiceClient();
        } else {
            // show Dialog error message
            final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setMessage(getString(R.string.network_error_message));
            builder.setTitle(getString(R.string.network_error_title));
            builder.setPositiveButton("RETRY", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (Checks.IsNetworkConnected(HomeActivity.this)) {
                        startServiceClient();
                    } else {
                        checkNetworkConnection();
                    }
                }
            });
            builder.setCancelable(false);
            builder.setNegativeButton("EXIT", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    HomeActivity.this.finish();
                }
            });
            builder.create().show();
        }
    }

    /**
     * Starts Mobile Service Client and check permission.
     */
    private void startServiceClient() {
        try {
            // Create the Mobile Service Client instance, using the provided
            // Mobile Service URL and key
            mClient = new MobileServiceClient(Configuration.getWebHost(), this)
                    .withFilter(new RefreshTokenCacheFilter());
            ApplicationState.setMobileClient(mClient);

            // Extend timeout from default of 10s to 200s
            mClient.setAndroidHttpClientFactory(new OkHttpClientFactory() {
                @Override
                public OkHttpClient createOkHttpClient() {
                    OkHttpClient client = new OkHttpClient();
                    client.setReadTimeout(20, TimeUnit.MINUTES);
                    client.setWriteTimeout(20, TimeUnit.MINUTES);
                    return client;
                }
            });
            checkLocationPermission();
            setDrawerUi();

        } catch (MalformedURLException e) {
            createAndShowDialog(new Exception("There was an error creating the Mobile Service. Verify the URL"), "Error", this);
        } catch (Exception e) {
            createAndShowDialog(e, "Error", this);
        }
    }

    /**
     * Detects if authentication is in progress and waits for it to complete.
     * Returns true if authentication was detected as in progress. False otherwise.
     */
    public boolean detectAndWaitForAuthentication() {
        boolean detected = false;
        synchronized (mAuthenticationLock) {
            do {
                if (bAuthenticating)
                    detected = true;
                try {
                    mAuthenticationLock.wait(1000);
                } catch (InterruptedException e) {
                }
            }
            while (bAuthenticating);
        }
        if (bAuthenticating)
            return true;

        return detected;
    }


    /**
     * Waits for authentication to complete then adds or updates the token
     * in the X-ZUMO-AUTH request header.
     *
     * @param request The request that receives the updated token.
     */
    private void waitAndUpdateRequestToken(ServiceFilterRequest request) {
        MobileServiceUser user = null;
        if (detectAndWaitForAuthentication()) {
            user = mClient.getCurrentUser();
            if (user != null) {
                request.removeHeader("X-ZUMO-AUTH");
                request.addHeader("X-ZUMO-AUTH", user.getAuthenticationToken());
            }
        }
    }

    /**
     * Sets Drawer UI views
     */
    private void setDrawerUi() {
        SharedPreferences prefs = getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
        String userName = prefs.getString(USER_NAME_PREF, "");
        String userSurname = prefs.getString(USER_SURNAME_PREF, "");
        String userMail = prefs.getString(USER_MAIL_PREF, "");
        String userPhoto = prefs.getString(USER_PHOTO_PREF, "");
        if (!userPhoto.equals("")) {
            mUserLogoText.setVisibility(View.GONE);
            Picasso.get().load(userPhoto).transform(new ImageCircleTrasform()).into(mImageDrawer);
        }
        mUserMail.setText(userMail);
        mUserName.setText(userName + " " + userSurname);
        if (!userName.equals("") && !userSurname.equals("") && userPhoto.equals("")) {
            mImageDrawer.setVisibility(View.GONE);
            mUserLogoText.setText(userName.substring(0, 1) + userSurname.substring(0, 1));
        }
    }

    /**
     * Send the authentication request to microsoft
     */
    private void authenticate(boolean bRefreshCache) {
        bAuthenticating = true;

        if (!CacheUtil.loadUserTokenCache(mClient, this) || bRefreshCache) {
            // New login using the provider and update the token cache.
            loadSignInActivity();
        } else {
            // Other threads may be blocked waiting to be notified when
            // authentication is complete.
            synchronized (mAuthenticationLock) {
                bAuthenticating = false;
                mAuthenticationLock.notifyAll();
            }
            createTable();
            loadFragment(HomeFragment.newInstance(), false, false, HomeFragment.TAG);
            startRefreshItems(this);
            // start background service
            startKeyGenService();
        }
    }

    /**
     * This method allows to load the SignInActivity
     */
    private void loadSignInActivity() {
        Intent myIntent = new Intent(this, SignInActivity.class);
        startActivityForResult(myIntent, SIGN_IN_ACTIVITY_REQUEST_CODE);
    }

    /**
     * The RefreshTokenCacheFilter class filters responses for HTTP status code 401.
     * When 401 is encountered, the filter calls the authenticate method on the
     * UI thread. Out going requests and retries are blocked during authentication.
     * Once authentication is complete, the token cache is updated and
     * any blocked request will receive the X-ZUMO-AUTH header added or updated to
     * that request.
     */
    public class RefreshTokenCacheFilter implements ServiceFilter {

        AtomicBoolean mAtomicAuthenticatingFlag = new AtomicBoolean();

        @Override
        public ListenableFuture<ServiceFilterResponse> handleRequest(
                final ServiceFilterRequest request,
                final NextServiceFilterCallback nextServiceFilterCallback
        ) {
            // In this example, if authentication is already in progress we block the request
            // until authentication is complete to avoid unnecessary authentications as
            // a result of HTTP status code 401.
            // If authentication was detected, add the token to the request.
            waitAndUpdateRequestToken(request);

            // Send the request down the filter chain
            // retrying up to 5 times on 401 response codes.
            ListenableFuture<ServiceFilterResponse> future = null;
            ServiceFilterResponse response = null;
            int responseCode = 401;
            for (int i = 0; (i < 5) && (responseCode == 401); i++) {
                future = nextServiceFilterCallback.onNext(request);
                try {
                    response = future.get();
                    responseCode = response.getStatus().code;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    if (e.getCause().getClass() == MobileServiceException.class) {
                        MobileServiceException mEx = (MobileServiceException) e.getCause();
                        responseCode = mEx.getResponse().getStatus().code;
                        if (responseCode == 401) {
                            // Two simultaneous requests from independent threads could get HTTP status 401.
                            // Protecting against that right here so multiple authentication requests are
                            // not setup to run on the UI thread.
                            // We only want to authenticate once. Requests should just wait and retry
                            // with the new token.
                            if (mAtomicAuthenticatingFlag.compareAndSet(false, true)) {
                                // Authenticate on UI thread
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        // Force a token refresh during authentication.
                                        authenticate(true);
                                    }
                                });
                            }

                            // Wait for authentication to complete then update the token in the request.
                            waitAndUpdateRequestToken(request);
                            mAtomicAuthenticatingFlag.set(false);
                        }
                    }
                }
            }
            return future;
        }

    }


    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission, please accept to use location functionality")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(HomeActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
        } else { // Permission already granted
            // Authenticate the user
            authenticate(false);
        }
    }

    /**
     * Process the response of the authentication request
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // When request completes
        if (resultCode == RESULT_OK) {
            // Check the request code matches the one we send in the login request
            if (requestCode == SIGN_IN_ACTIVITY_REQUEST_CODE) {
                // Other threads may be blocked waiting to be notified when
                // authentication is complete.
                synchronized (mAuthenticationLock) {
                    // set this activity context to Azure client
                    mClient.setContext(this);
                    createTable();
                    setDrawerUi();
                    // start background service
                    loadFragment(SetKeysFragment.newInstance(), false, false, HomeFragment.TAG);
                    bAuthenticating = false;
                    mAuthenticationLock.notifyAll();
                }
            } else if (requestCode == REFRESH_TOKEN_REQUEST_CODE) {
                MobileServiceActivityResult result = mClient.onActivityResult(data);
                if (result.isLoggedIn()) {
                    synchronized (mAuthenticationLock) {
                        CacheUtil.cacheUserToken(mClient.getCurrentUser(), this);
                        createTable();
                        loadFragment(HomeFragment.newInstance(), false, false, HomeFragment.TAG);
                        // start background service
                        startKeyGenService();
                        bAuthenticating = false;
                        mAuthenticationLock.notifyAll();
                    }
                } else {
                    loadSignInActivity();
                }
            }
        }
    }

    private void createTable() {
        try {
            ApplicationState.createTable(this);
        } catch (Exception e) {
            createAndShowDialog(e, "Error", HomeActivity.this);
        }
    }

    public void startKeyGenService() {
        Intent intent = new Intent(this, KeyGenService.class);
        startService(intent);
    }

    private void stopKeyGenService() {
        Intent intent = new Intent(this, KeyGenService.class);
        stopService(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        // Authenticate the user
                        authenticate(false);
                    }

                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage("The app needs permission to access location services to function properly\n")
                            .setTitle("Location Permission Needed")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // check permission again
                                    checkLocationPermission();
                                }
                            })
                            .setNegativeButton("EXIT", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // User cancelled the dialog and kill app
                                    finish();
                                }
                            });
                    builder.create().show();
                }
            }
        }
    }

    /**
     * This method allows load or replace dynamically a Fragment in a FrameLayout
     *
     * @param fragment        Android Fragment view
     * @param addToBackStack  true if you want to add fragment to backStack, false otherwise
     * @param fragmentTag     the fragment unique tag
     * @param customAnimation true if you want a custom animation for fragment transaction, false otherwise.
     */
    public void loadFragment(Fragment fragment, boolean addToBackStack, boolean customAnimation, String fragmentTag) {
        // create a FragmentManager
        FragmentManager fm = getSupportFragmentManager();
        // create a FragmentTransaction to begin the transaction and replace the Fragment
        FragmentTransaction fragmentTransaction = fm.beginTransaction();
        // add custom animation for settings fragments
        if (customAnimation) {
            fragmentTransaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right,
                    android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        }
        // replace the FrameLayout with new Fragment
        fragmentTransaction.replace(R.id.frameLayout, fragment, fragmentTag);
        //check if there is an instance of the fragment
        Fragment fragmentInstance = fm.findFragmentByTag(fragmentTag);
        if (addToBackStack && fragmentInstance == null) {
            // add fragment to backStack
            fragmentTransaction.addToBackStack(null);
        }
        fragmentTransaction.commitAllowingStateLoss(); // save the changes
    }

    /**
     * This method allows to clear backStack
     */
    public void clearFragmentBackStack() {
        // create a FragmentManager
        FragmentManager fm = getSupportFragmentManager();
        fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_tracking) {
            clearFragmentBackStack();
            loadFragment(HomeFragment.newInstance(), false, false, HomeFragment.TAG);
        } else if (id == R.id.nav_history) {
            clearFragmentBackStack();
            loadFragment(RunHistoryFragment.newInstance(), true, false, RunHistoryFragment.TAG);
        } else if (id == R.id.nav_predictions) {
            clearFragmentBackStack();
            loadFragment(PredictionsFragment.newIstance(), true, false, PredictionsFragment.TAG);
        } else if (id == R.id.nav_setting) {
            clearFragmentBackStack();
            loadFragment(SettingsFragment.newInstance(), true, false, SettingsFragment.TAG);
        } else if (id == R.id.nav_logout) {
            clearFragmentBackStack();
            loadFragment(LogoutFragment.newInstance(), true, false, LogoutFragment.TAG);
        } else if (id == R.id.nav_about_us) {
            clearFragmentBackStack();
            loadFragment(AboutUsPageFragment.newInstance(), true, false, AboutUsPageFragment.TAG);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onDestroy() {
        stopKeyGenService();
        ApplicationState.releaseCryptoContext();
        super.onDestroy();
    }
}
