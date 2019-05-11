using Microsoft.WindowsAzure.Storage;
using Microsoft.WindowsAzure.Storage.Blob;
using System;
using System.Configuration;
using System.Diagnostics;
using System.IO;

namespace AsureRunService.Utilities
{
    public static class KeyUtilities
    {
        /// <summary>
        /// Creates a SAS token to send to the client so they can upload their keys to the blob.
        /// </summary>
        /// <returns>The token as a string</returns>
        public static string GetAccountSASToken(string containerName)
        {
            CloudBlobContainer container = GetBlobContainer(containerName);
            container.CreateIfNotExists();

            // Create a new access policy for the account.
            SharedAccessBlobPolicy adHocPolicy = new SharedAccessBlobPolicy()
            {
                // Set start time to five minutes before now to avoid clock skew.
                SharedAccessStartTime = DateTime.UtcNow.AddMinutes(-5),
                SharedAccessExpiryTime = DateTime.UtcNow.AddMinutes(10),
                Permissions = SharedAccessBlobPermissions.Write | SharedAccessBlobPermissions.List
            };

            // Return the SAS token.
            return container.Uri + container.GetSharedAccessSignature(adHocPolicy, null);
        }

        /// <summary>
        /// Get the keys from the blob storage
        /// </summary>
        /// <param name="keyId">ID of keys to get</param>
        /// <param name="galKey">Resulting Galois Keys</param>
        /// <param name="galSingleStepKey">Resulting Galois Keys for single step</param>
        /// <param name="relinKey">Resulting relinearization keys</param>
        public static void GetKeys(string keyId, ref string galKey, ref string galSingleStepKey, ref string relinKey)
        {
            // Get the keys from the blob
            // The following assumes that the keys are there
            bool cacheLoaded = false; 
            if (IsLocalCacheExists(keyId))
            {
                try
                {
                    GetLocalCache(keyId, ref galKey,ref galSingleStepKey,ref relinKey);
                    cacheLoaded = true; 
                } catch (Exception) { }
            } 

            if (!cacheLoaded)
            {
                CloudBlobContainer cloudBlobContainer = GetBlobContainer(keyId);
                cloudBlobContainer.FetchAttributes();
                StreamReader galStream = new StreamReader(cloudBlobContainer.GetBlobReference("galoisKey").OpenRead());
                StreamReader galSingleStepStream = new StreamReader(cloudBlobContainer.GetBlobReference("galoisSingleStepKey").OpenRead());
                StreamReader relinStream = new StreamReader(cloudBlobContainer.GetBlobReference("relinearizeKey").OpenRead());
                galKey = galStream.ReadToEnd();
                galSingleStepKey = galSingleStepStream.ReadToEnd();
                relinKey = relinStream.ReadToEnd();

                StoreLocalCache(keyId, galKey, galSingleStepKey, relinKey);
            }
        }

        private class FileNames
        {
            public static readonly string BaseFolder = @"d:\home\site\data\";
            public static readonly string GaloisKeysFile = "galKey";
            public static readonly string GaloisSingleStepKeysFile = "galSingleStepKey";
            public static readonly string RelinKeysFile = "relinKey";
        }

        /// <summary>
        /// Get the Storage Connection String
        /// </summary>
        private static string StorageConnectionString
        {
            get
            {
                string storageConnectionString = ConfigurationManager.AppSettings["StorageConnectionString"];
                if (string.IsNullOrEmpty(storageConnectionString))
                {
                    throw new ConfigurationErrorsException("StorageConnectionString not found");
                }

                return storageConnectionString;
            }
        }

        /// <summary>
        /// Get Blob Container for a given Key ID
        /// </summary>
        /// <param name="keyId">Key ID to get Blob Container for</param>
        private static CloudBlobContainer GetBlobContainer(string keyId)
        {
            CloudStorageAccount.TryParse(StorageConnectionString, out CloudStorageAccount storageAccount);
            CloudBlobClient cloudBlobClient = storageAccount.CreateCloudBlobClient();
            CloudBlobContainer cloudBlobContainer = cloudBlobClient.GetContainerReference(keyId);
            return cloudBlobContainer;
        }

