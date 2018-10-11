package com.example.mtproto

import akka.NotUsed
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import com.example.mtproto.model._
import scodec.bits.BitVector

import scala.concurrent.ExecutionContext

final class ConnectionHandler()(implicit ec: ExecutionContext) {
  import ConnectionHandler._

  val flow: Flow[ByteString, ByteString, NotUsed] = Flow[ByteString]
    .statefulMapConcat(parse)
    .map(handle)
    .takeWhile(shouldStop, inclusive = true)
    .map(toBytes)

  private def parse: () => ByteString => List[MTProtoRequest] = { () =>
    var state: ParsingState = WaitingForHeader(ByteString.empty)

    in => {
      val (nextstate, out) = state.next(in)
      state = nextstate
      out.toList
    }
  }

  private def handle(req: MTProtoRequest): MTProtoResponse = {
    req match {
      case ReqPQ(nonce) =>
        ResPQ(
          nonce,
          nonce,
          "pq",
          Vector(1, 2, 3)
        )
      case ReqDHParams(nonce, _, _, _, _, encrypted_data) =>
        ServerDHParamsOk (
          nonce,
          nonce,
          encrypted_data
        )
    }
  }

  private def toBytes(res: MTProtoResponse): ByteString = {
    val bytes = Message.codec.encode(res)
      .toOption
      .get // todo - error handling
    ByteString(bytes.toByteArray)
  }

  private def shouldStop(res: MTProtoResponse): Boolean = {
    res match {
      case m: ServerDHParamsOk => false
      case _ => true
    }
  }
}

object ConnectionHandler {
  val headerlength: Int = 4 + 4 + 8

  sealed trait ParsingState {
    def next(in: ByteString): (ParsingState, Option[MTProtoRequest])
  }

  case class WaitingForHeader(buffer: ByteString) extends ParsingState {
    override def next(in: ByteString): (ParsingState, Option[MTProtoRequest]) = {
      val newbuffer = buffer ++ in
      if(buffer.size >= headerlength) {
        val (header, rest) = newbuffer.splitAt(headerlength)
        val parsed = UnencryptedMessageHeader.codec.decode(BitVector(header.asByteBuffer))
          .toOption
          .get // todo - error handling
          .value

        WaitingForPayload(parsed.message_data_length, rest.compact) -> None
      } else {
        WaitingForHeader(newbuffer) -> None
      }
    }
  }

  case class WaitingForPayload(length: Int, buffer: ByteString) extends ParsingState {
    override def next(in: ByteString): (ParsingState, Option[MTProtoRequest]) = {
      val newbuffer = buffer ++ in
      if(buffer.size >= length) {
        val (header, rest) = newbuffer.splitAt(headerlength)
        val parsed = Message.codec.decode(BitVector(header.asByteBuffer))
          .toOption
          .get // todo - error handling
          .value

        WaitingForHeader(rest) -> None
      } else {
        WaitingForPayload(length, newbuffer.compact) -> None
      }
    }
  }
}
