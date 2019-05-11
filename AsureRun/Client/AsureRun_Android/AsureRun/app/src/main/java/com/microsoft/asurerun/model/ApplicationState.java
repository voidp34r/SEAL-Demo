// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.asurerun.model;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.microsoft.asurerun.service.OnTaskListener;
import com.microsoft.asurerun.service.SyncDataService;
import com.microsoft.asurerun.ui.HomeActivity;
import com.microsoft.asurerun.util.AzureFileManager;
import com.microsoft.asurerun.util.CacheUtil;
import com.microsoft.asurerun.util.Configuration;
import com.microsoft.asurerun.util.Utils;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.MobileServiceException;
import com.microsoft.windowsazure.mobileservices.MobileServiceList;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.microsoft.windowsazure.mobileservices.table.sync.MobileServiceSyncTable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.microsoft.asurerun.util.AuthUtil.logout;
import static com.microsoft.asurerun.util.CacheUtil.*;
import static com.microsoft.asurerun.util.DialogUtil.createAndShowDialogFromTask;
import static com.microsoft.asurerun.util.ServiceUtil.*;

// todo we should make this class a Singleton
public class ApplicationState {
    private static final String KEY_ID_FILE = "keyId";
    private static final String PUBLIC_KEY = "publicKey";
    private static final String SECRET_KEY = "secretKey";
    private static final String GALOIS_KEY = "galoisKey";
    private static final String GALOIS_SINGLE_STEP_KEY = "galoisSingleStepKey";
    private static final String RELINEARIZATION_KEY = "relinearizeKey";
    private static final String TAG = "APP_STATE";
    private static final String OFFLINE_TABLE_NAME = "OfflineStore";
    // the number of items that are loaded the first time
    public static  final int INITIAL_ITEMS_OFFSET = 7;

    public static int maxRunNumber;
    private static String mFileStorageDirectory;
    private static boolean isKeysCreated;

    /**
     * The native code handle used to encrypt and decrypt data
     */
    private static long mCryptoContext;

    /**
     * Mobile Service Tables used to access data
     */
    private static MobileServiceTable<KeyItem> mKeyTable;
    private static MobileServiceTable<RunItem> mRunTable;
    private static MobileServiceTable<SummaryItem> mSummaryTable;
    private static KeyItem mInsertedKeyItem;
    private static RunItemDbHelper mLocalRunDb;

    private static RunItem selectedRunItem;

    // the shared lists between RunActivity and RunSummaryActivity
    private static List<Double> mElevationGainDeltas;
    private static List<Double> mAvgPaceValList;

    public static List<Double> getElevationGainDeltas() {
        return mElevationGainDeltas;
    }

    public static List<Double> getAvgPaceValList() { return mAvgPaceValList; }
    public static void setAvgPaceValList(List<Double> list) { mAvgPaceValList = list; }

    /**
     * Mobile Service Client reference
     */
    private static MobileServiceClient mClient;
    // the authentication provider
    private static String mAuthProvider;

    private static ArrayList<RunItem> mLocalRunItems;

    public static String getKeyIdFile(){ return KEY_ID_FILE; }

    public static long getCryptoContext() {
        return mCryptoContext;
    }

    public static MobileServiceTable<KeyItem> getKeyTable() {
        return mKeyTable;
    }

    public static MobileServiceTable<RunItem> getRunTable() {
        return mRunTable;
    }

    public static MobileServiceTable<SummaryItem> getSummaryTable() {
        return mSummaryTable;
    }

    public static KeyItem getInsertedKeyItem() {
        return mInsertedKeyItem;
    }

    public static MobileServiceSyncTable<RunItem> getOfflineSyncTable() {
        return mClient.getSyncTable(OFFLINE_TABLE_NAME, RunItem.class);
    }

    public static RunItem getSelectedRunItem() {
        return selectedRunItem;
    }

    public static void setSelectedRunItem(RunItem item) {
        selectedRunItem = item;
    }

    public static void setAuthProvider(String authProvider) {
        mAuthProvider = authProvider;
    }

    public static String getAuthProvider() {
        return mAuthProvider;
    }

    public static boolean isKeysCreated() {
        return isKeysCreated;
    }

    public static void setIsKeysCreated(boolean bIsKeysCreated) {
        isKeysCreated = bIsKeysCreated;
    }

    /**
     * Create the tables (only happens after authentication)
     */
    public static void createTable(Context context) throws InterruptedException, ExecutionException {
        // Get the Mobile Service Table instance to use
        mKeyTable = mClient.getTable(KeyItem.class);
        mRunTable = mClient.getTable(RunItem.class);
        mSummaryTable = mClient.getTable(SummaryItem.class);
        initLocalStore(context);
    }

    /**
     * Initialize run variables
     */
    public static void initializeRunData() {
        mAvgPaceValList = new ArrayList<>();
        mElevationGainDeltas = new ArrayList<>();
    }

    /**
     * Initialize local storage
     *
     * @return
     */
    private static void initLocalStore(final Context context) {
        mLocalRunItems = new ArrayList<>();
        mLocalRunDb = new RunItemDbHelper(context);
    }

