package com.surajgharat.practice.grpc.server

import scala.concurrent.ExecutionContext
import scala.util.Try

object Main1 {
  def main(args:Array[String]):Unit={
    startDemoServer(args)
  }

  def startSumServer(args:Array[String]):Unit={
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

  def startDemoServer(args:Array[String]):Unit={
    val server1 = new DemoServer(ExecutionContext.global)

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
