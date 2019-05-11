using AsureRunService.DataObjects;
using AsureRunService.Extensions;
using AsureRunService.Models;
using AsureRunService.Utilities;
using Microsoft.Azure.Mobile.Server;
using System.Linq;
using System.Net;
using System.Security.Claims;
using System.Threading.Tasks;
using System.Web.Http;
using System.Web.Http.Controllers;
using System.Web.Http.OData;

namespace AsureRunService.Controllers
{
    [Authorize]
    public class KeyItemController : TableController<KeyItem>
    {
        protected override void Initialize(HttpControllerContext controllerContext)
        {
            base.Initialize(controllerContext);
            AsureRunContext context = new AsureRunContext();
            DomainManager = new EntityDomainManager<KeyItem>(context, Request);
        }

        public string UserId => ((ClaimsPrincipal)User).FindFirst(ClaimTypes.NameIdentifier).Value;

        public void ValidateKey(string id)
        {
            var result = Lookup(id).Queryable.PerUserFilter(UserId).FirstOrDefault<KeyItem>();
            if (result == null)
            {
                throw new HttpResponseException(HttpStatusCode.NotFound);
            }
        }

        // GET tables/KeyItem
        public IQueryable<KeyItem> GetAllKeyItem()
        {
            return Query().Where(item => item.UserId.Equals(UserId));
        }

        // GET tables/KeyItem/48D68C86-6EA6-4C25-AA33-223FC9A27959
        public SingleResult<KeyItem> GetKeyItem(string id)
        {
            return new SingleResult<KeyItem>(Lookup(id).Queryable.PerUserFilter(UserId));
        }

        // PATCH tables/KeyItem/48D68C86-6EA6-4C25-AA33-223FC9A27959
        public Task<KeyItem> PatchKeyItem(string id, Delta<KeyItem> patch)
        {
            ValidateKey(id);
            return UpdateAsync(id, patch);
        }

        // POST tables/KeyItem
        public async Task<IHttpActionResult> PostKeyItem(KeyItem item)
        {
            item.UserId = UserId;
            KeyItem current = await InsertAsync(item);
            return CreatedAtRoute("Tables", new { id = current.Id }, current);
        }

        // DELETE tables/KeyItem/48D68C86-6EA6-4C25-AA33-223FC9A27959
        public Task DeleteKeyItem(string id)
        {
            ValidateKey(id);
            KeyUtilities.DeleteLocalCache(id);
            KeyUtilities.DeleteBlobContainer(id);
            return DeleteAsync(id);
        }
    }
}