    public static void storeLocalRunItem(RunItem runItem) {
        mLocalRunDb.insert(runItem);
    }

    public static void storeLocalRunItems(RunItem[] runItems) {
        for(RunItem runItem : runItems)
            mLocalRunDb.insert(runItem);
    }

    public static void storeLocalRunItems(Collection<RunItem> runItems) {
        for(RunItem runItem : runItems)
            mLocalRunDb.insert(runItem);
    }

    public static ArrayList<RunItem> getLocalRunItems() {
        return mLocalRunItems;
    }
    public static RunItem[] getLocalRunItemsDb() { return mLocalRunDb.getRunItems(); }

    public static void deleteLocalRunItems() {
        mLocalRunDb.deleteAllRunItems();
    }

    public static void deleteLocalRunItem(RunItem runItem) {
        mLocalRunDb.deleteRunItem(runItem);
    }

    public static void setMobileClient(MobileServiceClient client) {
        mClient = client;
    }

    public static MobileServiceClient getMobileClient() {
        return mClient;
    }

    public static void createCryptoContext(Context context, String fileStorageDirectory, int polyModulus, int scale, OnTaskListener onTaskListener) throws Exception {
        isKeysCreated = false;
        mFileStorageDirectory = fileStorageDirectory + "/";
        mCryptoContext = nativeCreateCryptoContext(mFileStorageDirectory, polyModulus, scale);
        nativeLoadLocalKeys(mCryptoContext, mFileStorageDirectory + PUBLIC_KEY, mFileStorageDirectory + SECRET_KEY);
        onTaskListener.onTaskCompleted(STATE_PUBLIC);
        // Only send keys if the key is not already not on the server
        File file = new File(fileStorageDirectory, KEY_ID_FILE);
        if (file.exists()) {
            try {
                FileInputStream fileStream = new FileInputStream(file);
                byte[] fileData = new byte[(int) file.length()];
                fileStream.read(fileData);
                fileStream.close();
                String id = new String(fileData);
                mInsertedKeyItem = mKeyTable.lookUp(id).get(); // If this throws that means the KeyItem doesn't exist on the server
                // If the server's moduli don't match with the client's the server's key is invalid and must receive new keys from the client
                if (mInsertedKeyItem.getSlotCount() * 2 != Configuration.getPolyModulus() || mInsertedKeyItem.getInitialScale() != Configuration.getInitialScale()) {
                    sendKeys(context, onTaskListener);
                }
            } catch (Exception e) {
                sendKeys(context, onTaskListener);
            }
        } else {
            sendKeys(context, onTaskListener);
            // delete all user data (because the keys have changed)
            SyncDataService.startDeleteAllDataOnFirstStart(context);
        }
    }

    private static void sendKeys(Context context, OnTaskListener onTaskListener) throws Exception {
        KeyItem keyItem = new KeyItem(Configuration.getPolyModulus() / 2, Configuration.getInitialScale());
        keyItem.setUserEmail(CacheUtil.getUserMail(context));
        mInsertedKeyItem = mKeyTable.insert(keyItem).get();
        FileOutputStream file = new FileOutputStream(new File(mFileStorageDirectory, KEY_ID_FILE));
        file.write(mInsertedKeyItem.getId().getBytes());
        file.close();
        onTaskListener.onTaskCompleted(STATE_PRIVATE);
        nativeGenerateKeys(
                mCryptoContext,
                mFileStorageDirectory + PUBLIC_KEY,
                mFileStorageDirectory + SECRET_KEY,
                mFileStorageDirectory + GALOIS_KEY,
                mFileStorageDirectory + GALOIS_SINGLE_STEP_KEY,
                mFileStorageDirectory + RELINEARIZATION_KEY);
        sendKeyFromFile(mInsertedKeyItem.getId(), PUBLIC_KEY, false);
        sendKeyFromFile(mInsertedKeyItem.getId(), GALOIS_KEY, true);
        sendKeyFromFile(mInsertedKeyItem.getId(), GALOIS_SINGLE_STEP_KEY, true);
        sendKeyFromFile(mInsertedKeyItem.getId(), RELINEARIZATION_KEY, true);
        onTaskListener.onTaskCompleted(STATE_CLOUD);
    }

    /**
     * Upload a file with a given name to the Azure blob.
     *
     * @param containerName Azure blob container name
     * @param fileName Local file name. The same name will be used to upload to the blob.
     * @param delete Whether to delete the local file after uploading.
     * @throws IOException If an error happens reading the file
     * @throws URISyntaxException If the connection string to the blob has incorrect syntax
     * @throws InvalidKeyException If connection string to the blob has an invalid key
     * @throws StorageException If the blob client is unable to get a container reference
     * @throws ExecutionException if the blob client is unable to get a container reference
     * @throws InterruptedException if the blob client is unable to get a container reference
     */
    private static void sendKeyFromFile(String containerName, String fileName, boolean delete)
            throws IOException,
                   URISyntaxException,
                   InvalidKeyException,
                   StorageException,
                   ExecutionException,
                   InterruptedException {
        File file = new File(mFileStorageDirectory, fileName);
        if (file.exists()) {
            FileInputStream fileStream = new FileInputStream(file);
            AzureFileManager.UploadFile(fileStream, file.length(), containerName, fileName);
            if (delete)
                file.delete();
        } else
            throw new FileNotFoundException(fileName + " doesn't exist");
    }

