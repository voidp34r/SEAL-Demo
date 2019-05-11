using System.Linq;
using System.Threading.Tasks;
using System.Web.Http;
using System.Web.Http.Controllers;
using System.Web.Http.OData;
using Microsoft.Azure.Mobile.Server;
using AsureRunService.Models;
using AsureRunService.DataObjects;
using System.ComponentModel.DataAnnotations.Schema;
using System;

namespace AsureRunService.DataObjects
{
    [Authorize]
    public class SummaryItem : EntityData
    {        
        public string Summary { get; set; }
        
        public string KeyId { get; set; }

        public string UserId { get; set; }

        /* The following two objects are being unmapped from the Table because they 
           are considered to be leaking information that should not be know.  In 
           this case it leaks when the first and last runs where completed. */

        [NotMapped]
        public new DateTimeOffset? CreatedAt { get; set; }

        [NotMapped]
        public new DateTimeOffset? UpdatedAt { get; set; }
    }
}