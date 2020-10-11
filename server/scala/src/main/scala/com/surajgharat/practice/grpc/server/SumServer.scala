package com.surajgharat.practice.grpc.server

import java.util.logging.Logger
import com.surajgharat.practice.grpc.service.sumservice.{SumInput, SumOutput}
import scala.concurrent.{ExecutionContext, Future}
import io.grpc.{Server, ServerBuilder}
import com.surajgharat.practice.grpc.service.sumservice.SumServiceGrpc
import scala.util.Try;

object SumServer {
  private val logger = Logger.getLogger(classOf[SumServer].getName)

  def main(args: Array[String]): Unit = {
    val server1 = new SumServer(ExecutionContext.global)

    // Get port from args if provided
    val port =
      if (args == null || args.length < 1 || Try(args(0).toInt).isFailure) 50051
      else args(0).toInt

    // start the server
    server1.start(port)

    // wait for requests
    server1.blockUntilShutdown();
  }
}

class SumServer(ec: ExecutionContext) {
  private[this] var server: Server = null

  private def start(port: Int): Unit = {
    server = ServerBuilder
      .forPort(port)
      .addService(SumServiceGrpc.bindService(new SumServiceImpl, ec))
      .build
      .start

    SumServer.logger.info("Server started, listening on : " + port)

    sys.addShutdownHook {
      System.err.println(
        "*** shutting down gRPC server since JVM is shutting down"
      )
      stop()
      System.err.println("*** server shut down")
    }
  }

  private def stop(): Unit = {
    if (server != null) server.shutdown()
  }

  private def blockUntilShutdown(): Unit = {
    if (server != null) server.awaitTermination()
  }

  private class SumServiceImpl extends SumServiceGrpc.SumService {
    // service implementation
    override def sum(request: SumInput): Future[SumOutput] = {
      val result = Future.successful(SumOutput(request.n1 + request.n2))
      stop();
      result
    }
  }
}
