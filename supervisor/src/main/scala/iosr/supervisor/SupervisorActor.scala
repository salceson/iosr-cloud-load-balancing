package iosr.supervisor

import akka.actor.{Actor, ActorLogging}
import akka.routing.{RoundRobinRoutingLogic, Router}
import iosr.Messages._

class SupervisorActor extends Actor with ActorLogging {

  private val router = Router(RoundRobinRoutingLogic())

  override def receive: Receive = {
    case RegisterWorker =>
      val worker = sender()
      log.info(s"Registering worker $worker")
      router.addRoutee(worker)
      worker ! RegisterWorkerAck
    case DeregisterWorker =>
      val worker = sender()
      log.info(s"Deregistering worker $worker")
      router.removeRoutee(worker)
      worker ! DeregisterWorkerAck
    case msg@Request =>
      router.route(msg, sender())
  }

}