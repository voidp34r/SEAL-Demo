using System.Linq;
using System.Threading.Tasks;
using System.Web.Http;
using System.Web.Http.Controllers;
using System.Web.Http.OData;
using Microsoft.Azure.Mobile.Server;
using AsureRunService.DataObjects;
using AsureRunService.Models;
using AsureRunService.Extensions;
using System.Security.Claims;
using System.Net;

namespace AsureRunService.Controllers
{
    public class SummaryItemController : TableController<SummaryItem>
    {
        protected override void Initialize(HttpControllerContext controllerContext)
        {
            base.Initialize(controllerContext);
            AsureRunContext context = new AsureRunContext();
            DomainManager = new EntityDomainManager<SummaryItem>(context, Request);
        }

        public string UserId => ((ClaimsPrincipal)User).FindFirst(ClaimTypes.NameIdentifier).Value;

        public SummaryItem ValidateKey(string id)
        {
            var result = Lookup(id).Queryable.PerUserFilter(UserId).FirstOrDefault<SummaryItem>();
            if (result == null)
            {
                throw new HttpResponseException(HttpStatusCode.NotFound);
            }
            return result;
        }

        // GET tables/SummaryItem
        public IQueryable<SummaryItem> GetAllSummaryItem()
        {
            return Query().Where(item => item.UserId.Equals(UserId));
        }

        // GET tables/SummaryItem/48D68C86-6EA6-4C25-AA33-223FC9A27959
        public SingleResult<SummaryItem> GetSummaryItem(string id)
        {
            return new SingleResult<SummaryItem>(Lookup(id).Queryable.PerUserFilter(UserId));
        }

        // PATCH tables/SummaryItem/48D68C86-6EA6-4C25-AA33-223FC9A27959
        public Task<SummaryItem> PatchSummaryItem(string id, Delta<SummaryItem> patch)
        {
            SummaryItem oldSummaryItem = ValidateKey(id);

            // Only do the following if the summary value is being updated
            if (patch.GetChangedPropertyNames().Contains("Summary"))
            {
                string newSummaryCipher = patch.GetEntity().AddSummary(oldSummaryItem);
                patch.TrySetPropertyValue("Summary", newSummaryCipher);
            }

            return UpdateAsync(id, patch);
        }

        // POST tables/SummaryItem
        public async Task<IHttpActionResult> PostSummaryItem(SummaryItem item)
        {
            item.UserId = UserId;
            SummaryItem current = await InsertAsync(item);
            return CreatedAtRoute("Tables", new { id = current.Id }, current);
        }

        // DELETE tables/SummaryItem/48D68C86-6EA6-4C25-AA33-223FC9A27959
        public Task DeleteSummaryItem(string id)
        {
            ValidateKey(id);
            return DeleteAsync(id);
        }
    }
}