        /// <summary>
        /// Whether the given Key ID is stored in the local cache
        /// </summary>
        /// <param name="keyId">Key ID to check</param>
        public static bool IsLocalCacheExists(string keyId)
        {
            return Directory.Exists(Path.Combine(FileNames.BaseFolder, keyId));
        } 

        /// <summary>
        /// Store the given keys in the local cache
        /// </summary>
        /// <param name="keyId">Key ID</param>
        /// <param name="galKey">Galois keys</param>
        /// <param name="galSingleStepKey">Galois keys for single step</param>
        /// <param name="relinKey">Relinearization keys</param>
        public static void StoreLocalCache(string keyId, string galKey,string galSingleStepKey,string relinKey)
        {
            if (keyId == null) return; 

            string folderPath = Path.Combine(FileNames.BaseFolder, keyId) ;
            try
            {
                Directory.CreateDirectory(folderPath);

                //write 3 files 
                string filename = Path.Combine(folderPath, FileNames.GaloisKeysFile);
                using (StreamWriter writer = new StreamWriter(File.Create(filename)))
                {
                    writer.Write(galKey);
                }

                filename = Path.Combine(folderPath, FileNames.GaloisSingleStepKeysFile);
                using (StreamWriter writer = new StreamWriter(File.Create(filename)))
                {
                    writer.Write(galSingleStepKey);
                }

                filename = Path.Combine(folderPath, FileNames.RelinKeysFile);
                using (StreamWriter writer = new StreamWriter(File.Create(filename)))
                {
                    writer.Write(relinKey);
                }


            }
            catch (Exception e)
            {
                //Clear the directory 
                Directory.Delete(folderPath);
                Console.WriteLine("{0} Exception caught.", e);

            }
        }

        /// <summary>
        /// Delete Blob Container for the given Key ID
        /// </summary>
        /// <param name="keyId">Key ID that identifies the blob container to delete</param>
        public static void DeleteBlobContainer(string keyId)
        {
            if (null == keyId)
                throw new ArgumentNullException(nameof(keyId));

            Trace.TraceWarning($"DeleteBlobContainer for {keyId}");

            CloudBlobContainer blobContainer = GetBlobContainer(keyId);
            blobContainer.DeleteIfExists();
        }

        /// <summary>
        /// Delete local cache for the given Key ID
        /// </summary>
        /// <param name="keyId">Key ID to delete</param>
        public static void DeleteLocalCache(string keyId)
        {
            if (null == keyId)
                throw new ArgumentNullException(nameof(keyId));

            Trace.TraceWarning($"DeleteLocalCache for {keyId}");

            string folderPath = Path.Combine(FileNames.BaseFolder, keyId);

            try
            {
                Directory.Delete(folderPath, recursive: true);
            }
            catch (Exception e)
            {
                Console.WriteLine("{0} exception caught.", e);
            }
        }

        /// <summary>
        /// Get keys from the local cache
        /// </summary>
        /// <param name="keyId">Key ID of keys to get</param>
        /// <param name="galKey">Resulting Galois Keys</param>
        /// <param name="galSingleStepKey">Resulting Galois Keys for single step</param>
        /// <param name="relinKey">Resulting Relinearization Keys</param>
        public static void GetLocalCache(string keyId, ref string galKey, ref string galSingleStepKey, ref string relinKey)
        {
            if (keyId == null) return;

            Trace.TraceInformation($"GetLocalCache for {keyId}");

            string folderPath = Path.Combine(FileNames.BaseFolder, keyId);
            try
            {
                string filename = Path.Combine(folderPath, FileNames.GaloisKeysFile);
                using (StreamReader reader = new StreamReader(filename))
                {
                    galKey = reader.ReadToEnd(); 
                }

                filename = Path.Combine(folderPath, FileNames.GaloisSingleStepKeysFile);
                using (StreamReader reader = new StreamReader(filename))
                {
                    galSingleStepKey = reader.ReadToEnd();
                }

                filename = Path.Combine(folderPath, FileNames.RelinKeysFile);
                using (StreamReader reader = new StreamReader(filename))
                {
                    relinKey = reader.ReadToEnd();
                }

            }
            catch (Exception e)
            {
                throw new System.Exception("Invalid cache", e);
            }
        }

    }
    
}