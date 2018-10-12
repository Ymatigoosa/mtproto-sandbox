package com.example.mtproto.model

import java.nio.charset.Charset

import scodec._
import scodec.{codecs => C}
import scodec.bits._
import scodec.bits.BitVector
import Common._
import scodec.codecs.{DiscriminatorCodec, discriminated}

object Common {
  val vectorLong: Codec[Vector[Long]] =
    discriminated[Vector[Long]].by(C.bits(32))
    .| (hex"1cb5c415".bits){ case i => i}(identity)(C.vectorOfN(C.int32, C.int64))

  val str: Codec[String] =
    C.string32(Charset.forName("UTF-8"))
}

sealed trait Message

object Message {
  val codec: DiscriminatorCodec[Message, BitVector] =
    C.discriminated[Message].by(C.bits(32))
    .| (ReqPQ.header)             { case i: ReqPQ => i}(identity)(ReqPQ.codec)
    .| (ResPQ.header)             { case i: ResPQ => i}(identity)(ResPQ.codec)
    .| (ReqDHParams.header)       { case i: ReqDHParams => i}(identity)(ReqDHParams.codec)
    .| (ServerDHParamsOk.header)  { case i: ServerDHParamsOk => i}(identity)(ServerDHParamsOk.codec)
}

sealed trait MTProtoRequest extends Message
sealed trait MTProtoResponse extends Message

case class UnencryptedMessageHeader(auth_key_id: Long, message_id: Long, message_data_length: Int)

object UnencryptedMessageHeader {
  val codec: Codec[UnencryptedMessageHeader] = (
      C.int64 ::
      C.int64 ::
      C.int32
    ).as[UnencryptedMessageHeader]
}

case class ReqPQ(
  nonce: BitVector
) extends MTProtoRequest

object ReqPQ {
  val header: BitVector = hex"60469778".bits

  val codec: Codec[ReqPQ] = C.bits(128).as[ReqPQ]
}

case class ResPQ (
  nonce: BitVector,
  server_nonce: BitVector,
  pq: String,
  server_public_key_fingerprints: Vector[Long]
) extends MTProtoResponse

object ResPQ {
  val header: BitVector = hex"05162463".bits

  val codec: Codec[ResPQ] = (
      C.bits(128) ::
      C.bits(128) ::
      str ::
      vectorLong
    ).as[ResPQ]
}

case class ReqDHParams(
  nonce: BitVector,
  server_nonce: BitVector,
  p: String,
  q: String,
  public_key_fingerprint: Long,
  encrypted_data: String
) extends MTProtoRequest

object ReqDHParams {
  val header: BitVector = hex"d712e4be".bits

  val codec: Codec[ReqDHParams] = (
      C.bits(128) ::
      C.bits(128) ::
      str ::
      str ::
      C.int64 ::
      str
    ).as[ReqDHParams]
}

case class ServerDHParamsOk (
  nonce: BitVector,
  server_nonce: BitVector,
  encrypted_answer: String
) extends MTProtoResponse

object ServerDHParamsOk {
  val header: BitVector = hex"d0e8075c".bits

  val codec: Codec[ServerDHParamsOk] = (
      C.bits(128) ::
      C.bits(128) ::
      str
    ).as[ServerDHParamsOk]
}

