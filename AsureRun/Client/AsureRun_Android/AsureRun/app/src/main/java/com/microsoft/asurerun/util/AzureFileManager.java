// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.asurerun.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.concurrent.ExecutionException;

import com.google.android.gms.auth.api.signin.internal.Storage;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonElement;
import com.microsoft.asurerun.BuildConfig;
import com.microsoft.asurerun.model.ApplicationState;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageCredentials;
import com.microsoft.azure.storage.StorageCredentialsSharedAccessSignature;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

/**
 * Manage Azure blobs
 */
public class AzureFileManager {
    /**
     * Requests a SAS token from the server
     * @return String to connect to blob with
     * @throws ExecutionException if the computation threw an exception
     * @throws InterruptedException if the current thread was interrupted
     */
    private static String getStorageConnectionString(String containerName) throws ExecutionException, InterruptedException {
        String sas = ApplicationState.getMobileClient().invokeApi("SAS/" + containerName).get().toString();

        //The string comes back with extra quotes around it, so remove them
        return sas.replace("\"", "");
    }

    /**
     * Get a container with a given name
     * @param containerName Container name
     * @return CloudBlobContainer instance
     * @throws URISyntaxException if containerName contains incorrect Uri syntax
     * @throws InvalidKeyException if containerName contains an invalid key
     * @throws StorageException if the blob client is unable to get a container reference
     * @throws ExecutionException if the blob client is unable to get a container reference
     * @throws InterruptedException if the blob client is unable to get a container reference
     */
    private static CloudBlobContainer getContainer(String containerName)
            throws URISyntaxException,
                   StorageException,
                   ExecutionException,
                   InterruptedException {
        //Return a reference to the container using the SAS URI.
        return new CloudBlobContainer(new URI(getStorageConnectionString(containerName)));
    }

    /**
     * Delete a blob container with the given name
     *
     * @param containerName Container name
     * @throws URISyntaxException if containerName contains incorrect Uri syntax
     * @throws InvalidKeyException if containerName contains an invalid key
     * @throws StorageException if the blob client is unable to get a container reference
     * @throws ExecutionException if the blob client is unable to get a container reference
     * @throws InterruptedException if the blob client is unable to get a container reference
     */
    public static void DeleteBlobContainer(String containerName)
            throws URISyntaxException,
                   StorageException,
                   ExecutionException,
                   InterruptedException {
        CloudBlobContainer container = getContainer(containerName);
        container.deleteIfExists();
    }

    /**
     * Upload a file to Azure
     * @param file File to upload
     * @param fileSize Size of file to upload
     * @param containerName Name of the Azure Blob Container
     * @param destinationName File name
     * @throws URISyntaxException if containerName contains incorrect Uri syntax
     * @throws InvalidKeyException if containerName contains an invalid key
     * @throws StorageException if the blob client is unable to get a container reference
     * @throws IOException if there is an error reading the file
     * @throws ExecutionException if the blob client is unable to get a container reference
     * @throws InterruptedException if the blob client is unable to get a container reference
     */
    public static void UploadFile(InputStream file, long fileSize, String containerName, String destinationName)
            throws URISyntaxException,
                   StorageException,
                   IOException,
                   ExecutionException,
                   InterruptedException {
        CloudBlobContainer container = getContainer(containerName);

        CloudBlockBlob fileBlob = container.getBlockBlobReference(destinationName);
        fileBlob.upload(file, fileSize);
    }

    /**
     * Delete an existing file from Azure.
     * @param containerName Name of the Azure Blob Container
     * @param destinationName File name
     * @throws URISyntaxException if containerName contains incorrect Uri syntax
     * @throws InvalidKeyException if containerName contains an invalid key
     * @throws StorageException if the blob client is unable to get a container reference
     * @throws ExecutionException if the blob client is unable to get a container reference
     * @throws InterruptedException if the blob client is unable to get a container reference
     */
    public static void DeleteFile(String containerName, String destinationName)
            throws URISyntaxException,
                   StorageException,
                   ExecutionException,
                   InterruptedException {
        CloudBlobContainer container = getContainer(containerName);

        if (!container.exists()) {
            throw new IllegalArgumentException("Container does not exist");
        }

        CloudBlockBlob fileBlob = container.getBlockBlobReference(destinationName);
        fileBlob.deleteIfExists();
    }
}
