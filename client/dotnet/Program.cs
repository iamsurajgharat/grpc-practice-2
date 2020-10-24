using System;
using Grpc.Net.Client;
using Com.Surajgharat.Practice.Grpc.Service;
using System.Threading.Tasks;
using System.Net.Http;
using Grpc.Core;
using System.Linq;
using Microsoft.Extensions.Logging;

namespace dotnet
{
    class Program
    {
        static ILogger logger;
        static async Task Main(string[] args)
        {
            // input
            int port = GetNumFromArgs(args, 0, 50051);
            int n1 = GetNumFromArgs(args, 1, 10);
            int n2 = GetNumFromArgs(args, 2, 20);
            int n3 = GetNumFromArgs(args, 3, 20);
            int n4 = GetNumFromArgs(args, 4, 15);
            int n5 = GetNumFromArgs(args, 4, 20);

            logger = GetLogger<Program>();

            // Enable non-ssl http2 support
            AppContext.SetSwitch("System.Net.Http.SocketsHttpHandler.Http2UnencryptedSupport", true);


            //create channel
            var grpcChannelConifg = new GrpcChannelOptions();
            grpcChannelConifg.HttpHandler = new MyHttp2MessageHandler();


            // channel
            var channel = GrpcChannel.ForAddress("http://localhost:" + port, grpcChannelConifg);

            // client
            var client = new DemoService.DemoServiceClient(channel);

            // grpc calls
            await MakeNonStreamGrpcCallAsync(client, n1, n2);
            await MakeServerStreamGrpcCallAsync(client, n3);
            await MakeClientStreamGrpcCallAsync(client, n4);
            await MakeBiStreamGrpcCallAsync(client, n5);
            await MakeTerminateCallAsync(client);
        }

        private static async Task MakeNonStreamGrpcCallAsync(DemoService.DemoServiceClient client, int n1, int n2)
        {
            LogInfo("[Simple non streaming gRPC call] about to start");
            DemoNumber sumOutput = await client.SumAsync(new SumInput { N1 = n1, N2 = n2 });
            LogInfo("[Simple non streaming gRPC call] Done : " + sumOutput.Value);
        }

        private static async Task MakeTerminateCallAsync(DemoService.DemoServiceClient client)
        {
            LogInfo("[Terminate] about to start");
            await client.TerminateAsync(new PoisonPill());
            LogInfo("[Ternimate] Done");
        }

        private static async Task MakeServerStreamGrpcCallAsync(DemoService.DemoServiceClient client, int n)
        {
            LogInfo("[Server streaming gRPC call] about to start");
            using (AsyncServerStreamingCall<DemoNumber> call = client.GetAllPrimeNumbersWithinGivenRange(new DemoNumber { Value = n }))
            {
                LogInfo("[Server streaming gRPC call] starting to read server stream");
                while (await call.ResponseStream.MoveNext())
                {
                    LogInfo("[Server streaming gRPC call] received server stream element :" + call.ResponseStream.Current.Value);
                    //await Task.Delay(5000);
                }

                LogInfo("[Server streaming gRPC call] done with reading server stream");
            }

        }

        private static async Task MakeClientStreamGrpcCallAsync(DemoService.DemoServiceClient client, int n)
        {
            LogInfo("[Client streaming gRPC call] about to start");
            using var call = client.SumMultiple();

            foreach (var i in Enumerable.Range(1, n))
            {
                LogInfo("[Client streaming gRPC call] sending stream element :" + i);
                //await Task.Delay(5000);
                await call.RequestStream.WriteAsync(new DemoNumber { Value = i });
            }

            await call.RequestStream.CompleteAsync();

            LogInfo("[Client streaming gRPC call] done with sending all stream elements");

            var response = await call;

            LogInfo("[Client streaming gRPC call] received server response : " + response.Value);
        }

        private static async Task MakeBiStreamGrpcCallAsync(DemoService.DemoServiceClient client, int n)
        {
            LogInfo("[Bi-streaming gRPC call] about to start");

            using var call = client.GetCountsToMakeHundred();

            LogInfo("[Bi-streaming gRPC call] starting background task to receive messages");
            var readTask = Task.Run(async () =>
            {
                while (await call.ResponseStream.MoveNext())
                {
                    LogInfo("[Bi-streaming gRPC call] received from server :" + call.ResponseStream.Current.Value);
                    //await Task.Delay(5000);
                }

                LogInfo("[Bi-streaming gRPC call] received done from server ");
            });

            Random rnd = new Random();

            LogInfo("[Bi-streaming gRPC call] starting to send messages");
            foreach (var i in Enumerable.Range(1, n))
            {
                int a1 = rnd.Next(1, 100);

                LogInfo("[Bi-streaming gRPC call] sending :" + i);
                await Task.Delay(5000);
                await call.RequestStream.WriteAsync(new DemoNumber { Value = a1 });
            }

            LogInfo("[Bi-streaming gRPC call] done with sending");
            await call.RequestStream.CompleteAsync();
            await readTask;
        }

        private static int GetNumFromArgs(string[] args, int index, int resultIfNotFound)
        {
            return args == null || args.Length <= index ? resultIfNotFound : (Int32.TryParse(args[index], out var result) ? result : resultIfNotFound);
        }

        private static void LogInfo(string info)
        {
            logger.LogInformation(info);
        }

        private static ILogger<T> GetLogger<T>()
        {
            using var loggerFactory = LoggerFactory.Create(builder =>
            {
                builder
                    .AddFilter("Microsoft", LogLevel.Warning)
                    .AddFilter("System", LogLevel.Warning)
                    .AddFilter("LoggingConsoleApp.Program", LogLevel.Debug)
                    .AddConsole();
            });

            return loggerFactory.CreateLogger<T>();
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
