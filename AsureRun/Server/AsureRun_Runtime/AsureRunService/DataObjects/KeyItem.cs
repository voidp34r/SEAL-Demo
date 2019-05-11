using Microsoft.Azure.Mobile.Server;
using System;
using System.ComponentModel.DataAnnotations.Schema;

namespace AsureRunService.DataObjects
{
    public class KeyItem : EntityData
    {
        public int SlotCount { get; set; }

        public int InitialScale { get; set; }

        public string UserId { get; set; }

        public string UserEmail { get; set; }
        /* The following two objects are being unmapped from the Table because they 
           are considered to be leaking information that should not be know.  In 
           this case it leaks when the client application was first ran. */

        [NotMapped]
        public new DateTimeOffset? CreatedAt { get; set; }

        [NotMapped]
        public new DateTimeOffset? UpdatedAt { get; set; }
    }
}