using AsureRunService.DataObjects;
using AsureRunService.Utilities;
using System.Linq;
using System.IO;
using System;
using System.Collections.Generic;
using Microsoft.Research;
using System.Diagnostics;

namespace AsureRunService.Extensions
{
    public static class RunItemExtensions
    {
        public static IQueryable<RunItem> PerUserFilter(this IQueryable<RunItem> query, string userId)
        {
            return query.Where(item => item.UserId.Equals(userId));
        }

        public static RunItem ComputeRunStats(this RunItem runItem)
        {
            try
            {
                Trace.WriteLine("Start ComputeRunStats : RunItem " + runItem.KeyId);
                // Load the base64 keys 
                String galKeyString = "", galSingleStepKeyString = "", relinKeyString = "";
                KeyUtilities.GetKeys(runItem.KeyId, ref galKeyString, ref galSingleStepKeyString, ref relinKeyString);

                // Calculate the cipher results
                SEALWrapper sw = new SEALWrapper(4096);
                sw.LoadKeys(galKeyString, galSingleStepKeyString, relinKeyString);
                sw.ComputeStatsCiphers(runItem.Cipher1, runItem.Cipher2, runItem.Summary, runItem.CipherGyro);
                runItem.Stats = sw.getStats();
                runItem.Summary = sw.getSummary();
                runItem.CipherGyro = sw.getMlResults();

                Trace.WriteLine("End ComputeRunStats : RunItem " + runItem.KeyId);

            }
            catch (Exception e)
            {
                Trace.TraceError("{%s} occurred while computing stats {RunItem : %d}", runItem.KeyId, e);
            }

            return runItem;
        }
    }

    public static class KeyItemExtensions
    {
        public static IQueryable<KeyItem> PerUserFilter(this IQueryable<KeyItem> query, string userId)
        {
            return query.Where(item => item.UserId.Equals(userId));
        }
    }

    public static class SummaryItemExtensions
    {
        public static IQueryable<SummaryItem> PerUserFilter(this IQueryable<SummaryItem> query, string userId)
        {
            return query.Where(item => item.UserId.Equals(userId));
        }
        
        // Adds the two summary ciphers together
        public static string AddSummary(this SummaryItem summaryItem1, SummaryItem summaryItem2)
        {
            String resultBase64 = "";
            try
            {
                Trace.WriteLine("Start AddSummary " );

                SEALWrapper sw = new SEALWrapper(4096);
                sw.AddCiphers(summaryItem1.Summary, summaryItem2.Summary);
                resultBase64 = sw.getAdded();
            }
            catch(Exception e)
            {
                System.Diagnostics.Trace.TraceError("{%s} occurred while Adding Summary ", e);
            }

            Trace.WriteLine("End AddSummary ");

            // Convert to base64
            return resultBase64;
        }
    }
}