package iosr

import iosr.filters._

object Messages {

  // Worker => Filters

  case class ScaleCommand(image: Array[Byte], params: ScaleParams)

  case class TwirlCommand(image: Array[Byte], params: TwirlParams)

  case class SparkleCommand(image: Array[Byte], params: SparkleParams)

  case class RotateCommand(image: Array[Byte], params: RotateParams)

  case class ContrastCommand(image: Array[Byte], params: ContrastParams)

  // Request and response

  case class Request(id: String, image: Array[Byte], operationsParams: List[Params])

  case class Response(id: String, image: Array[Byte])

  // Supervisor <=> Worker

  case object RegisterWorker

  case object RegisterWorkerAck

  case object DeregisterWorker

  case object DeregisterWorkerAck

  // LB <=> Worker

  case object Deregister

  // Monitoring <=> Worker

  case object TerminateWorker

  case class LoadData(numOfRequests: Long)

  // Filters => Worker

  case class FilterDone(image: Array[Byte])

}
