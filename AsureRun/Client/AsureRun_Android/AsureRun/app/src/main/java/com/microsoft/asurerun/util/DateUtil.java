package com.microsoft.asurerun.util;

import android.content.Context;

import com.microsoft.asurerun.R;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DateUtil {
    /**
     * @return the phases of the day
     */
    public static String getDayPhases(Context context) {
        Calendar c = Calendar.getInstance();
        int hours = c.get(Calendar.HOUR_OF_DAY);

        if (hours >= 1 && hours <= 12) {
            return context.getString(R.string.morning_string);
        } else if (hours >= 12 && hours <= 16) {
            return context.getString(R.string.afternoon_string);
        } else if (hours >= 16 && hours <= 21) {
            return context.getString(R.string.evening_string);
        } else if (hours >= 21 && hours <= 24) {
            return context.getString(R.string.night_string);
        }
        return "";
    }

    /**
     * @return the phases of the day from Calendar
     */
    public static String getDayPhases(Context context,Calendar calendar) {
        int hours = calendar.get(Calendar.HOUR_OF_DAY);

        if (hours >= 1 && hours <= 12) {
            return context.getString(R.string.morning_string);
        } else if (hours >= 12 && hours <= 16) {
            return context.getString(R.string.afternoon_string);
        } else if (hours >= 16 && hours <= 21) {
            return context.getString(R.string.evening_string);
        } else if (hours >= 21 && hours <= 24) {
            return context.getString(R.string.night_string);
        }
        return "";
    }

    /**
     *
     * @param calendar
     * @return formatted string date from calendar object
     */
    public static String getFormattedDateFromCalendar(Calendar calendar) {
        Date date = calendar.getTime();
        SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH);
        return format.format(date);
    }

    /**
     * @param timeInSeconds the time in seconds
     * @return formatted string time
     */
    public static String getFormattedTimeFromSeconds(long timeInSeconds) {
        String mTime = String.format("%02d:%02d",
                TimeUnit.SECONDS.toMinutes(timeInSeconds),
                TimeUnit.SECONDS.toSeconds(timeInSeconds) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(timeInSeconds))
        );
        return mTime;
    }

    /**
     * @param timeInMilliSeconds the time in milliseconds
     * @return formatted string time
     */
    public static String getFormattedTimeFromMilliseconds(long timeInMilliSeconds) {
        String mTime = String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(timeInMilliSeconds),
                TimeUnit.MILLISECONDS.toSeconds(timeInMilliSeconds) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeInMilliSeconds))
        );
        return mTime;
    }

    public static Date getTimeFromSeconds(double secondsSinceMidnight)
    {
        int hour = (int) secondsSinceMidnight / 3600;
        int minutes  = (int) secondsSinceMidnight % 60;
        int seconds = (int) secondsSinceMidnight % 3600;

        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minutes);
        c.set(Calendar.SECOND, seconds);

        return c.getTime();
    }

    public static String getFormattedDateFromMillis(long millis){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS",Locale.US);
        Calendar calendar = new GregorianCalendar(Locale.US);
        calendar.setTimeInMillis(millis);
        return sdf.format(calendar.getTime());
    }
}
