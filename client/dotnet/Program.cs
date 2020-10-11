using System;
using Grpc.Net.Client;
using Com.Surajgharat.Practice.Grpc.Service;
using System.Threading.Tasks;
using System.Net.Http;

namespace dotnet
{
    class Program
    {
        static async Task Main(string[] args)
        {
            // input
            int port = GetNumFromArgs(args, 0, 50051);
            int n1 = GetNumFromArgs(args, 1, 10);
            int n2 = GetNumFromArgs(args, 2, 20);

            // Enable non-ssl http2 support
            AppContext.SetSwitch("System.Net.Http.SocketsHttpHandler.Http2UnencryptedSupport", true);


            //create channel
            var grpcChannelConifg = new GrpcChannelOptions();
            grpcChannelConifg.HttpHandler = new MyHttp2MessageHandler();


            // channel
            var channel = GrpcChannel.ForAddress("http://localhost:" + port, grpcChannelConifg);

            // client
            var client = new SumService.SumServiceClient(channel);

            // grpc call
            SumOutput sumOutput = await client.SumAsync(new SumInput { N1 = n1, N2 = n2 });

            Console.WriteLine("Result : " + sumOutput.Result);
        }

        private static int GetNumFromArgs(string[] args, int index, int resultIfNotFound)
        {
            return args == null || args.Length <= index ? resultIfNotFound : (Int32.TryParse(args[index], out var result) ? result : resultIfNotFound);
        }
    }



    class MyHttp2MessageHandler : DelegatingHandler
    {
        public MyHttp2MessageHandler()
        {
            InnerHandler = new HttpClientHandler();
        }
        protected override Task<HttpResponseMessage> SendAsync(
            HttpRequestMessage request, System.Threading.CancellationToken cancellationToken)
        {
            request.Version = new Version(2, 0);
            return base.SendAsync(request, cancellationToken);
        }
    }
}
