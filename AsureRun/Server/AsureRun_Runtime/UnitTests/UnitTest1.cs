using System;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using System.Collections.Generic;
using System.IO;
using System.Runtime.InteropServices;
using Microsoft.Research;
using AsureRunService.Utilities;

namespace UnitTests
{
    [TestClass]
    public class UnitTest1
    {
        [TestMethod]
        public void TestGetKeys()
        {
            string galKeys = "";
            string galSingleStepKeys = "";
            string relinKeys = "";

            // Right now this is hard coded for testing.  If the table is recreated then the values
            // especially the ID needs to be updated for what is created in the database.
            KeyUtilities.GetKeys("fea872dde1814bdb951a033d42077e8a",
                ref galKeys, ref galSingleStepKeys, ref relinKeys);

            // Try to load the Keys into a Wrapper
            SEALWrapper sw = new SEALWrapper(4096);
            sw.LoadKeys(galKeys, galSingleStepKeys, relinKeys);

            // Get the keys back from the Wrapper
            string testGalKeys = sw.getGaloisKeys();
            string testGalSingleStepKeys = sw.getGaloisSingleStepKeys();
            string testRelinKeys = sw.getRelinKeys();

            // Test that they equal
            // Have to test each character at a time because the assert
            // grabs too much memory if you test 2 strings of this size.
            Assert.AreEqual(galKeys.Length, testGalKeys.Length);
            Assert.AreEqual(galSingleStepKeys.Length, testGalSingleStepKeys.Length);
            Assert.AreEqual(relinKeys.Length, testRelinKeys.Length);
            for (int i = 0; i < galKeys.Length; i++)
                Assert.AreEqual(galKeys[i], testGalKeys[i], "Gal Key Mismatch: " + i);
            for (int i = 0; i < galSingleStepKeys.Length; i++)
                Assert.AreEqual(galSingleStepKeys[i], testGalSingleStepKeys[i], "Gal Single-Step Key Mismatch: " + i);
            for (int i = 0; i < relinKeys.Length; i++)
                Assert.AreEqual(relinKeys[i], testRelinKeys[i], "Relin Key Mismatch: " + i);
        }

        [TestMethod]
        public void TestSEALWrapperClass()
        {
            // Create the test data
            int slotCount = 4096;
            Random rand = new Random();
            List<double> input = new List<double>(slotCount);
            for (int i = 0; i < slotCount; i++)
            {
                input.Add((double)(rand.Next(0, 500)) / 10.0);
            }

            // Test encrypt and decrypt using base64 strings
            // This also tests the saving and loading of Ciphertexts
            SEALWrapper sw = new SEALWrapper(slotCount, true, true);
            String encypted = sw.Encrypt(input);
            List<double> output = sw.Decrypt(encypted);
            for(int i = 0; i < slotCount; i++)
            {
                Assert.IsTrue(Math.Abs(input[i] - output[i]) < 0.00001);
            }

            // Test the saving and loading of the keys
            String galBase64_1 = sw.getGaloisKeys();
            String galSingleStepBase64_1 = sw.getGaloisSingleStepKeys();
            String relinBase64_1 = sw.getRelinKeys();
            SEALWrapper swKeys = new SEALWrapper(slotCount, false, true);
            swKeys.LoadKeys(galBase64_1, galSingleStepBase64_1, relinBase64_1);
            String galBase64_2 = swKeys.getGaloisKeys();
            String galSingleStepBase64_2 = swKeys.getGaloisSingleStepKeys();
            String relinBase64_2 = swKeys.getRelinKeys();
            Assert.AreEqual(galBase64_1, galBase64_2);
            Assert.AreEqual(galSingleStepBase64_1, galSingleStepBase64_2);
            Assert.AreEqual(relinBase64_1, relinBase64_2);
        }

