// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.asurerun.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.microsoft.asurerun.model.ApplicationState;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceUser;

public class CacheUtil {
    /**
     * Authentication Token Cache Variables
     */
    public static final String SHAREDPREFFILE = "temp";
    public static final String USERIDPREF = "uid";
    public static final String TOKENPREF = "tkn";
    public static final String AUTH_PROVIDER = "prvd";
    public static final String USER_NAME_PREF = "usrnmpr";
    public static final String USER_SURNAME_PREF = "usrsrnpr";
    public static final String USER_MAIL_PREF = "usrmlpr";
    public static final String USER_PHOTO_PREF = "usrphtpr";
    public final static String KM_MILES_KEY = "KmMilKeys";
    public final static String MILES_VALUE = "MilVal";
    public final static String KILOMETERS_VALUE = "KmVal";
    public final static String MAX_RUN_NUMBER_PREF = "mxRnNb";

    // the key to get provider from SignInActivity
    public static final String PROVIDER_KEY = "provider";

    /**
     * Caches the User Authentication Token and the authentication provider
     *
     * @param provider the authentication provider
     * @param user     the current user
     * @param context  the activity context
     */
    public static void cacheUserToken(MobileServiceUser user, String provider, Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(USERIDPREF, user.getUserId());
        editor.putString(TOKENPREF, user.getAuthenticationToken());
        editor.putString(AUTH_PROVIDER, provider);
        editor.commit();
    }

    /**
     * Caches the User Authentication Token
     *
     * @param user    the current user
     * @param context the activity context
     */
    public static void cacheUserToken(MobileServiceUser user, Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(USERIDPREF, user.getUserId());
        editor.putString(TOKENPREF, user.getAuthenticationToken());
        editor.commit();
    }

    /**
     * Caches the User name,surname and email
     *
     * @param username the user name
     * @param surname  the user surname
     * @param mail     the user mail
     * @param context  the activity context
     */
    public static void cacheUserInfo(String username, String surname, String mail, Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(USER_MAIL_PREF, mail);
        editor.putString(USER_NAME_PREF, username);
        editor.putString(USER_SURNAME_PREF, surname);
        editor.commit();
    }
    /**
     * Caches the User name,surname,photo and email
     *
     * @param username the user name
     * @param surname  the user surname
     * @param mail     the user mail
     * @param urlPhoto the user image
     * @param context  the activity context
     */
    public static void cacheUserInfo(String username, String surname, String mail,String urlPhoto, Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(USER_MAIL_PREF, mail);
        editor.putString(USER_NAME_PREF, username);
        editor.putString(USER_SURNAME_PREF, surname);
        editor.putString(USER_PHOTO_PREF,urlPhoto);
        editor.commit();
    }

    /**
     * Deletes user's token and Info
     * @param context the activity context
     */
    public static void deleteUserTokenAndInfo(Context context){
        SharedPreferences prefs = context.getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(AUTH_PROVIDER);
        editor.remove(USERIDPREF);
        editor.remove(TOKENPREF);
        editor.remove(USER_MAIL_PREF);
        editor.remove(USER_NAME_PREF);
        editor.remove(USER_SURNAME_PREF);
        editor.remove(USER_PHOTO_PREF);
        editor.remove(MAX_RUN_NUMBER_PREF);
        editor.commit();
    }

    /**
     * Loads a User Authentication Token
     *
     * @param client the Azure Mobile service client
     * @param context the activity context
     * @return true if it loads user token, false otherwise
     */
    public static boolean loadUserTokenCache(MobileServiceClient client,Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
        ApplicationState.setAuthProvider(prefs.getString(AUTH_PROVIDER, null));
        String userId = prefs.getString(USERIDPREF, null);
        if (userId == null)
            return false;
        String token = prefs.getString(TOKENPREF, null);
        if (token == null)
            return false;
        MobileServiceUser user = new MobileServiceUser(userId);
        user.setAuthenticationToken(token);
        client.setCurrentUser(user);
        return true;
    }

    /**
     * Caches the max run number
     * @param context the activity/Service context
     * @param maxRunNumber the max run number
     */
    public static void cacheMaxRunNumber(Context context,int maxRunNumber){
        SharedPreferences prefs = context.getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(MAX_RUN_NUMBER_PREF,maxRunNumber);
        editor.commit();
    }

    /**
     *
     * @param context the activity/Service context
     * @return a value > 0 if it loads max run number, 0 otherwise.
     */
    public static int loadMaxRunNumber(Context context){
        SharedPreferences prefs = context.getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
        return prefs.getInt(MAX_RUN_NUMBER_PREF,0);
    }

    public static String getUserMail(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
        String userMail = prefs.getString(USER_MAIL_PREF, "");
        return userMail;
    }
}
