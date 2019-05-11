// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.asurerun.model;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.microsoft.asurerun.util.Checks;
import com.microsoft.asurerun.util.DateUtil;
import com.microsoft.asurerun.util.MapUtil;
import com.microsoft.asurerun.util.Utils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import static com.microsoft.asurerun.util.MapUtil.MAP_THUMBNAIL_HEIGHT;
import static com.microsoft.asurerun.util.MapUtil.MAP_THUMBNAIL_WIDTH;

public class ServerCalculations {

    private RunItem mRunItem;

    private double mTotalDistanceKm;
    private double mTotalTimeSec;
    private double mElevationGain;
    private Bitmap mThumbnail;
    private Calendar mDate;
    private LatLng[] mCoordinates;
    private List<Double> mElevationGainDeltas;
    private List<Double> mAvgPaceDeltas;
    private double[] mTimeStamps;
    private double mMlResult;

    public static final int CIPHER_SIZE = 4096;

    public static final double EARTH_RADIUS = 6371.0;

    public static final int SUMMARY_DAY_OF_WEEK_OFFSET = 100;
    public static final int SUMMARY_WEEK_OF_YEAR_OFFSET = SUMMARY_DAY_OF_WEEK_OFFSET + 7;
    public static final int SUMMARY_DAY_OFFSET = SUMMARY_WEEK_OF_YEAR_OFFSET + 53;
    public static final int SUMMARY_DATE_SIZE = SUMMARY_DAY_OFFSET + 366;
    public static final int SUMMARY_ELEVATION_GAIN_OFFSET = SUMMARY_DATE_SIZE;

    private final static String TAG = ServerCalculations.class.getCanonicalName();

    public ServerCalculations(RunItem runItem) {
        this.mRunItem = runItem;
    }

