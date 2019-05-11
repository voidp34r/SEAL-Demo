// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.asurerun.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.facebook.AccessToken;
import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.microsoft.asurerun.R;
import com.microsoft.asurerun.model.ApplicationState;
import com.microsoft.asurerun.ui.SignInActivity;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceAuthenticationProvider;

import static com.microsoft.asurerun.ui.HomeActivity.SIGN_IN_ACTIVITY_REQUEST_CODE;
import static com.microsoft.asurerun.util.CacheUtil.deleteUserTokenAndInfo;

public class AuthUtil {
    public static final String MICROSOFT_PROVIDER = "microsoft";
    public static final String GOOGLE_PROVIDER = "google";
    public static final String FACEBOOK_PROVIDER = "facebook";

    public static MobileServiceAuthenticationProvider getAuthenticationProvider(String provider) {
        MobileServiceAuthenticationProvider authenticationProvider;
        switch (provider) {
            case MICROSOFT_PROVIDER:
                authenticationProvider = MobileServiceAuthenticationProvider.MicrosoftAccount;
                break;
            case GOOGLE_PROVIDER:
                authenticationProvider = MobileServiceAuthenticationProvider.Google;
                break;
            case FACEBOOK_PROVIDER:
                authenticationProvider = MobileServiceAuthenticationProvider.Facebook;
                break;
            default:
                throw new IllegalArgumentException("request provider does not match any MobileServiceAuthenticationProvider ");
        }
        return authenticationProvider;
    }

    /**
     * Allows to logout from Google account
     *
     * @param context the activity context
     */
    private static void logoutGoogleProvider(final Context context) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.googleClientId))
                .requestProfile()
                .build();
        // Build a GoogleSignInClient with the options specified by gso.
        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(context, gso);
        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (account != null) {
            mGoogleSignInClient.signOut()
                    .addOnCompleteListener(((Activity) context), new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            ApplicationState.setIsKeysCreated(false);
                            // delete all user info
                            deleteUserTokenAndInfo(context);
                            // load sign in
                            Intent myIntent = new Intent(context, SignInActivity.class);
                            ((Activity) context).startActivityForResult(myIntent, SIGN_IN_ACTIVITY_REQUEST_CODE);
                        }
                    });
        }
    }

    /**
     * Allows to logout from authProvider
     *
     * @param authProvider the current Auth provider
     * @param context the activity context
     * @param client the Azure service client
     */
    public static void logout(String authProvider, Context context, MobileServiceClient client) {
        if (authProvider.equals(GOOGLE_PROVIDER)) {
            client.logout();
            logoutGoogleProvider(context);
        } else if (authProvider.equals(FACEBOOK_PROVIDER)) {
            client.logout();
            logoutFacebookAccount(context);
        } else if (authProvider.equals(MICROSOFT_PROVIDER)) {
            //todo: MS logout
        }
    }

    /**
     * Allows to logout from Facebook account
     *
     * @param context the activity context
     */
    private static void logoutFacebookAccount(Context context) {
        AccessToken.setCurrentAccessToken(null);
        if (LoginManager.getInstance() != null) {
            LoginManager.getInstance().logOut();
        }
        ApplicationState.setIsKeysCreated(false);
        // delete all user info
        deleteUserTokenAndInfo(context);
        // load sign in
        Intent myIntent = new Intent(context, SignInActivity.class);
        ((Activity) context).startActivityForResult(myIntent, SIGN_IN_ACTIVITY_REQUEST_CODE);
    }
}
