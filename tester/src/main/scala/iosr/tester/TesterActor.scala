package iosr.tester

import akka.actor.{Actor, ActorLogging, ActorPath, Props}
import iosr.Messages.{Request, Response}
import iosr.filters._

import scala.concurrent.duration._
import scala.language.postfixOps

class TesterActor(supervisorPath: ActorPath, image: Array[Byte]) extends Actor with ActorLogging {
  private val supervisor = context.actorSelection(supervisorPath)

  import context.dispatcher
  import iosr.tester.TesterActor._

  context.system.scheduler.schedule(1 second, 20 seconds, self, SendRequests)
  context.system.scheduler.scheduleOnce(1 minute, self, Terminate)

  override def receive: Receive = {
    case SendRequests =>
      (0 until 50) foreach { i =>
        log.info(s"Sending message ${i + 1}...")
        supervisor ! getRequest(i + 1)
      }
    case Response(id, _) =>
      log.info(s"Got response for $id")
    case Terminate =>
      context.system.terminate()
  }

  private def getRequest(i: Int): Request = {
    Request(s"Request-$i", image, List(
      ScaleParams(200, 200, preserveRatio = true),
      TwirlParams(20),
      SparkleParams(),
      RotateParams(RotateLeft),
      ContrastParams(0.8)
    ))
  }
}

object TesterActor {
  def props(supervisorPath: ActorPath, image: Array[Byte]): Props =
    Props(classOf[TesterActor], supervisorPath, image)

  case object SendRequests

  case object Terminate

}