    public void processAndEncrypt(ArrayList<Double> cartesianX,
                                  ArrayList<Double> cartesianY,
                                  ArrayList<Double> cartesianZ,
                                  ArrayList<Double> timeStamps,
                                  ArrayList<float[]> gyroscopeValues,
                                  ArrayList<float[]> accelerometerValues,
                                  ArrayList<Double> elevationGainDeltas,
                                  ArrayList<Double> avgPaceDelta, double mElevationGainSum, Bitmap mapSnapshot) {

        Log.e(TAG, "Timestamps: " + timeStamps.toString());
        Log.e(TAG, "CartesianX: " + cartesianX.toString());
        Log.e(TAG, "CartesianY: " + cartesianY.toString());
        Log.e(TAG, "CartesianZ: " + cartesianZ.toString());

        double[] arrayXY = new double[CIPHER_SIZE];
        double[] arrayZT = new double[CIPHER_SIZE];
        double[] arrayEG = new double[CIPHER_SIZE];
        int halfCipherSize = CIPHER_SIZE / 2;
        for (int i = 0; i < elevationGainDeltas.size(); ++i) {
            arrayEG[i] = elevationGainDeltas.get(i);
            arrayEG[i + halfCipherSize] = avgPaceDelta.get(i);
        }
        for (int i = 0; i < cartesianX.size(); ++i) {
            arrayXY[i] = cartesianX.get(i);
            arrayXY[i + halfCipherSize] = cartesianY.get(i);
            arrayZT[i] = cartesianZ.get(i);
            arrayZT[i + halfCipherSize] = timeStamps.get(i);
        }
        double lastX = 0;
        double lastY = 0;
        double lastZ = 0;
        double lastT = 0;
        if (!cartesianX.isEmpty()) {
            lastX = arrayXY[cartesianX.size() - 1];
            lastY = arrayXY[cartesianX.size() - 1 + halfCipherSize];
            lastZ = arrayZT[cartesianX.size() - 1];
        }
        if (!timeStamps.isEmpty())
            lastT = timeStamps.get(timeStamps.size() - 1);
        for (int i = cartesianX.size(); i < timeStamps.size(); ++i) {
            arrayXY[i] = lastX;
            arrayXY[i + halfCipherSize] = lastY;
            arrayZT[i] = lastZ;
            arrayZT[i + halfCipherSize] = timeStamps.get(i);
        }
        for (int i = timeStamps.size(); i < halfCipherSize; ++i) {
            arrayXY[i] = lastX;
            arrayXY[i + halfCipherSize] = lastY;
            arrayZT[i] = lastZ;
            arrayZT[i + halfCipherSize] = lastT;
        }

        double averageGyroX = 0;
        double averageGyroY = 0;
        double averageGyroZ = 0;
        double averageAccX = 0;
        double averageAccY = 0;
        double averageAccZ = 0;
        for(int i = 0; i < gyroscopeValues.size(); ++i) {
            averageGyroX += gyroscopeValues.get(i)[0];
            averageGyroY += gyroscopeValues.get(i)[1];
            averageGyroZ += gyroscopeValues.get(i)[2];
            averageAccX += accelerometerValues.get(i)[0];
            averageAccY += accelerometerValues.get(i)[1];
            averageAccZ += accelerometerValues.get(i)[2];
        }
        if(gyroscopeValues.size() > 0) {
            averageGyroX /= gyroscopeValues.size();
            averageGyroY /= gyroscopeValues.size();
            averageGyroZ /= gyroscopeValues.size();
            averageAccX /= gyroscopeValues.size();
            averageAccY /= gyroscopeValues.size();
            averageAccZ /= gyroscopeValues.size();
        }
        double[] gyroData = new double[CIPHER_SIZE];
        gyroData[0] = averageGyroX;
        gyroData[1] = averageGyroY;
        gyroData[2] = averageGyroZ;
        gyroData[3] = averageAccX;
        gyroData[4] = averageAccY;
        gyroData[5] = averageAccZ;

        Calendar today = Calendar.getInstance();
        int offset = 0;
        int yearofCentury = today.get(Calendar.YEAR) % 100;
        offset += 100;
        int dayOfWeek = today.get(Calendar.DAY_OF_WEEK) - 1 + offset; //[Sunday-Saturday] maps to [1-7] so subtract 1
        offset += 7;
        int weekOfYear = today.get(Calendar.WEEK_OF_YEAR) - 1 + offset; //first week is 1 so subtract
        offset += 53;
        int dayOfYear = today.get(Calendar.DAY_OF_YEAR) - 1 + offset; //first day is 1 so subtract 1
        offset += 366;
        double[] summaryMask = new double[CIPHER_SIZE];
        summaryMask[yearofCentury] = 1;
        summaryMask[halfCipherSize + yearofCentury] = 1;
        summaryMask[dayOfWeek] = 1;
        summaryMask[halfCipherSize + dayOfWeek] = 1;
        summaryMask[weekOfYear] = 1;
        summaryMask[halfCipherSize + weekOfYear] = 1;
        summaryMask[dayOfYear] = 1;
        summaryMask[halfCipherSize + dayOfYear] = 1;
        summaryMask[SUMMARY_ELEVATION_GAIN_OFFSET] = mElevationGainSum;
        System.arraycopy(summaryMask, 0, summaryMask, CIPHER_SIZE / 2, CIPHER_SIZE / 2);
        if (mapSnapshot != null) {
            double[] doublePixels = new double[CIPHER_SIZE];
            int [] intPixels = new int[CIPHER_SIZE];
            mapSnapshot.getPixels(intPixels, 0, MAP_THUMBNAIL_WIDTH, 0, 0, MAP_THUMBNAIL_WIDTH, MAP_THUMBNAIL_HEIGHT);
            for(int i = 0; i < intPixels.length; i++){
                doublePixels[i] = intPixels[i];
            }
            String encryptThumbnail = ApplicationState.encrypt(doublePixels);
            this.mRunItem.setCipherThumbnail(encryptThumbnail);
        }
        this.mRunItem.setCipherEP(ApplicationState.encrypt(arrayEG));
        this.mRunItem.setCipher1(ApplicationState.encrypt(arrayXY));
        this.mRunItem.setCipher2(ApplicationState.encrypt(arrayZT));
        this.mRunItem.setSummary(ApplicationState.encrypt(summaryMask));
        this.mRunItem.setCipherGyro(ApplicationState.encrypt(gyroData));
    }

