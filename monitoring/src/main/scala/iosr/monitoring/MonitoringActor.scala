package iosr.monitoring

import akka.actor.{ActorRef, FSM, Props}
import com.typesafe.config.Config
import iosr.Messages.{Deregister, LoadData, RegisterWorker, TerminateWorker}
import iosr.monitoring.DockerClientActor._
import iosr.monitoring.MonitoringActor.{Data, MonitoringState}

import scala.concurrent.duration._
import scala.language.postfixOps

class MonitoringActor(config: Config, dockerClientActor: ActorRef) extends FSM[MonitoringState, Data] {

  import MonitoringActor._
  import context.dispatcher

  private val minWorkers = config.getInt("monitoring.min-workers")
  private val maxWorkers = config.getInt("monitoring.max-workers")
  private val minLoad = config.getInt("monitoring.min-load")
  private val maxLoad = config.getInt("monitoring.max-load")
  private val loadCheckInterval = config.getInt("monitoring.load-check-interval-seconds") seconds

  startWith(Initial, EmptyData)

  when(Initial) {
    case Event(Start, EmptyData) =>
      dockerClientActor ! StartNewContainer(0)
      context.system.scheduler.schedule(30 seconds, loadCheckInterval, self, CheckLoad)
      goto(StartingContainer) using MonitoringData(Array(), Map())
  }

  when(StartingContainer)(handleLoadData orElse {
    case Event(RegisterWorker, MonitoringData(registeredWorkers, loadByWorkers)) =>
      log.info(s"Registered worker #${registeredWorkers.length}: ${sender.path}")
      goto(Monitoring) using MonitoringData(registeredWorkers :+ sender(), loadByWorkers)
    case Event(CheckLoad, _) =>
      stay
  })

  when(RemovingContainer)(handleLoadData orElse {
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
    case Event(CheckLoad, _) =>
      stay
  })

  when(Monitoring)(handleLoadData orElse {
    case Event(CheckLoad, MonitoringData(registeredWorkers, loadByWorkers)) =>
      val a                             vgLoad = loadByWorkers.values.sum / loadByWorkers.size
      log.info(s"Current average load is: $avgLoad")
      if(avgLoad < minLoad && registeredWorkers.length > minWorkers) {
        log.info(s"Load below threshold. Removing worker${registeredWorkers.length-1}")
        registeredWorkers.last ! Deregister
        goto(RemovingContainer)
      }
      else if (avgLoad > maxLoad && registeredWorkers.length < maxWorkers) {
        val workerId = registeredWorkers.length
        log.info(s"Load over threshold. Starting worker${workerId}")
        dockerClientActor ! StartNewContainer(workerId)
        goto(StartingContainer)
      }
      else {
        stay
      }
  })

  private def handleLoadData: StateFunction = {
    case Event(LoadData(numOfRequests), MonitoringData(registeredWorkers, loadByWorkers)) =>
      log.info(s"Got LoadData: $numOfRequests from worker${registeredWorkers.indexOf(sender)}")
      val updatedLoadByWorkers: Map[ActorRef, Long] = loadByWorkers + (sender -> numOfRequests)
      stay using MonitoringData(registeredWorkers, updatedLoadByWorkers)
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

  case object CheckLoad

}