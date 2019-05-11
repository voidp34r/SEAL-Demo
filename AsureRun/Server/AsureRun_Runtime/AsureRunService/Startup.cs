using Microsoft.Owin;
using Owin;

[assembly: OwinStartup(typeof(AsureRunService.Startup))]

namespace AsureRunService
{
    public partial class Startup
    {
        public void Configuration(IAppBuilder app)
        {
            ConfigureMobileApp(app);
        }
    }
}