    public void decrypt() throws Exception {
        clearValues();
        if (Checks.isBase64(this.mRunItem.getCipher1()) && Checks.isBase64(this.mRunItem.getCipher2())) {
            double[] xy = ApplicationState.decrypt(this.mRunItem.getCipher1());
            double[] zt = ApplicationState.decrypt(this.mRunItem.getCipher2());
            mCoordinates = pullCoordinates(xy, zt);
            mTimeStamps = pullTimeStamps(zt);
        } else {
            throw new Exception("xyCipher is a bad Base64 string");
        }
        if (Checks.isBase64(this.mRunItem.getSummary())) {
            double[] summary = ApplicationState.decrypt(this.mRunItem.getSummary());
            mElevationGain = calculateElevationGain(summary);
            mDate = pullDate(summary, mTimeStamps);
        } else {
            throw new Exception("encryptedSummary is a bad Base64 string");
        }
        /*
        The stats array contains from [0, halfLength - 2] the squared delta between each two points
        sent up to the server. At [halfLength - 1] is the total time.
         */
        if (Checks.isBase64(this.mRunItem.getStats())) {
            double[] stats = ApplicationState.decrypt(this.mRunItem.getStats());
            mTotalTimeSec = stats[CIPHER_SIZE / 2 - 1];
            mTotalDistanceKm = sumUpDistances(stats, mTotalTimeSec);
        } else {
            throw new Exception("encryptedStats is a bad Base64 string");
        }
        if (Checks.isBase64(this.mRunItem.getCipherEP())) {
            double[] eG = ApplicationState.decrypt(this.mRunItem.getCipherEP());
            int halfCipherSize = CIPHER_SIZE / 2;
            for (int i = 0; i < Math.round(mTotalTimeSec / 2); ++i) {
                mElevationGainDeltas.add(eG[i]);
                mAvgPaceDeltas.add(eG[i + halfCipherSize]);
            }
        } else {
            throw new Exception("cipherEp is a bad Base64 string");
        }
        if (Checks.isBase64(this.mRunItem.getCipherThumbnail())) {
            double[] doublePixel = ApplicationState.decrypt(this.mRunItem.getCipherThumbnail());
            int[]pixel = new int[doublePixel.length];
            Bitmap bmp = Bitmap.createBitmap(MAP_THUMBNAIL_WIDTH, MAP_THUMBNAIL_HEIGHT, Bitmap.Config.ARGB_8888);
            for(int i = 0; i< pixel.length; i++)
                pixel[i]= Double.valueOf(doublePixel[i]).intValue();
            bmp.setPixels(pixel, 0, MAP_THUMBNAIL_WIDTH, 0, 0, MAP_THUMBNAIL_WIDTH, MAP_THUMBNAIL_HEIGHT);
            mThumbnail = bmp;
        } else {
            throw new Exception("cipherThumbnail is a bad Base64 string");
        }

        if (Checks.isBase64(this.mRunItem.getCipherGyro())) {
            double[] gemmResult = ApplicationState.decrypt(this.mRunItem.getCipherGyro());
            mMlResult = 1.0 / (1.0 + Math.exp(-gemmResult[0]));
        } else {
            throw new Exception("cipherGyro is a bad Base64 string");
        }
    }

    private void clearValues() {
        mTotalDistanceKm = 0;
        mTotalTimeSec = 0;
        mElevationGain = 0;
        mElevationGainDeltas = new ArrayList<>();
        mAvgPaceDeltas = new ArrayList<>();
    }

    private double sumUpDistances(double[] stats, double totalTime) {
        double distance = 0;
        int endCipher = CIPHER_SIZE / 2 - 1;
        for (int i = 0; i < endCipher && i < Math.round(totalTime / 2); ++i) {
            double stat = Math.abs(stats[i]);
            if (stat >= 0)
                distance += Math.sqrt(stat);
        }
        return distance;
    }

    private double calculateElevationGain(double[] summary) {
        // find first mask to divide elevation slot by
        for (int i = 0; i < 100; ++i)
            if (summary[i] > 0.0001)
                return summary[SUMMARY_ELEVATION_GAIN_OFFSET] / summary[i];
        return 0;
    }

