package iosr.worker

import akka.actor.{ActorPath, ActorRef, ActorSelection, LoggingFSM, Props}
import akka.pattern.ask
import iosr.Messages._
import iosr.filters._
import iosr.worker.WorkerActor._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class WorkerActor extends LoggingFSM[WorkerState, WorkerData] {

  import context.dispatcher

  private val scaleFilterActor = context.actorOf(Props[ScaleFilter])
  private val rotateFilterActor = context.actorOf(Props[RotateFilter])
  private val sparkleFilterActor = context.actorOf(Props[SparkleFilter])
  private val contrastFilterActor = context.actorOf(Props[ContrastFilter])
  private val twirlFilterActor = context.actorOf(Props[TwirlFilter])

  private val filterTimeoutDuration = 5 seconds
  private implicit val timeout: akka.util.Timeout = filterTimeoutDuration

  when(Initial) {
    case Event(Startup(supervisorPath, monitoringPath), EmptyData) =>
      val supervisor = context.actorSelection(supervisorPath)
      val monitoring = context.actorSelection(monitoringPath)
      supervisor ! RegisterWorker
      monitoring ! RegisterWorker
      goto(Registering) using RunningData(supervisor, monitoring)
  }

  when(Registering) {
    case Event(RegisterWorkerAck, RunningData(supervisor, monitoring)) =>
      log.info("Got RegisterWorkerAck")
      goto(Running) using RunningData(supervisor, monitoring)
  }

  when(Running)(handleRequests orElse {
    case Event(Deregister, RunningData(supervisor, _)) =>
      log.info("Got Deregister")
      supervisor ! DeregisterWorker
      goto(Deregistering)
  })

  when(Deregistering)(handleRequests orElse {
    case Event(DeregisterWorkerAck, RunningData(_, monitoring)) =>
      log.info("Got DeregisterWorkerAck")
      monitoring ! TerminateWorker
      context.system.scheduler.scheduleOnce(1 minute, self, TerminateNow)
      goto(Terminating) using EmptyData
  })

  when(Terminating) {
    case Event(TerminateNow, EmptyData) =>
      context.system.terminate()
      stop()
  }

  startWith(Initial, EmptyData)

  private def handleRequests: StateFunction = {
    case Event(request: Request, _) =>
      val senderActor = sender()
      handleRequest(request, senderActor)
      stay
  }

  private def handleRequest(request: Request, senderActor: ActorRef): Unit = {
    val id = request.id
    val imageBytes = request.image
    val operationsParams = request.operationsParams
    val result = operationsParams.foldLeft[Array[Byte]](imageBytes) {
      case (image, scaleParams: ScaleParams) =>
        val response = Await.result(
          scaleFilterActor ? ScaleCommand(image, scaleParams),
          filterTimeoutDuration
        )
        response.asInstanceOf[FilterDone].image
      case (image, rotateParams: RotateParams) =>
        val response = Await.result(
          rotateFilterActor ? RotateCommand(image, rotateParams),
          filterTimeoutDuration
        )
        response.asInstanceOf[FilterDone].image
      case (image, sparkleParams: SparkleParams) =>
        val response = Await.result(
          sparkleFilterActor ? SparkleCommand(image, sparkleParams),
          filterTimeoutDuration
        )
        response.asInstanceOf[FilterDone].image
      case (image, contrastParams: ContrastParams) =>
        val response = Await.result(
          contrastFilterActor ? ContrastCommand(image, contrastParams),
          filterTimeoutDuration
        )
        response.asInstanceOf[FilterDone].image
      case (image, twirlParams: TwirlParams) =>
        val response = Await.result(
          twirlFilterActor ? TwirlCommand(image, twirlParams),
          filterTimeoutDuration
        )
        response.asInstanceOf[FilterDone].image
    }
    senderActor ! Response(id, result)
  }
}

object WorkerActor {

  //Data

  sealed trait WorkerData

  case object EmptyData extends WorkerData

  case class RunningData(supervisor: ActorSelection, monitoring: ActorSelection) extends WorkerData

  //State

  sealed trait WorkerState

  case object Initial extends WorkerState

  case object Registering extends WorkerState

  case object Running extends WorkerState

  case object Deregistering extends WorkerState

  case object Terminating extends WorkerState

  //Messages

  case class Startup(supervisorPath: ActorPath, monitoringPath: ActorPath)

  case object TerminateNow

}
