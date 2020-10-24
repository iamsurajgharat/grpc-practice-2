package com.surajgharat.practice.grpc.server

import java.util.concurrent.TimeUnit
import java.util.logging.Logger

import com.surajgharat.practice.grpc.service.demoService.{DemoNumber, DemoServiceGrpc, Empty, PoisonPill}
import com.surajgharat.practice.grpc.service.sumservice.SumInput
import io.grpc.stub.StreamObserver
import io.grpc.{Server, ServerBuilder}

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}

object DemoServer {
  private val logger = Logger.getLogger(classOf[DemoServer].getName)
}

class DemoServer(ec: ExecutionContext){
  private[this] var server: Server = null

  def start(port: Int): Unit = {
    server = ServerBuilder
      .forPort(port)
      .addService(DemoServiceGrpc.bindService(new DemoServerImpl, ec))
      .build
      .start

    DemoServer.logger.info("Server started, listening on : " + port)

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

  private class DemoServerImpl extends DemoServiceGrpc.DemoService{
    import DemoServer._
    override def sum(request: SumInput): Future[DemoNumber] = {
      logger.info(s"[Simple non streaming gRPC call] Received input ${request.n1} and ${request.n2}")
      Future.successful(DemoNumber(request.n1+request.n2))
    }

    /*
      Server-side streaming
     */
    override def getAllPrimeNumbersWithinGivenRange(request: DemoNumber, responseObserver: StreamObserver[DemoNumber]): Unit = {
      logger.info(s"[Server streaming gRPC call] Received input ${request.value}")

      // Iterate through all numbers in the given range
      for(i <- 2 to request.value){

        logger.info(s"[Server streaming gRPC call] Checking if $i is prime number")
        //TimeUnit.SECONDS.sleep(3);

        // if the number is prime then send to client
        if(isPrime(i)) {
          responseObserver.onNext(DemoNumber(i))
          DemoServer.logger.info(s"[Server streaming gRPC call] Sent $i as it is a prime number")
        }
      }

      // send stream completion signal to client
      responseObserver.onCompleted()

      logger.info("[Server streaming gRPC call] Done all processing for input :"+request.value)
    }

    /*
      Client-side streaming
     */
    override def sumMultiple(responseObserver: StreamObserver[DemoNumber]): StreamObserver[DemoNumber] = {
      logger.info(s"[Client streaming gRPC call] Received init/invoke")
      getClientStreamObserverForSum(responseObserver)
    }

    /*
      Bidirectional streaming
     */
    override def getCountsToMakeHundred(responseObserver: StreamObserver[DemoNumber]): StreamObserver[DemoNumber] = {
      logger.info(s"[Bi streaming gRPC call] Received init/invoke")
      getBiStreamObserverForCountForHundred(responseObserver)
    }

    /*
      To gracefully shutdown the server
     */
    override def terminate(request: PoisonPill): Future[Empty] = {
      logger.info(s"[terminate] Received terminate request. Bye!")
      stop()
      Future.successful(Empty())
    }

    private def getClientStreamObserverForSum(responseObserver: StreamObserver[DemoNumber]): StreamObserver[DemoNumber] = new StreamObserver[DemoNumber] {
      private var sum = 0
      override def onNext(value: DemoNumber): Unit = {
        DemoServer.logger.info("[Client streaming gRPC call] Received stream element :"+value.value)
        TimeUnit.SECONDS.sleep(1)
        sum = sum + value.value
      }

      override def onError(t: Throwable): Unit = ???

      override def onCompleted(): Unit = {
        DemoServer.logger.info("[Client streaming gRPC call] input stream is complete, and so sending response now : "+ sum)
        responseObserver.onNext(DemoNumber(sum))
        responseObserver.onCompleted()
      }
    }

    private def getBiStreamObserverForCountForHundred(responseObserver: StreamObserver[DemoNumber]): StreamObserver[DemoNumber] = new StreamObserver[DemoNumber] {
      var sum = 0
      var count = 0
      override def onNext(value: DemoNumber): Unit = {
        sum = sum + value.value
        count = count + 1
        DemoServer.logger.info(s"[Bi streaming gRPC call] Received ${value.value}. Current sum is $sum and count is $count")
        if(sum >= 100){
          responseObserver.onNext(DemoNumber(count))
          count = 0
          sum = 0
        }
      }

      override def onError(t: Throwable): Unit = ???

      override def onCompleted(): Unit = {
        DemoServer.logger.info(s"[Bi streaming gRPC call] client input is complete and so also completing server output")
        responseObserver.onCompleted()
      }
    }

    private def isPrime(n:Int):Boolean = !canDivideByAnyFromRange(n,2, n/2)

    @tailrec
    private def canDivideByAnyFromRange(n:Int, start:Int, end:Int):Boolean =
      if( start > end ) false
      else if(n % start == 0) true
      else canDivideByAnyFromRange(n, start+1, end)
  }
}