    private Calendar pullDate(double[] summary, double[] timestamps) {

        int actualYear = 2000;
        int actualDay = 0;
        int actualHour = 0;

        for (int year = 0; year < SUMMARY_DAY_OFFSET; ++year)
            if (summary[year] > 1) { //TODO: The server seems to be down scaling the 1 to roughly this value. Change this back to 1 after fixing server side.
                actualYear = year + 2000; //TODO: maybe make this app persist beyond the year 2100
                break;
            }
        for (int day = 0; day < 366; ++day)
            if (summary[day + SUMMARY_DAY_OFFSET] > 1) { //TODO: The server seems to be down scaling the 1 to roughly this value. Change this back to 1 after fixing server side.
                actualDay = day + 1;// add 1 because first day starts at 1
                break;
            }

        if (timestamps != null && timestamps.length > 0) {
            double startTime = timestamps[0];
            actualHour = DateUtil.getTimeFromSeconds(startTime).getHours();
        }

        //Create date from day and Year
        Calendar calendar = new GregorianCalendar(actualYear, 1, 1);
        calendar.set(Calendar.DAY_OF_YEAR, actualDay);
        calendar.set(Calendar.HOUR_OF_DAY, actualHour);
        return calendar;
    }

    private LatLng[] pullCoordinates(double[] xy, double[] zt) {
        int halfCipherSize = CIPHER_SIZE / 2;
        LatLng[] coordinates = new LatLng[halfCipherSize];
        for (int i = 0; i < coordinates.length; ++i) {
            double x = xy[i];
            double y = xy[i + halfCipherSize];
            double z = zt[i];
            coordinates[i] = cartesianToLatLng(x, y, z);
        }
        return coordinates;
    }

    private LatLng cartesianToLatLng(double x, double y, double z) {
        x /= EARTH_RADIUS;
        y /= EARTH_RADIUS;
        z /= EARTH_RADIUS;
        double latitudeRadians = Math.asin(y);
        double longitudeRadians = Math.acos(z / Math.cos(latitudeRadians));
        // Accounts for inverse cosine's range of [0, pi] in case longitude is to the west
        if (x < 0)
            longitudeRadians = -longitudeRadians;
        return new LatLng(Math.toDegrees(latitudeRadians), Math.toDegrees(longitudeRadians));
    }

    private double[] pullTimeStamps(double[] zt) {
        int halfCipherSize = CIPHER_SIZE / 2;
        double[] timeStamps = new double[halfCipherSize];
        System.arraycopy(zt, halfCipherSize, timeStamps, 0, halfCipherSize);
        return timeStamps;
    }

    // Public getters

    /**
     * @return total distance in Kilometers
     */
    public double getTotalDistance() {
        return mTotalDistanceKm;
    }

    /**
     * @return total time in seconds
     */
    public double getTotalTime() {
        return mTotalTimeSec;
    }

    /**
     * @return average speed in s/km
     */
    public double getAverageSpeed() {
        if (this.mTotalDistanceKm >= 0.001) {
            return mTotalTimeSec / mTotalDistanceKm;
        }
        return 0;
    }

    public double getElevationGain() {
        return mElevationGain;
    }

    public Calendar getDate() {
        return mDate;
    }

    public LatLng[] getCoordinates() {
        return mCoordinates;
    }

    public double[] getTimeStamps() {
        return mTimeStamps;
    }

    public List<Double> getElevationGainDeltas() {
        return mElevationGainDeltas;
    }

    public List<Double> getAvgPaceDeltas() {
        return mAvgPaceDeltas;
    }

    public Bitmap getThumbnail() {
        return mThumbnail;
    }

    public double getMlResult() {
        return mMlResult;
    }

    @Override
    public String toString() {
        return "ServerCalculations{" +
                "mTotalDistanceKm=" + mTotalDistanceKm +
                ", mTotalTimeSec=" + mTotalTimeSec +
                ", mElevationGain=" + mElevationGain +
                ", mDate=" + mDate +
                ", mCoordinates=" + Arrays.toString(mCoordinates) +
                ", mTimeStamps=" + Arrays.toString(mTimeStamps) +
                '}';
    }
}
