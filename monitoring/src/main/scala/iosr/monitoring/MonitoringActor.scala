package iosr.monitoring

import akka.actor.{ActorRef, FSM, Props}
import com.typesafe.config.Config
import iosr.Messages.RegisterWorker
import iosr.monitoring.DockerClientActor._
import iosr.monitoring.MonitoringActor.{Data, MonitoringState}

class MonitoringActor(config: Config, dockerClientActor: ActorRef) extends FSM[MonitoringState, Data] {

  import MonitoringActor._

  startWith(Initial, EmptyData)

  when(Initial) {
    case Event(Start, EmptyData) =>
      dockerClientActor ! StartNewContainer(0)
      dockerClientActor ! StartNewContainer(1)
      dockerClientActor ! StartNewContainer(2)
      dockerClientActor ! StartNewContainer(3)
      Thread.sleep(10000)
      dockerClientActor ! RemoveContainer(0)
      dockerClientActor ! RemoveContainer(1)
      dockerClientActor ! RemoveContainer(2)
      dockerClientActor ! RemoveContainer(3)
      stay
    //      goto(StartingContainer) using MonitoringData(Array())
    case Event(msg@RegisterWorker, _) =>
      log.info(msg.toString)
      stay
    case Event(msg@RemoveContainerAck, _) =>
      log.info(msg.toString)
      stay
  }

  when(StartingContainer) {
    case Event(RegisterWorker, MonitoringData(registeredWorkers)) =>
      log.info(s"Registered worker #${registeredWorkers.length}: ${sender.path}")
      goto(Monitoring) using MonitoringData(registeredWorkers :+ sender())
  }

  when(RemovingContainer) {
    case Event(RemoveContainerAck(workerId), MonitoringData(registeredWorkers)) =>
      val workers = registeredWorkers.toBuffer
      val worker = workers.remove(workerId)
      log.info(s"Removed worker #$workerId: ${worker.path}")

      goto(Monitoring) using MonitoringData(workers.toArray)
  }

}

object MonitoringActor {

  def props(config: Config, dockerClientActor: ActorRef) = Props(classOf[MonitoringActor], config, dockerClientActor)

  // states
  trait MonitoringState

  case object Initial extends MonitoringState

  case object Monitoring extends MonitoringState

  case object StartingContainer extends MonitoringState

  case object RemovingContainer extends MonitoringState

  // data
  trait Data

  case object EmptyData extends Data

  case class MonitoringData(
                             registeredWorkers: Array[ActorRef]
                           ) extends Data

  // msg
  case object Start

}