    /**
     * Delete keys from local files and from Azure
     *
     * @throws FileNotFoundException if key file does not exist
     * @throws IllegalStateException if no inserted key exists
     * @throws URISyntaxException if Azure delete file operation is called with an incorrect Uri syntax
     * @throws InvalidKeyException if Azure delete file operation is called with an invalid key
     * @throws StorageException if the blob client is unable to get a container reference
     * @throws ExecutionException if the blob client is unable to get a container reference
     * @throws InterruptedException if the blob client is unable to get a container reference
     */
    public static void deleteKeys()
            throws IllegalStateException,
                   FileNotFoundException,
                   URISyntaxException,
                   InvalidKeyException,
                   StorageException,
                   ExecutionException,
                   InterruptedException {
        if (mInsertedKeyItem == null) {
            throw new IllegalStateException("No inserted key item");
        }

        // Delete keys from Azure
        mKeyTable.delete(mInsertedKeyItem);

        // Delete local keys
        deleteFile(KEY_ID_FILE);
        deleteFile(PUBLIC_KEY);
        deleteFile(SECRET_KEY);
    }

    /**
     * Delete a file from the file storage directory.
     *
     * @param fileName File name in file storage directory.
     * @throws FileNotFoundException If file is not found.
     */
    private static void deleteFile(String fileName) throws FileNotFoundException {
        File file = new File(mFileStorageDirectory, fileName);
        if (!file.exists()) {
            throw new FileNotFoundException("File does not exist: " + file.toString());
        }

        file.delete();
    }

    public static void deleteRunItems(final Context context) {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

            @Override
            protected void onPreExecute() {
                this.dialog.setTitle("Deleting all user data");
                this.dialog.setMessage("Working...");
                this.dialog.show();
                this.dialog.setCancelable(false);
                this.dialog.setCanceledOnTouchOutside(false);
            }

            private ProgressDialog dialog = new ProgressDialog(context);

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    ListenableFuture<MobileServiceList<RunItem>> items = getRunTable().execute();
                    Futures.addCallback(items, new FutureCallback<MobileServiceList<RunItem>>() {
                        @Override
                        public void onSuccess(MobileServiceList<RunItem> result) {
                            try {
                                for (RunItem item : result) {
                                    getRunTable().delete(item);
                                }
                                deleteKeys();
                                mLocalRunItems.clear();
                                mLocalRunDb.deleteAllRunItems();
                                SharedPreferences prefs = context.getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
                                String authProvider = prefs.getString(AUTH_PROVIDER, null);

                                // Dismiss dialog before we attempt to logout, otherwise it will leak.
                                dialog.dismiss();

                                logout(authProvider, context, mClient);

                                // clear backStack
                                ((HomeActivity)context).clearFragmentBackStack();
                            } catch (Exception e) {
                                Log.e(TAG, "Deleting key error", e);
                            } finally {
                                if (dialog.isShowing()) {
                                    dialog.dismiss();
                                }
                            }
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            Log.e(TAG, "Deleting key error", t);
                        }
                    }, AsyncTask.SERIAL_EXECUTOR);
                } catch (MobileServiceException e) {
                    createAndShowDialogFromTask(e, "Deleting data error", context);
                }
                return null;
            }
        };
        Utils.runAsyncTask(task);
    }

    public static void deleteRunItem(final Context context, final RunItem runItem) {
        final ProgressDialog dialog = new ProgressDialog(context);
        dialog.setTitle("Deleting run from server");
        dialog.setMessage("Working...");
        dialog.show();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        mLocalRunItems.remove(runItem);

        Futures.addCallback(getRunTable().delete(runItem), new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void v) {
                dialog.dismiss();
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e(TAG, "Deleting key error");
            }
        });
    }

    public static void releaseCryptoContext() {
        nativeReleaseCryptoContext(mCryptoContext);
    }

    public static String encrypt(double[] values) {
        return nativeEncrypt(mCryptoContext, values);
    }

    public static double[] decrypt(String base64Input) {
        return nativeDecrypt(mCryptoContext, base64Input);
    }

    /**
     * A native method that is implemented by the 'cryptoadapter' native library,
     * which is packaged with this application.
     */
    public native static long nativeCreateCryptoContext(String fileStorageDirectory, int polyModulus, int scale);

    public native static void nativeReleaseCryptoContext(long cryptoContext);

    public native static String nativeEncrypt(long cryptoContext, double[] values);

    public native static double[] nativeDecrypt(long cryptoContext, String input);

    public native static boolean nativeLoadLocalKeys(long cryptoContext, String publicKeyPath, String secretKeyPath);

    public native static void nativeGenerateKeys(
            long cryptoContext,
            String publicKeyPath,
            String secretKeyPath,
            String galoisKeyPath,
            String galoisSingleStepKeyPath,
            String relinearizeKeyPath);

}
