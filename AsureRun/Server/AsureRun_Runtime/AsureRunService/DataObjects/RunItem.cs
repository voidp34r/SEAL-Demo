using Microsoft.Azure.Mobile.Server;
using System;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace AsureRunService.DataObjects
{
    public class RunItem : EntityData
    {
        public int RunNumber { get; set; }

        /**
         * This cipher stores the XY coordinates per sec
         **/
        public string Cipher1 { get; set; }

        /**
         * This cipher stores the Z coordinates and T for time per sec 
         **/
        public string Cipher2 { get; set; }

        /**
         * This cipher holds Elevation and Pace per sec **/
        public string CipherEP { get; set; }

        /**
         * this cipher is used to hold a thumbnail of the map */
        public string CipherThumbnail { get; set; }

        public string Stats { get; set; }

        public string Summary { get; set; }

        public string CipherGyro { get; set; }

        public string KeyId { get; set; }

        public string UserId { get; set; }

        /* The following two objects are being unmapped from the Table because they 
           are considered to be leaking information that should not be know.  In 
           this case it leaks when a run has completed. */

        [NotMapped]
        public new DateTimeOffset? CreatedAt { get; set; }

        [NotMapped]
        public new DateTimeOffset? UpdatedAt { get; set; }
    }
}