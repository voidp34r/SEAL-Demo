// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.asurerun.ui.login.providers;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
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

import java.util.Arrays;

import static com.microsoft.asurerun.util.AuthUtil.FACEBOOK_PROVIDER;

/**
 * Provider that handles login to Facebook account
 */
public class FacebookProvider implements LoginProvider {
    public static final String TAG = "FB_SIGN_IN";

    private SignInActivity mSignInActivity;
    private MobileServiceClient mClient;

    //Facebook sign in buttons
    private LoginButton mFBSignButton;
    private ImageButton mFBCustomButton;

    private CallbackManager mFacebookCallBackMan;

    /**
     * Setup configuration for the Facebook login provider
     *
     * @param signInActivity SignInActivity instance
     * @param client MobileServiceClient instance
     */
    @Override
    public void configureLogin(SignInActivity signInActivity, MobileServiceClient client) {
        mSignInActivity = signInActivity;
        mClient = client;

        mFBSignButton = (LoginButton) mSignInActivity.findViewById(R.id.btnFBLogin);
        mFBCustomButton = (ImageButton) mSignInActivity.findViewById(R.id.btnFBLoginCustom);

        mFBCustomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSignInActivity.setCurrentProvider(FacebookProvider.this);
                mFBSignButton.performClick();
            }
        });

        final String EMAIL = "email";
        mFacebookCallBackMan = CallbackManager.Factory.create();
        mFBSignButton.setReadPermissions(Arrays.asList(EMAIL));
        // Callback registration
        mFBSignButton.registerCallback(mFacebookCallBackMan, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.e(TAG, "Facebook success=" + loginResult.getAccessToken().getToken());
                final String profilePicUrl = "https://graph.facebook.com/" + loginResult.getAccessToken().getUserId() +
                        "/picture?type=large&width=300&height=300";
                final String accessToken = loginResult.getAccessToken().getToken();
                mSignInActivity.getProgressBar().setVisibility(View.VISIBLE);
                GraphRequest request = GraphRequest.newMeRequest(
                        loginResult.getAccessToken(),
                        new GraphRequest.GraphJSONObjectCallback() {
                            @Override
                            public void onCompleted(
                                    JSONObject object,
                                    GraphResponse response) {
                                Log.e(TAG, "Facebook success=" + object.toString());
                                JSONObject payload = new JSONObject();
                                try {
                                    final String name = object.getString("first_name");
                                    final String lastName = object.getString("last_name");
                                    final String email = object.getString("email");
                                    payload.put("access_token", accessToken);
                                    mSignInActivity.getProgressBar().setVisibility(View.VISIBLE);
                                    ListenableFuture<MobileServiceUser> mLogin = mClient.login(MobileServiceAuthenticationProvider.Facebook, payload.toString());
                                    Futures.addCallback(mLogin, new FutureCallback<MobileServiceUser>() {
                                        @Override
                                        public void onFailure(Throwable exc) {
                                            Log.e(TAG, exc.getCause().getMessage());
                                            mSignInActivity.getProgressBar().setVisibility(View.INVISIBLE);
                                        }

                                        @Override
                                        public void onSuccess(MobileServiceUser user) {
                                            Log.e(TAG, "Facebook Login Complete");
                                            CacheUtil.cacheUserToken(user, FACEBOOK_PROVIDER, mSignInActivity);
                                            CacheUtil.cacheUserInfo(name, lastName, email, profilePicUrl, mSignInActivity);
                                            Intent returnIntent = new Intent();
                                            mSignInActivity.setResult(Activity.RESULT_OK, returnIntent);
                                            mSignInActivity.getProgressBar().setVisibility(View.INVISIBLE);
                                            mSignInActivity.finish();                                        }
                                    });
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,email,first_name,last_name");
                request.setParameters(parameters);
                request.executeAsync();
            }

            @Override
            public void onCancel() {
                Log.e(TAG, "signInResult:failed code= CANCELLED");
            }

            @Override
            public void onError(FacebookException exception) {
                Log.e(TAG, "signInResult:failed code=" + exception.getMessage());
            }
        });
    }

    /**
     * Handle Activity Result for Facebook login
     *
     * @param requestCode Request code
     * @param resultCode Actual result code from the request
     * @param data Additional data from the result
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mFacebookCallBackMan.onActivityResult(requestCode, resultCode, data);
    }
}
