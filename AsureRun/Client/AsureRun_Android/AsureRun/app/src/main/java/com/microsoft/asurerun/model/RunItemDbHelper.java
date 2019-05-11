// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.asurerun.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.microsoft.asurerun.model.RunItem.RunItemEntry;

public class RunItemDbHelper extends SQLiteOpenHelper {
    private static final String TAG = "RunItemDbHelper";

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + RunItemEntry.TABLE_NAME + " (" +
                    RunItemEntry._ID + " INTEGER PRIMARY KEY, " +
                    RunItemEntry.COLUMN_NAME_RUN_NUMBER + " INTEGER, " +
                        RunItemEntry.COLUMN_NAME_CIPHER_1 + " MEDIUMTEXT, " +
                    RunItemEntry.COLUMN_NAME_CIPHER_2 + " MEDIUMTEXT, " +
                    RunItemEntry.COLUMN_NAME_CIPHER_EP + " MEDIUMTEXT, " +
                    RunItemEntry.COLUMN_NAME_CIPHER_THUMBNAIL + " MEDIUMTEXT, " +
                    RunItemEntry.COLUMN_NAME_STATS + " MEDIUMTEXT, " +
                    RunItemEntry.COLUMN_NAME_SUMMARY + " MEDIUMTEXT)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + RunItemEntry.TABLE_NAME;

    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "RunItem.db";

    public RunItemDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public void insert(RunItem runItem) {
        ContentValues values = new ContentValues();

        values.put(RunItemEntry.COLUMN_NAME_RUN_NUMBER, runItem.getRunNumber());

        insertStringHelper(values, RunItemEntry.COLUMN_NAME_CIPHER_1, runItem.getCipher1());
        insertStringHelper(values, RunItemEntry.COLUMN_NAME_CIPHER_2, runItem.getCipher2());
        insertStringHelper(values, RunItemEntry.COLUMN_NAME_CIPHER_EP, runItem.getCipherEP());
        insertStringHelper(values, RunItemEntry.COLUMN_NAME_CIPHER_THUMBNAIL, runItem.getCipherThumbnail());
        insertStringHelper(values, RunItemEntry.COLUMN_NAME_STATS, runItem.getStats());
        insertStringHelper(values, RunItemEntry.COLUMN_NAME_SUMMARY, runItem.getSummary());

        getWritableDatabase().insert(RunItemEntry.TABLE_NAME, null, values);
    }

    private void insertStringHelper(ContentValues values, String key, String value) {
        if(value != null)
            values.put(key, value);
        else
            values.putNull(key);
    }

    public void deleteAllRunItems() {
        onUpgrade(getWritableDatabase(), 1, 1);
    }

    public void deleteRunItem(RunItem runItem) {
        // Define 'where' part of query.
        String selection = RunItemEntry.COLUMN_NAME_RUN_NUMBER + " LIKE ?";
        // Specify arguments in placeholder order.
        String[] selectionArgs = { String.valueOf(runItem.getRunNumber()) };
        // Issue SQL statement.
        getWritableDatabase().delete(RunItemEntry.TABLE_NAME, selection, selectionArgs);
    }

    public RunItem[] getRunItems() {
        // Need to do all these split up projections because moveToNext always fails when there's too many columns, probably due to each cipher being rather large
        String[] projection = {
            RunItemEntry.COLUMN_NAME_RUN_NUMBER,
            RunItemEntry.COLUMN_NAME_CIPHER_1,
            RunItemEntry.COLUMN_NAME_CIPHER_2
        };
        Cursor cursor = getReadableDatabase().query(RunItemEntry.TABLE_NAME, projection, null, null, null, null, RunItemEntry.COLUMN_NAME_RUN_NUMBER + " DESC");
        RunItem[] runItems = new RunItem[cursor.getCount()];
        for(int i = 0; i < runItems.length; ++i)
            runItems[i] = new RunItem();
        while(cursor.moveToNext()) {
            RunItem runItem = runItems[cursor.getPosition()];
            runItem.setRunNumber(cursor.getInt(cursor.getColumnIndex(RunItemEntry.COLUMN_NAME_RUN_NUMBER)));
            runItem.setCipher1(cursor.getString(cursor.getColumnIndex(RunItemEntry.COLUMN_NAME_CIPHER_1)));
            runItem.setCipher2(cursor.getString(cursor.getColumnIndex(RunItemEntry.COLUMN_NAME_CIPHER_2)));
        }
        cursor.close();

        String[] projection2 = {
            RunItemEntry.COLUMN_NAME_RUN_NUMBER,
            RunItemEntry.COLUMN_NAME_CIPHER_EP,
            RunItemEntry.COLUMN_NAME_CIPHER_THUMBNAIL
        };
        cursor = getReadableDatabase().query(RunItemEntry.TABLE_NAME, projection2, null, null, null, null, RunItemEntry.COLUMN_NAME_RUN_NUMBER + " DESC");
        while(cursor.moveToNext()) {
            RunItem runItem = runItems[cursor.getPosition()];
            runItem.setCipherEP(cursor.getString(cursor.getColumnIndex(RunItemEntry.COLUMN_NAME_CIPHER_EP)));
            runItem.setCipherThumbnail(cursor.getString(cursor.getColumnIndex(RunItemEntry.COLUMN_NAME_CIPHER_THUMBNAIL)));
        }
        cursor.close();

        String[] projection3 = {
            RunItemEntry.COLUMN_NAME_RUN_NUMBER,
            RunItemEntry.COLUMN_NAME_STATS,
            RunItemEntry.COLUMN_NAME_SUMMARY
        };
        cursor = getReadableDatabase().query(RunItemEntry.TABLE_NAME, projection3, null, null, null, null, RunItemEntry.COLUMN_NAME_RUN_NUMBER + " DESC");
        while(cursor.moveToNext()) {
            RunItem runItem = runItems[cursor.getPosition()];
            runItem.setStats(cursor.getString(cursor.getColumnIndex(RunItemEntry.COLUMN_NAME_STATS)));
            runItem.setSummary(cursor.getString(cursor.getColumnIndex(RunItemEntry.COLUMN_NAME_SUMMARY)));
        }
        cursor.close();

        return runItems;
    }
}
