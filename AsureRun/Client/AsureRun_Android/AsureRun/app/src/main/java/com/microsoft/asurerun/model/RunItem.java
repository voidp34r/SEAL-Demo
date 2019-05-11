// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.asurerun.model;

import android.graphics.Bitmap;
import android.provider.BaseColumns;

import java.util.Calendar;

/**
 * Represents an item in a Run list
 */
public class RunItem {

    public static class RunItemEntry implements BaseColumns {
        public static final String TABLE_NAME = "runItems";
        public static final String COLUMN_NAME_ID = "id";
        public static final String COLUMN_NAME_KEY_ID = "keyId";
        public static final String COLUMN_NAME_USER_ID = "userId";
        public static final String COLUMN_NAME_RUN_NUMBER = "runNumber";
        public static final String COLUMN_NAME_CIPHER_1 = "cipher1";
        public static final String COLUMN_NAME_CIPHER_2 = "cipher2";
        public static final String COLUMN_NAME_CIPHER_EP = "cipherEP";
        public static final String COLUMN_NAME_CIPHER_THUMBNAIL = "cipherThumbnail";
        public static final String COLUMN_NAME_CIPHER_GYRO = "cipherGyro";
        public static final String COLUMN_NAME_STATS = "stats";
        public static final String COLUMN_NAME_SUMMARY = "summary";
    }

    private transient ServerCalculations mServerCalculation;

    /**
     * NOTE: These are fields used to temporarily show the item not yet loaded on the server
     */
    private transient boolean mIsLocalItem = false;
    private transient long mTotalTime;
    private transient Bitmap mMapSnapshot;
    private transient double mTotalDistance;
    private transient double mAvgPace;
    private transient double mElevationGain;
    private transient Calendar mDate;

    /**
     * Item Id
     */
    @com.google.gson.annotations.SerializedName(RunItemEntry.COLUMN_NAME_ID)
    private String mId;

    /**
     * Key Id
     */
    @com.google.gson.annotations.SerializedName(RunItemEntry.COLUMN_NAME_KEY_ID)
    private String mKeyId;

    /**
     * User Id
     */
    @com.google.gson.annotations.SerializedName(RunItemEntry.COLUMN_NAME_USER_ID)
    private String mUserId;

    /**
     * Run Number
     */
    @com.google.gson.annotations.SerializedName(RunItemEntry.COLUMN_NAME_RUN_NUMBER)
    private int mRunNumber;

    /**
     * Item cartesian X&Y as base 64 ciphertext
     */
    @com.google.gson.annotations.SerializedName(RunItemEntry.COLUMN_NAME_CIPHER_1)
    private String mCipher1;

    /**
     * Item cartesian Z&Timestamp as base 64 ciphertext
     */
    @com.google.gson.annotations.SerializedName(RunItemEntry.COLUMN_NAME_CIPHER_2)
    private String mCipher2;


    /**
     * Item cartesian Elevation & Pace as base 64 ciphertext
     */
    @com.google.gson.annotations.SerializedName(RunItemEntry.COLUMN_NAME_CIPHER_EP)
    private String mCipherEP;

    /**
     * Item cartesian Z&Timestamp as base 64 ciphertext
     */
    @com.google.gson.annotations.SerializedName(RunItemEntry.COLUMN_NAME_CIPHER_THUMBNAIL)
    private String mCipherThumbnail;

    /**
     * Item accelerometer and gyroscope as base 64 ciphertext
     */
    @com.google.gson.annotations.SerializedName(RunItemEntry.COLUMN_NAME_CIPHER_GYRO)
    private String mCipherGyro;

    /**
     * Item stats as base 64 ciphertext
     */
    @com.google.gson.annotations.SerializedName(RunItemEntry.COLUMN_NAME_STATS)
    private String mStats;

    /**
     * Item summary as base 64 ciphertext
     */
    @com.google.gson.annotations.SerializedName(RunItemEntry.COLUMN_NAME_SUMMARY)
    private String mSummary;

