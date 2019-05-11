// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.asurerun.model;

public class SummaryItem {

    @com.google.gson.annotations.SerializedName("id")
    private String mId;

    @com.google.gson.annotations.SerializedName("keyId")
    private String mKeyId;

    @com.google.gson.annotations.SerializedName("userId")
    private String mUserId;

    @com.google.gson.annotations.SerializedName("summary")
    private String mSummary;

    public SummaryItem() {
    }

    public String getSummary() {
        return mSummary;
    }

    public void setSummary(String mSummary) {
        this.mSummary = mSummary;
    }

    public String getUserId() {
        return mUserId;
    }

    public void setUserId(String mUserId) {
        this.mUserId = mUserId;
    }

    public String getKeyId() {
        return mKeyId;
    }

    public void setKeyId(String mKeyId) {
        this.mKeyId = mKeyId;
    }
}