        [TestMethod]
        public void TestCipherMath()
        {
            int count = 10;
            int slotCount = 4096;
            double scale = 1.0;

            double Lat = 47.0;
            double Long = 122.0;
            double now = 1536782668;
            double startOfDay = now - 1736;

            int offset = 0;
            int yearofCentury = 3;
            offset += 100;
            int dayOfYear = 22 + offset;
            offset += 366;
            int dayOfWeek = 3 + offset;
            offset += 7;
            int weekOfYear = 2 + offset;

            // Create the Summary Mask
            List<double> summaryMask = new List<double>(slotCount);
            for (int i = 0; i < slotCount; i++)
            {
                summaryMask.Add(0);
            }
            summaryMask[yearofCentury] = 1;
            summaryMask[(slotCount / 2) + yearofCentury] = 1;
            summaryMask[dayOfWeek] = 1;
            summaryMask[(slotCount / 2) + dayOfWeek] = 1;
            summaryMask[weekOfYear] = 1;
            summaryMask[(slotCount / 2) + weekOfYear] = 1;
            summaryMask[dayOfYear] = 1;
            summaryMask[(slotCount / 2) + dayOfYear] = 1;

            // Create the Lats, Longs, and Timestamps
            List<double> lats = new List<double>(slotCount / 2);
            List<double> longs = new List<double>(slotCount / 2);
            List<double> timestamps = new List<double>(slotCount / 2);

            for (int i = 0; i < count; i++)
            {
                lats.Add(Lat);
                longs.Add(Long);

                Lat += 0.01;
                Long += 0.01;

                timestamps.Add((now - startOfDay) + (i * 2));
            }

            for (int i = count; i < slotCount / 2; i++)
            {
                lats.Add(0);
                longs.Add(0);
                timestamps.Add(0);
            }

            // Create the average gyroscope data
            List<double> gyro = new List<double>(slotCount / 2);
            for (int i = 0; i < 6; ++i)
                gyro.Add(1);

            // Calculate the Cartesian
            List<double> cx = new List<double>(slotCount / 2);
            List<double> cy = new List<double>(slotCount / 2);
            List<double> cz = new List<double>(slotCount / 2);

            double PI = 3.14159265359;

            for (int i = 0; i < count; i++)
            {
                cx.Add((double)(6371.0 * Math.Cos(lats[i] * PI / 180.0) * Math.Cos(longs[i] * PI / 180.0) * scale));
                cy.Add((double)(6371.0 * Math.Cos(lats[i] * PI / 180.0) * Math.Sin(longs[i] * PI / 180.0) * scale));
                cz.Add((double)(6371.0 * Math.Sin(lats[i] * PI / 180.0) * scale));
            }

            for (int i = count; i < slotCount / 2; i++)
            {
                cx.Add(0);
                cy.Add(0);
                cz.Add(0);
            }

            List<double> statsGold = new List<double>(slotCount);
            List<double> summaryGold = new List<double>(slotCount);

            double delta, totalDistance = 0, totalTime = timestamps[count - 1] - timestamps[0];
            for (int i = 0; i < count - 1; i++)
            {
                delta = (double)Math.Pow((cx[i + 1] - cx[i]), 2) + (double)Math.Pow((cy[i + 1] - cy[i]), 2) + (double)Math.Pow((cz[i + 1] - cz[i]), 2);
                statsGold.Add(delta);
                totalDistance += delta;
            }

            for (int i = count - 1; i < slotCount; i++)
            {
                statsGold.Add(0);
            }

            statsGold[(slotCount / 2) - 1] = totalTime;
            statsGold[(slotCount / 2) + yearofCentury] = totalDistance;
            statsGold[(slotCount / 2) + dayOfWeek] = totalDistance;
            statsGold[(slotCount / 2) + weekOfYear] = totalDistance;
            statsGold[(slotCount / 2) + dayOfYear] = totalDistance;

            for (int i = 0; i < slotCount; i++)
            {
                summaryGold.Add(0);
            }
            summaryGold[yearofCentury] = totalTime;
            summaryGold[dayOfWeek] = totalTime;
            summaryGold[weekOfYear] = totalTime;
            summaryGold[dayOfYear] = totalTime;
            summaryGold[(slotCount / 2) + yearofCentury] = totalDistance;
            summaryGold[(slotCount / 2) + dayOfWeek] = totalDistance;
            summaryGold[(slotCount / 2) + weekOfYear] = totalDistance;
            summaryGold[(slotCount / 2) + dayOfYear] = totalDistance;

            // Create the vectors for the cipher math
            List<double> firstList = new List<double>(slotCount);
            List<double> secondList = new List<double>(slotCount);
            for (int i = 0; i < count; i++)
            {
                firstList.Add(cx[i]);
                secondList.Add(cz[i]);
            }
            for (int i = count; i < slotCount / 2; i++)
            {
                firstList.Add(cx[count - 1]);
                secondList.Add(cz[count - 1]);
            }
            for (int i = 0; i < count; i++)
            {
                firstList.Add(cy[i]);
                secondList.Add(timestamps[i]);
            }
            for (int i = count; i < slotCount / 2; i++)
            {
                firstList.Add(cy[count - 1]);
                secondList.Add(timestamps[count - 1]);
            }

            // Create the Ciphertexts
            SEALWrapper sw = new SEALWrapper(slotCount, true, true);
            String firstbase64 = sw.Encrypt(firstList);
            String secondbase64 = sw.Encrypt(secondList);
            String summaryMaskBase64 = sw.Encrypt(summaryMask);
            String gyroBase64 = sw.Encrypt(gyro);
            String galKeysString = sw.getGaloisKeys();
            String galKeysSingleStepString = sw.getGaloisSingleStepKeys();
            String relinKeysString = sw.getRelinKeys();

            // Perform the add with a new SEALWrapper to simulate the call from the server
            SEALWrapper serverSW = new SEALWrapper(slotCount);
            serverSW.LoadKeys(galKeysString, galKeysSingleStepString, relinKeysString);
            Assert.IsTrue(serverSW.ComputeStatsCiphers(firstbase64, secondbase64, summaryMaskBase64, gyroBase64), "Server Call");
            String statsBase64 = serverSW.getStats();
            String summaryBase64 = serverSW.getSummary();
            String mlBase64 = serverSW.getMlResults();

            // Get the List result with the first SEALWrapper that has the keys still
            List<double> statsList = sw.Decrypt(statsBase64);
            List<double> summaryList = sw.Decrypt(summaryBase64);
            List<double> mlList = sw.Decrypt(mlBase64);

            // Reset the near zero to zero
            for(int i = 0; i < slotCount; i++)
            {
                if (Math.Abs(statsList[i]) < 0.00001) statsList[i] = 0.0;
                if (Math.Abs(summaryList[i]) < 0.00001) summaryList[i] = 0.0;
            }

            // Test that the results match
            Assert.AreEqual(statsList.Count, statsGold.Count, "Stats Size");
            Assert.AreEqual(summaryList.Count, summaryGold.Count, "Summary Size");
            for (int i = 0; i < slotCount; i++)
            {
                double diffStatsList = Math.Abs(statsList[i] - statsGold[i]);
                double diffSummaryList = Math.Abs(summaryList[i] - summaryGold[i]);
                Assert.IsTrue(diffStatsList < 0.1, "Stats Index Miss Match: " + i);
                Assert.IsTrue(diffSummaryList < 0.1, "Summary Index Miss Match: " + i);
            }

            // Test gyro output
            double[] tensorB = new double[] {
                0.41964226961135864,
                6.6290693283081055,
                -2.404352903366089,
                -0.024301817640662193,
                -0.17596858739852905,
                0.14117737114429474
            };
            double expectedMlResult = 0;
            for (int i = 0; i < tensorB.Length; ++i)
                expectedMlResult += tensorB[i];
            Assert.IsTrue(Math.Abs(expectedMlResult - mlList[0]) < 0.1);
        }