    /**
     * RunItem constructor
     */
    public RunItem() {

    }

    /**
     * Initializes a new RunItem
     *
     * TODO: Update comment
     * @param text
     *            The item text
     * @param id
     *            The item id
     */
    public RunItem(int runNumber, String cipher1, String cipher2, String stats, String summary, String id, String keyId, String userId) {
        this.setRunNumber(runNumber);
        this.setCipher1(cipher1);
        this.setCipher2(cipher2);
        this.setSummary(summary);
        this.setUserId(userId);
        this.setStats(stats);
        this.setKeyId(keyId);
        this.setId(id);
    }

    /**
     * Gets the variables
     */
    public int getRunNumber() { return mRunNumber; }
    public String getCipher1() { return mCipher1; }
    public String getCipher2() { return mCipher2; }
    public String getSummary() { return mSummary; }
    public String getUserId() { return mUserId; }
    public String getStats() { return mStats; }
    public String getKeyId() { return mKeyId; }
    public String getId() { return mId; }
    public String getCipherThumbnail() { return  mCipherThumbnail; }
    public String getCipherEP() { return mCipherEP; }
    public String getCipherGyro() { return mCipherGyro; }
    public long getTotalTime(){ return mTotalTime; }
    public double getTotalDistance(){ return mTotalDistance; }
    public double getElevationGain(){ return mElevationGain; }
    public Calendar getDate() {return  mDate;}
    public double getAvgPace() { return mAvgPace; }
    public boolean isLocalItem() { return mIsLocalItem; }
    public Bitmap getMapSnapshot() { return mMapSnapshot; }

    /**
     * Sets the variables
     */
    public final void setRunNumber(int number) { mRunNumber = number; }
    public final void setCipher1(String text) { mCipher1 = text; }
    public final void setCipher2(String text) { mCipher2 = text; }
    public final void setSummary(String text) { mSummary = text; }
    public final void setUserId(String text) { mUserId = text; }
    public final void setStats(String text) { mStats = text; }
    public final void setKeyId(String text) { mKeyId = text; }
    public final void setCipherThumbnail(String text) { mCipherThumbnail = text; }
    public final void setCipherEP(String text) { mCipherEP = text; }
    public final void setCipherGyro(String text) { mCipherGyro = text; }
    public final void setId(String id) { mId = id; }
    public void setTotalTime(long totalTime){ mTotalTime = totalTime; }
    public void setTotalDistance(double totalDistance){ mTotalDistance = totalDistance; }
    public void setElevationGain(double elevationGain){ mElevationGain = elevationGain; }
    public void setDate(Calendar date) {mDate = date;}
    public void setAvgPace(double avgPace){ mAvgPace = avgPace; }
    public void setIsLocalItem(boolean isLocalItem){ mIsLocalItem = isLocalItem; }
    public void setMapSnapshot(Bitmap map){ mMapSnapshot = map; }

    @Override
    public boolean equals(Object o) {
        return o instanceof RunItem && ((RunItem) o).mId == mId;
    }

    @Override
    public String toString() {
        // TODO: Here is where we decrypt and create a string to be displayed
        String listString = "Run " + mRunNumber + ": Total Distance ##:## - Total Time ## (m)";
        return listString;
    }

    /**
     * Takes the encrypted stats and summary of this RunItem and decrypts all the data. The data is
     * then laid out to be easily retrieved.
     *
     * @return A ServerCalculations object with all the data decrypted
     */
    public ServerCalculations decryptStatsAndSummary() throws Exception {
        if(mServerCalculation==null){
            try {
                mServerCalculation =  new ServerCalculations(this);
                mServerCalculation.decrypt();
            } catch (Exception e) {
                throw new Exception("Run Item [RunNumber: "+getRunNumber()+" ]"+ e.getMessage());
            }
        }
        return mServerCalculation;
    }
}