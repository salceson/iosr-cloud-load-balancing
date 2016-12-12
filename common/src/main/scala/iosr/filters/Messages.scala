package iosr.filters

object Messages {

  case class ScaleCommand(image: Array[Byte], params: ScaleParams)

  case class TwirlCommand(image: Array[Byte], params: TwirlParams)

  case class SparkleCommand(image: Array[Byte], params: SparkleParams)

  case class RotateCommand(image: Array[Byte], params: RotateParams)

  case class ContrastCommand(image: Array[Byte], params: ContrastParams)

  case class Request(image: Array[Byte], operationsParams: List[Params])

  case class Response(image: Array[Byte])

  case object RegisterWorker

  case object RegisterWorkerAck

  case object DeregisterWorker

  case object DeregisterWorkerAck

}
