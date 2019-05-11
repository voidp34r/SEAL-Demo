// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.asurerun.ui.login.providers;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.microsoft.asurerun.R;
import com.microsoft.asurerun.ui.SignInActivity;
import com.microsoft.asurerun.util.CacheUtil;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.AuthenticationResult;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.exception.MsalServiceException;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceAuthenticationProvider;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static com.microsoft.asurerun.util.AuthUtil.MICROSOFT_PROVIDER;

/**
 * Provider that handles login to Microsoft account
 */
public class MicrosoftProvider implements LoginProvider {

    /* MS Configs */
    private final static String SCOPES[] = {"https://graph.microsoft.com/User.Read"};
    private final static String MSGRAPH_URL = "https://graph.microsoft.com/v1.0/me";

    public static final String TAG = "MS_SIGN_IN";

    //Microsoft sign in button
    private ImageButton mMsSignButton;

    private PublicClientApplication mMSAClientAuth;
    private AuthenticationResult mAuthResult;

    private SignInActivity mSignInActivity;
    private MobileServiceClient mClient;

    /**
     * Setup configuration for the Microsoft login provider
     *
     * @param signInActivity SignInActivity instance
     * @param client MobileServiceClient instance
     */
    @Override
    public void configureLogin(SignInActivity signInActivity, MobileServiceClient client) {
        mSignInActivity = signInActivity;
        mClient = client;
        mMSAClientAuth = new PublicClientApplication(
                mSignInActivity.getApplicationContext(), mSignInActivity.getString(R.string.microsoftClientId));

        mMsSignButton = (ImageButton) mSignInActivity.findViewById(R.id.btnMSLogin);
        mMsSignButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSignInActivity.setCurrentProvider(MicrosoftProvider.this);
                mMSAClientAuth.acquireToken(mSignInActivity, SCOPES, getAuthInteractiveCallback());
            }
        });
    }

    /**
     * Handle Activity Result for Microsoft login
     *
     * @param requestCode Request code
     * @param resultCode Actual result code from the request
     * @param data Additional data from the result
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mMSAClientAuth.handleInteractiveRequestRedirect(requestCode, resultCode, data);
    }

    /**
     * Get an AuthenticationCallback object to handle the result of the Microsoft login
     */
    private AuthenticationCallback getAuthInteractiveCallback() {
        return new AuthenticationCallback() {
            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                /* Successfully got a token, use it to call a protected resource */
                Log.e(TAG, " MSA signInResult:= " + authenticationResult.getAccessToken());
                mAuthResult = authenticationResult;
                mSignInActivity.getProgressBar().setVisibility(View.VISIBLE);
                callGraphAPI();
            }

            @Override
            public void onError(MsalException exception) {
                /* Failed to acquireToken */

                if (exception instanceof MsalClientException) {
                    /* Exception inside MSAL, more info inside MsalError.java */
                    Log.e(TAG, "MSA signInResult: error =" + exception.getMessage());
                } else if (exception instanceof MsalServiceException) {
                    /* Exception when communicating with the STS, likely config issue */
                    Log.e(TAG, "MSA signInResult:error =" + exception.getMessage());
                }
            }

            @Override
            public void onCancel() {
                /* User canceled the authentication */
            }
        };
    }

    /**
     * Use Volley to make an HTTP request to the /me endpoint from MS Graph using an access token
     */
    private void callGraphAPI() {
        Log.d(TAG, "Starting volley request to graph");
        /* Make sure we have a token to send to graph */
        if (mAuthResult.getAccessToken() == null) {
            return;
        }
        RequestQueue queue = Volley.newRequestQueue(mSignInActivity);
        JSONObject parameters = new JSONObject();
        try {
            parameters.put("key", "value");
        } catch (Exception e) {
            Log.d(TAG, "Failed to put parameters: " + e.toString());
        }
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, MSGRAPH_URL,
                parameters, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(final JSONObject response) {
                /* Successfully called graph, process data and send to UI */
                Log.e(TAG, "MSA Response: " + response.toString());
                JSONObject payload = new JSONObject();
                try {
                    payload.put("access_token", mAuthResult.getAccessToken());
                    ListenableFuture<MobileServiceUser> mLogin = mClient.login(MobileServiceAuthenticationProvider.MicrosoftAccount, payload.toString());
                    Futures.addCallback(mLogin, new FutureCallback<MobileServiceUser>() {
                        @Override
                        public void onFailure(Throwable exc) {
                            Log.e(TAG, exc.getCause().getMessage());
                            mSignInActivity.getProgressBar().setVisibility(View.INVISIBLE);
                        }

                        @Override
                        public void onSuccess(MobileServiceUser user) {
                            Log.e(TAG, "MSA Login Complete");
                            CacheUtil.cacheUserToken(user, MICROSOFT_PROVIDER, mSignInActivity);
                            try {
                                CacheUtil.cacheUserInfo(response.getString("givenName"), response.getString("surname"), response.getString("mail"), mSignInActivity);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            Intent returnIntent = new Intent();
                            mSignInActivity.setResult(Activity.RESULT_OK, returnIntent);
                            mSignInActivity.finish();
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "Error: " + error.toString());
                mSignInActivity.getProgressBar().setVisibility(View.INVISIBLE);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + mAuthResult.getAccessToken());
                return headers;
            }
        };
        Log.d(TAG, "Adding HTTP GET to Queue, Request: " + request.toString());
        request.setRetryPolicy(new DefaultRetryPolicy(
                3000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(request);
    }
}
