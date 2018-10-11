package sample.stream

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Tcp
import akka.util.ByteString

import scala.concurrent.ExecutionContext
import scala.util.Failure
import scala.util.Success

object Server extends App {

  implicit val system: ActorSystem = ActorSystem("ClientAndServer")
  implicit val ec: ExecutionContext = system.dispatcher
  implicit val m: Materializer = ActorMaterializer()
  val (address, port) = ("127.0.0.1", 6000)
  server(system, address, port)

  def server(system: ActorSystem, address: String, port: Int): Unit = {

    val handler = Sink.foreach[Tcp.IncomingConnection] { conn =>
      println("Client connected from: " + conn.remoteAddress)
      conn handleWith Flow[ByteString]
    }

    val connections = Tcp().bind(address, port)
    val binding = connections.to(handler).run()

    binding.onComplete {
      case Success(b) =>
        println("Server started, listening on: " + b.localAddress)
      case Failure(e) =>
        println(s"Server could not bind to $address:$port: ${e.getMessage}")
        system.terminate()
    }

  }
}