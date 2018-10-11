package com.example

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Tcp
import com.example.mtproto.ConnectionHandler

import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Success

object Server extends App {

  implicit val system: ActorSystem = ActorSystem("ClientAndServer")
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val m: Materializer = ActorMaterializer()
  val (address, port) = ("127.0.0.1", 6000)
  server(address, port)

  def server(address: String, port: Int): Unit = {

    val messagehandler = new ConnectionHandler()

    val sockethandler = Sink.foreach[Tcp.IncomingConnection] { conn =>
      println("Client connected from: " + conn.remoteAddress)
      conn.handleWith(messagehandler.flow)
    }

    val connections = Tcp().bind(address, port)
    val binding = connections.to(sockethandler).run()

    binding.onComplete {
      case Success(b) =>
        println("Server started, listening on: " + b.localAddress)
      case Failure(e) =>
        println(s"Server could not bind to $address:$port: ${e.getMessage}")
        system.terminate()
    }

  }
}