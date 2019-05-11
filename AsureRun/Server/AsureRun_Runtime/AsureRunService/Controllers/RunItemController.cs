using System.Linq;
using System.Threading.Tasks;
using System.Web.Http;
using System.Web.Http.Controllers;
using System.Web.Http.OData;
using Microsoft.Azure.Mobile.Server;
using AsureRunService.Models;
using AsureRunService.DataObjects;
using AsureRunService.Extensions;
using System.Security.Claims;
using System.Net;
using System.Collections.Generic;

namespace AsureRunService.Controllers
{
    [Authorize]
    public class RunItemController : TableController<RunItem>
    {
        protected override void Initialize(HttpControllerContext controllerContext)
        {
            base.Initialize(controllerContext);
            AsureRunContext context = new AsureRunContext();
            DomainManager = new EntityDomainManager<RunItem>(context, Request);
        }

        public string UserId => ((ClaimsPrincipal)User).FindFirst(ClaimTypes.NameIdentifier).Value;

        public void ValidateKey(string id)
        {
            var result = Lookup(id).Queryable.PerUserFilter(UserId).FirstOrDefault<RunItem>();
            if (result == null)
            {
                throw new HttpResponseException(HttpStatusCode.NotFound);
            }
        }

        // GET tables/RunItem
        public IQueryable<RunItem> GetAllRunItem()
        {
            return Query().Where(item => item.UserId.Equals(UserId));
        }

        // GET tables/RunItem/48D68C86-6EA6-4C25-AA33-223FC9A27959
        public SingleResult<RunItem> GetRunItem(string id)
        {
            return new SingleResult<RunItem>(Lookup(id).Queryable.PerUserFilter(UserId));
        }

        // PATCH tables/RunItem/48D68C86-6EA6-4C25-AA33-223FC9A27959
        public Task<RunItem> PatchRunItem(string id, Delta<RunItem> patch)
        {
            ValidateKey(id);
            return UpdateAsync(id, patch);
        }

        // POST tables/RunItem
        public async Task<IHttpActionResult> PostRunItem(RunItem item)
        {
            item.UserId = UserId;
            item = item.ComputeRunStats();

            RunItem current = await InsertAsync(item);
            return CreatedAtRoute("Tables", new { id = current.Id }, current);
        }

        // DELETE tables/RunItem/48D68C86-6EA6-4C25-AA33-223FC9A27959
        public Task DeleteRunItem(string id)
        {
            ValidateKey(id);
            return DeleteAsync(id);
        }
    }
}
