package com.surajgharat.practice.grpc.server

import java.util.concurrent.TimeUnit
import java.util.logging.Logger

import com.surajgharat.practice.grpc.service.sumservice.{SumInput, SumNumber, SumOutput}

import scala.concurrent.{ExecutionContext, Future}
import io.grpc.{Server, ServerBuilder}
import com.surajgharat.practice.grpc.service.sumservice.SumServiceGrpc
import io.grpc.stub.StreamObserver

import scala.util.Try;

object SumServer {
  private val logger = Logger.getLogger(classOf[SumServer].getName)
}

class SumServer(ec: ExecutionContext) {
  private[this] var server: Server = null

  def start(port: Int): Unit = {
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

  def stop(): Unit = {
    if (server != null) server.shutdown()
  }

  def blockUntilShutdown(): Unit = {
    if (server != null) server.awaitTermination()
  }

  private class SumServiceImpl extends SumServiceGrpc.SumService {
    // service implementation
    override def sum(request: SumInput): Future[SumOutput] = {
      val result = Future.successful(SumOutput(request.n1 + request.n2))
      //stop();
      result
    }

    override def getSumUniqueComponents(target:SumNumber, responseObserver: StreamObserver[SumNumber]):Unit = {
      SumServer.logger.info("Starting to serve server stream grpc call. Input :"+target.value)

      for(i <- 1 to 10){
        SumServer.logger.info("Sending next component from server streaming :"+i)
        TimeUnit.SECONDS.sleep(3);
        responseObserver.onNext(SumNumber(i))
      }
      responseObserver.onCompleted()

      SumServer.logger.info("Done with serving server stream grpc call")
      stop()
    }
  }
}
