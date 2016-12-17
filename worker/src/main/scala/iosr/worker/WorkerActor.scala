package iosr.worker

import akka.actor.{ActorPath, ActorRef, ActorSelection, Cancellable, LoggingFSM, Props, Terminated}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Routee, Router}
import iosr.Messages._
import iosr.worker.WorkerActor._

import scala.collection.immutable.Queue
import scala.concurrent.duration._
import scala.language.postfixOps

class WorkerActor extends LoggingFSM[WorkerState, WorkerData] {

  import context.dispatcher

  when(Initial) {
    case Event(Startup(supervisorPath, monitoringPath), EmptyData) =>
      val supervisor = context.actorSelection(supervisorPath)
      val monitoring = context.actorSelection(monitoringPath)
      supervisor ! RegisterWorker
      val router = initRouter(5)
      goto(Registering) using InitialData(router, supervisor, monitoring)
  }

  when(Registering) {
    case Event(RegisterWorkerAck, InitialData(router, supervisor, monitoring)) =>
      val requestsCancellable = context.system.scheduler.schedule(10 millis, 10 millis, self, CheckForRequests)
      goto(Running) using RequestsData(Queue.empty, requestsCancellable, router, supervisor, monitoring)
    case Event(Terminated(oldRoutee), id@InitialData(router, _, _)) =>
      val newRouter = handleTerminatedRoutee(oldRoutee, router)
      stay using id.copy(router = newRouter)
  }

  when(Running)(handleRequests orElse {
    case Event(CheckForRequests, _) =>
      stay
    case Event(Terminated(oldRoutee), rd@RequestsData(_, _, router, _, _)) =>
      val newRouter = handleTerminatedRoutee(oldRoutee, router)
      stay using rd.copy(router = newRouter)
    case Event(Deregister, RequestsData(_, _, _, supervisor, _)) =>
      supervisor ! DeregisterWorker
      goto(Deregistering)
  })

  when(Deregistering)(handleRequests orElse {
    case Event(Terminated(oldRoutee), rd@RequestsData(_, _, router, _, _)) =>
      val newRouter = handleTerminatedRoutee(oldRoutee, router)
      stay using rd.copy(router = newRouter)
    case Event(DeregisterWorkerAck, RequestsData(requestsWithActors, requestsCancellable, _, _, monitoring)) if requestsWithActors.isEmpty =>
      requestsCancellable.cancel()
      context.system.scheduler.scheduleOnce(1 second, self, TerminateNow)
      goto(Terminating) using TerminatingData(monitoring)
    case Event(DeregisterWorkerAck, _) =>
      goto(Deregistered)
  })

  when(Deregistered)(handleRequests orElse {
    case Event(CheckForRequests, RequestsData(_, requestsCancellable, _, _, monitoring)) =>
      requestsCancellable.cancel()
      context.system.scheduler.scheduleOnce(1 second, self, TerminateNow)
      goto(Terminating) using TerminatingData(monitoring)
  })

  when(Terminating) {
    case Event(TerminateNow, TerminatingData(monitoring)) =>
      monitoring ! TerminateWorker
      context.system.terminate()
      stop()
  }

  startWith(Initial, EmptyData)

  private def handleRequests: StateFunction = {
    case Event(r: Request, rd@RequestsData(requestsWithActors, _, _, _, _)) =>
      val senderActor = sender()
      stay using rd.copy(requestsWithActors = requestsWithActors.enqueue((r, senderActor)))
    case Event(CheckForRequests, rd@RequestsData(requestsWithActors, _, router, _, _)) if requestsWithActors.nonEmpty =>
      val ((request, actor), newQueue) = requestsWithActors.dequeue
      router.route(request, actor)
      stay using rd.copy(requestsWithActors = newQueue)
  }

  private def handleTerminatedRoutee(routee: ActorRef, router: Router) = {
    val routerWithRemovedTerminatedRoutee = router.removeRoutee(routee)
    val newRoutee = spawnRoutee()
    routerWithRemovedTerminatedRoutee.addRoutee(newRoutee)
  }

  private def initRouter(numberOfRoutees: Int) = {
    val routees = Vector.fill(numberOfRoutees)(spawnRoutee())
    Router(RoundRobinRoutingLogic(), routees)
  }

  private def spawnRoutee(): Routee = {
    val routeeActor = context.actorOf(Props[FilterProcessor])
    context watch routeeActor
    ActorRefRoutee(routeeActor)
  }
}

object WorkerActor {

  //Data

  sealed trait WorkerData

  case object EmptyData extends WorkerData

  case class InitialData(router: Router,
                         supervisor: ActorSelection,
                         monitoring: ActorSelection)
    extends WorkerData

  case class RequestsData(requestsWithActors: Queue[(Request, ActorRef)],
                          requestsCancellable: Cancellable,
                          router: Router,
                          supervisor: ActorSelection,
                          monitoring: ActorSelection)
    extends WorkerData

  case class TerminatingData(monitoring: ActorSelection) extends WorkerData

  //State

  sealed trait WorkerState

  case object Initial extends WorkerState

  case object Registering extends WorkerState

  case object Running extends WorkerState

  case object Deregistering extends WorkerState

  case object Deregistered extends WorkerState

  case object Terminating extends WorkerState

  //Messages

  case class Startup(supervisorPath: ActorPath, monitoringPath: ActorPath)

  case object CheckForRequests

  case object TerminateNow

}