        [TestMethod]
        public void UpdateSummaryPatch()
        {
            // Test is invalid now
            int slotCount = 4096;

            // Create the Test Data
            Random rand = new Random();
            List<double> list1 = new List<double>(slotCount);
            List<double> list2 = new List<double>(slotCount);
            List<double> resultGold = new List<double>(slotCount);
            for (int i = 0; i < slotCount; i++)
            {
                list1.Add((double)(rand.Next(0, 500)) / 10.0);
                list2.Add((double)(rand.Next(0, 500)) / 10.0);
                resultGold.Add(list1[i] + list2[i]);
            }

            // Create the Ciphertexts
            SEALWrapper sw = new SEALWrapper(slotCount, true, false);
            String list1base64 = sw.Encrypt(list1);
            String list2base64 = sw.Encrypt(list2);

            // Perform the add with a new SEALWrapper to simulate the call from the server
            SEALWrapper serverSW = new SEALWrapper(slotCount);
            Assert.IsTrue(serverSW.AddCiphers(list1base64, list2base64));
            String resultBase64 = serverSW.getAdded();

            // Get the List result with the first SEALWrapper that has the keys still
            List<double> resultList = sw.Decrypt(resultBase64);

            // Test that the results match
            Assert.AreEqual(resultList.Count, resultGold.Count);
            for(int i = 0; i < slotCount; i++)
            {
                Assert.IsTrue(Math.Abs(resultList[i] - resultGold[i]) < 0.00001);
            }
        }
    }
}
