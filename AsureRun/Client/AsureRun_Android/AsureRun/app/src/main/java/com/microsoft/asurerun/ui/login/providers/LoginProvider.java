// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.asurerun.ui.login.providers;

import android.content.Intent;

import com.microsoft.asurerun.ui.SignInActivity;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;

/**
 * Interface of a Login Provider
 */
public interface LoginProvider {
    /**
     * Setup configuration for this login provider
     *
     * @param signInActivity SignInActivity instance
     * @param client MobileServiceClient instance
     */
    void configureLogin(SignInActivity signInActivity, MobileServiceClient client);

    /**
     * Handle Activity Result for this login provider
     *
     * @param requestCode Request code
     * @param resultCode Actual result code from the request
     * @param data Additional data from the result
     */
    void onActivityResult(int requestCode, int resultCode, Intent data);
}
