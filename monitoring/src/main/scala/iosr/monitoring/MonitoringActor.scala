package iosr.monitoring

import akka.actor.{ActorRef, FSM, Props}
import com.typesafe.config.Config
import iosr.Messages.{Deregister, LoadData, RegisterWorker, TerminateWorker}
import iosr.monitoring.DockerClientActor._
import iosr.monitoring.MonitoringActor.{Data, MonitoringState}

class MonitoringActor(config: Config, dockerClientActor: ActorRef) extends FSM[MonitoringState, Data] {

  import MonitoringActor._

  private val minWorkers = config.getInt("monitoring.min-workers")
  private val maxWorkers = config.getInt("monitoring.max-workers")
  private val minLoad = config.getInt("monitoring.min-load")
  private val maxLoad = config.getInt("monitoring.max-load")

  startWith(Initial, EmptyData)

  when(Initial) {
    case Event(Start, EmptyData) =>
      dockerClientActor ! StartNewContainer(0)
      goto(StartingContainer) using MonitoringData(Array(), Map())
  }

  when(StartingContainer) {
    case Event(RegisterWorker, MonitoringData(registeredWorkers, loadByWorkers)) =>
      log.info(s"Registered worker #${registeredWorkers.length}: ${sender.path}")
      goto(Monitoring) using MonitoringData(registeredWorkers :+ sender(), loadByWorkers)
  }

  when(RemovingContainer) {
    case Event(TerminateWorker, MonitoringData(registeredWorkers, _)) =>
      val workerId = registeredWorkers.indexOf(sender)
      log.info(s"Got TerminateWorker from worker$workerId: ${sender.path}")
      dockerClientActor ! RemoveContainer(workerId)
      stay
    case Event(RemoveContainerAck(workerId), MonitoringData(registeredWorkers, loadByWorkers)) =>
      val workers = registeredWorkers.toBuffer
      val worker = workers.remove(workerId)
      log.info(s"Removed worker #$workerId: ${worker.path}")

      goto(Monitoring) using MonitoringData(workers.toArray, loadByWorkers)
  }

  when(Monitoring) {
    case Event(LoadData(numOfRequests), MonitoringData(registeredWorkers, loadByWorkers)) =>
      val updatedLoadByWorkers: Map[ActorRef, Long] = loadByWorkers + (sender -> numOfRequests)
      val avgLoad = updatedLoadByWorkers.values.sum / updatedLoadByWorkers.foldLeft(0L)(_ + _._2).toDouble
      if(avgLoad < minLoad && registeredWorkers.length > minWorkers) {
        log.info(s"Load below threshold. Removing worker${registeredWorkers.length-1}")
        registeredWorkers.last ! Deregister
        goto(RemovingContainer) using MonitoringData(registeredWorkers, updatedLoadByWorkers)
      }
      else if (avgLoad > maxLoad && registeredWorkers.length < maxWorkers) {
        val workerId = registeredWorkers.length
        log.info(s"Load over threshold. Starting worker${workerId}")
        dockerClientActor ! StartNewContainer(workerId)
        goto(StartingContainer) using MonitoringData(registeredWorkers, updatedLoadByWorkers)
      }
      else {
        stay using MonitoringData(registeredWorkers, updatedLoadByWorkers)
      }
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
                             registeredWorkers: Array[ActorRef],
                             loadByWorkers: Map[ActorRef, Long]
                           ) extends Data

  // msg
  case object Start

}