// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.asurerun.ui.login.providers;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.microsoft.asurerun.R;
import com.microsoft.asurerun.ui.SignInActivity;
import com.microsoft.asurerun.util.CacheUtil;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceAuthenticationProvider;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceUser;

import org.json.JSONException;
import org.json.JSONObject;

import static com.microsoft.asurerun.util.AuthUtil.GOOGLE_PROVIDER;

/**
 * Provider that handles login to Google account
 */
public class GoogleProvider implements LoginProvider {
    public static final String TAG = "GOOGLE_SIGN_IN";
    public static final int GOOGLE_SIGNIN_REQUEST_CODE = 22;

    //Google sign in button
    private ImageButton mGLSignButton;

    SignInActivity mSignInActivity;
    MobileServiceClient mClient;
    private GoogleSignInClient mGoogleSignInClient;

    /**
     * Setup configuration for the Google login provider
     *
     * @param signInActivity SignInActivity instance
     * @param client MobileServiceClient instance
     */
    @Override
    public void configureLogin(SignInActivity signInActivity, MobileServiceClient client) {
        mSignInActivity = signInActivity;
        mClient = client;

        mGLSignButton = (ImageButton) mSignInActivity.findViewById(R.id.btnGLLogin);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(mSignInActivity.getString(R.string.googleClientId))
                .requestProfile()
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(mSignInActivity, gso);

        mGLSignButton = (ImageButton) mSignInActivity.findViewById(R.id.btnGLLogin);
        mGLSignButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSignInActivity.setCurrentProvider(GoogleProvider.this);
                Intent signInIntent = mGoogleSignInClient.getSignInIntent();
                mSignInActivity.startActivityForResult(signInIntent, GOOGLE_SIGNIN_REQUEST_CODE);
            }
        });
    }

    /**
     * Handle Activity Result for Google login
     * @param requestCode Request code
     * @param resultCode Actual result code from the request
     * @param data Additional data from the result
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // The Task returned from this call is always completed, no need to attach a listener.
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

        if (resultCode == Activity.RESULT_OK) {
            handleSignInResult(task);
        } else {
            Log.e(TAG, "Sign in error: ", task.getException());
        }
    }

    /**
     * Handles the google sign in response and  passes the currently signed-in user to the backend server.
     *
     * @param completedTask the task returned from google sign in request
     */
    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            final GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            Log.d(TAG, " Google signInResult:= " + account.getIdToken());
            JSONObject payload = new JSONObject();
            payload.put("id_token", account.getIdToken());
            mSignInActivity.getProgressBar().setVisibility(View.VISIBLE);
            ListenableFuture<MobileServiceUser> mLogin = mClient.login(MobileServiceAuthenticationProvider.Google, payload.toString());
            Futures.addCallback(mLogin, new FutureCallback<MobileServiceUser>() {
                @Override
                public void onFailure(Throwable exc) {
                    Log.e(TAG, "Azure login error: "+ exc.getCause().getMessage());
                    mSignInActivity.getProgressBar().setVisibility(View.INVISIBLE);
                }

                @Override
                public void onSuccess(MobileServiceUser user) {
                    Log.e(TAG, "Google Login Complete");
                    CacheUtil.cacheUserToken(user, GOOGLE_PROVIDER, mSignInActivity);
                    CacheUtil.cacheUserInfo(account.getGivenName(), account.getFamilyName(), account.getEmail(), account.getPhotoUrl().toString(), mSignInActivity);
                    Intent returnIntent = new Intent();
                    mSignInActivity.setResult(Activity.RESULT_OK, returnIntent);
                    mSignInActivity.finish();
                }
            });
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.e(TAG, "Google signInResult:failed code=" + e.getStatusCode());
            mSignInActivity.getProgressBar().setVisibility(View.INVISIBLE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
