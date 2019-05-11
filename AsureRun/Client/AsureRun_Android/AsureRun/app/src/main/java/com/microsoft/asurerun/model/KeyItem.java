// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.asurerun.model;

public class KeyItem {
    @com.google.gson.annotations.SerializedName("id")
    private String mId;

    @com.google.gson.annotations.SerializedName("slotCount")
    private int mSlotCount;

    @com.google.gson.annotations.SerializedName("initialScale")
    private int mInitialScale;

    @com.google.gson.annotations.SerializedName("userId")
    private String mUserId;

    @com.google.gson.annotations.SerializedName("userEmail")
    private String mUserEmail;

    public KeyItem(int slotCount, int initialScale)
    {
        mSlotCount = slotCount;
        mInitialScale = initialScale;
    }

    /**
     * Gets the variables
     */
    public String getId() { return mId; }
    public int getSlotCount() { return mSlotCount; }
    public int getInitialScale() { return mInitialScale; }
    public String getUserId() { return mUserId; }
    public String getUserEmail() { return mUserEmail; }

    /**
     * Sets the variables
     */
    public void setSlotCount(int number)
    {
        mSlotCount = number;
    }
    public void setInitialScale(int number)
    {
        mInitialScale = number;
    }
    public void setUserId(String id)
    {
        mUserId = id;
    }
    public void setUserEmail(String email)
    {
        mUserEmail = email;
    }

}
