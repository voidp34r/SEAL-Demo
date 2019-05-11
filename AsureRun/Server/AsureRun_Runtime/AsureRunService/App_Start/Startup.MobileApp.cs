using System;
using System.Collections.Generic;
using System.Configuration;
using System.Data.Entity;
using System.Web.Http;
using Microsoft.Azure.Mobile.Server;
using Microsoft.Azure.Mobile.Server.Authentication;
using Microsoft.Azure.Mobile.Server.Config;
using AsureRunService.DataObjects;
using AsureRunService.Models;
using Owin;

namespace AsureRunService
{
    public partial class Startup
    {
        public static void ConfigureMobileApp(IAppBuilder app)
        {
            HttpConfiguration config = new HttpConfiguration();

            //For more information on Web API tracing, see http://go.microsoft.com/fwlink/?LinkId=620686 
            config.EnableSystemDiagnosticsTracing();

            new MobileAppConfiguration()
                .UseDefaultConfiguration()
                .ApplyTo(config);

            // Use Entity Framework Code First to create database tables based on your DbContext
            Database.SetInitializer(new AsureRunInitializer());
            // To prevent Entity Framework from modifying your database schema, use a null database initializer
            // Database.SetInitializer<AsureRunContext>(null);

            MobileAppSettingsDictionary settings = config.GetMobileAppSettingsProvider().GetMobileAppSettings();

            if (string.IsNullOrEmpty(settings.HostName))
            {
                // This middleware is intended to be used locally for debugging. By default, HostName will
                // only have a value when running in an App Service application.
                app.UseAppServiceAuthentication(new AppServiceAuthenticationOptions
                {
                    SigningKey = ConfigurationManager.AppSettings["SigningKey"],
                    ValidAudiences = new[] { ConfigurationManager.AppSettings["ValidAudience"] },
                    ValidIssuers = new[] { ConfigurationManager.AppSettings["ValidIssuer"] },
                    TokenHandler = config.GetAppServiceTokenHandler()
                });
            }
            app.UseWebApi(config);
        }
    }

    public class AsureRunInitializer : DropCreateDatabaseIfModelChanges<AsureRunContext>
    {
        protected override void Seed(AsureRunContext context)
        {
            // TODO: Remove the following block of code when the tables are 100% set...
            List<KeyItem> keyItems = new List<KeyItem>
            {
                new KeyItem { Id = Guid.NewGuid().ToString(), UserId = "u1", SlotCount = 4096, InitialScale = 60 }
            };

            foreach (KeyItem keyItem in keyItems)
            {
                context.Set<KeyItem>().Add(keyItem);
            }

            List<SummaryItem> summaryItems = new List<SummaryItem>
            {
                new SummaryItem { Id = Guid.NewGuid().ToString(), UserId = "u1", KeyId = "k1", Summary = "s1" }
            };

            foreach (SummaryItem summaryItem in summaryItems)
            {
                context.Set<SummaryItem>().Add(summaryItem);
            }

            List<RunItem> runItems = new List<RunItem>
            {
                new RunItem { Id = Guid.NewGuid().ToString(), RunNumber = 1, UserId = "u1", KeyId = "k1", Cipher1 = "c1", Cipher2 = "c1", CipherEP = "", CipherThumbnail = "", Stats = "s1", Summary = "m1" }
            };

            foreach (RunItem runItem in runItems)
            {
                context.Set<RunItem>().Add(runItem);
            }

            base.Seed(context);
        }
    }
}

