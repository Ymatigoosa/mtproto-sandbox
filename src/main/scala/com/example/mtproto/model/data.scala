package com.example.mtproto.model

import java.nio.charset.Charset

import scodec._
import codecs.{bits => cbits, _}
import scodec.bits._
import scodec.bits.BitVector
import Common._

object Common {
  val vectorLong: Codec[Vector[Long]] =
    discriminated[Vector[Long]].by(cbits(32))
    .| (hex"1cb5c415".bits){ case i => i}(identity)(vectorOfN(int32L, int64L))

  val str: Codec[String] =
    string32L(Charset.forName("UTF-8"))
}

sealed trait Message

object Message {
  val codec: DiscriminatorCodec[Message, BitVector] =
    discriminated[Message].by(cbits(32))
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
      int64L ::
      int64L ::
      int32L
    ).as[UnencryptedMessageHeader]
}

case class ReqPQ(
  nonce: BitVector
) extends MTProtoRequest

object ReqPQ {
  val header: BitVector = hex"60469778".bits

  val codec: Codec[ReqPQ] = cbits(128).as[ReqPQ]
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
      cbits(128) ::
      cbits(128) ::
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
      cbits(128) ::
      cbits(128) ::
      str ::
      str ::
      int64L ::
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
      cbits(128) ::
      cbits(128) ::
      str
    ).as[ServerDHParamsOk]
}

