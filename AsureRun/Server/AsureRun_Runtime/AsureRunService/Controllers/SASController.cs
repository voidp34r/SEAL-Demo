using System.Web.Http;
using Microsoft.Azure.Mobile.Server.Config;
using AsureRunService.Utilities;

namespace AsureRunService.Controllers
{
    /// <summary>
    /// Controller used to access Azure Storage from the mobile App
    /// </summary>
    [MobileAppController]
    public class SASController : ApiController
    {
        /// <summary>
        /// Get SAS token for a given container name
        /// </summary>
        public string Post()
        {
            // This will come out as api/SAS/<string client sends which is a directory server must create>
            string uri = Request.RequestUri.AbsolutePath;
            string[] uriSplit = uri.Split('/');
            return KeyUtilities.GetAccountSASToken(uriSplit[uriSplit.Length - 1]);
        }
    }
}
