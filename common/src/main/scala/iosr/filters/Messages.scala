package iosr.filters

object Messages {

  case class ScaleMessage(image: Array[Byte], params: ScaleParams)

  case class TwirlMessage(image: Array[Byte], params: TwirlParams)

  case class SparkleMessage(image: Array[Byte], params: SparkleParams)

  case class MultiFilterMessage(image: Array[Byte], params: List[Params])

  case class DoneMessage(image: Array[Byte])

}
