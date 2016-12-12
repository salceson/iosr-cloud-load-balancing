package iosr.worker

import akka.actor.{Actor, Props}
import akka.pattern.ask
import akka.util.Timeout
import iosr.filters.Messages._
import iosr.filters._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class FilterProcessor extends Actor {
  private val scaleFilterActor = context.actorOf(Props[ScaleFilter])
  private val rotateFilterActor = context.actorOf(Props[RotateFilter])
  private val sparkleFilterActor = context.actorOf(Props[SparkleFilter])
  private val contrastFilterActor = context.actorOf(Props[ContrastFilter])
  private val twirlFilterActor = context.actorOf(Props[TwirlFilter])

  private val timeoutDuration = 5 seconds
  private implicit val timeout: Timeout = timeoutDuration

  override def receive: Receive = {
    case Request(imageBytes, operationsParams) =>
      val senderActor = sender()
      val result = operationsParams.foldLeft[Array[Byte]](imageBytes) {
        case (image, scaleParams: ScaleParams) =>
          val response = Await.result(
            scaleFilterActor ? ScaleCommand(image, scaleParams),
            timeoutDuration
          )
          response.asInstanceOf[Response].image
        case (image, rotateParams: RotateParams) =>
          val response = Await.result(
            rotateFilterActor ? RotateCommand(image, rotateParams),
            timeoutDuration
          )
          response.asInstanceOf[Response].image
        case (image, sparkleParams: SparkleParams) =>
          val response = Await.result(
            sparkleFilterActor ? SparkleCommand(image, sparkleParams),
            timeoutDuration
          )
          response.asInstanceOf[Response].image
        case (image, contrastParams: ContrastParams) =>
          val response = Await.result(
            contrastFilterActor ? ContrastCommand(image, contrastParams),
            timeoutDuration
          )
          response.asInstanceOf[Response].image
        case (image, twirlParams: TwirlParams) =>
          val response = Await.result(
            twirlFilterActor ? TwirlCommand(image, twirlParams),
            timeoutDuration
          )
          response.asInstanceOf[Response].image
      }
      senderActor ! Response(result)
  }
}
