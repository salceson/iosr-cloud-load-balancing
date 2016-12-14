package iosr.worker

import akka.actor.{ActorRef, ActorSelection, Cancellable, LoggingFSM, Props, Terminated}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Routee, Router}
import iosr.Messages._
import iosr.worker.WorkerActor._

import scala.collection.immutable.Queue
import scala.concurrent.duration._
import scala.language.postfixOps

class WorkerActor extends LoggingFSM[WorkerState, WorkerData] {

  import context.dispatcher

  when(Initial) {
    case Event(Startup(supervisorAddress), EmptyData) =>
      val supervisor = context.actorSelection(s"akka://$supervisorAddress/user/SupervisorActor")
      supervisor ! RegisterWorker
      val router = initRouter(5)
      goto(Registering) using InitialData(router, supervisor)
  }

  when(Registering) {
    case Event(RegisterWorkerAck, InitialData(router, supervisor)) =>
      val requestsCancellable = context.system.scheduler.schedule(10 millis, 10 millis, self, CheckForRequests)
      goto(Running) using RequestsData(Queue.empty, requestsCancellable, router, supervisor)
    case Event(Terminated(oldRoutee), id@InitialData(router, _)) =>
      val newRouter = handleTerminatedRoutee(oldRoutee, router)
      stay using id.copy(router = newRouter)
  }

  when(Running) {
    case Event(r: Request, rd@RequestsData(requestsWithActors, _, _, _)) =>
      val senderActor = sender()
      stay using rd.copy(requestsWithActors = requestsWithActors.enqueue((r, senderActor)))
    case Event(CheckForRequests, rd@RequestsData(requestsWithActors, _, router, _)) if requestsWithActors.nonEmpty =>
      val ((request, actor), newQueue) = requestsWithActors.dequeue
      router.route(request, actor)
      stay using rd.copy(requestsWithActors = newQueue)
    case Event(CheckForRequests, _) =>
      stay
    case Event(Terminated(oldRoutee), rd@RequestsData(_, _, router, _)) =>
      val newRouter = handleTerminatedRoutee(oldRoutee, router)
      stay using rd.copy(router = newRouter)
    case Event(Deregister, RequestsData(_, _, _, supervisor)) =>
      supervisor ! DeregisterWorker
      goto(Deregistering)
  }

  when(Deregistering) {
    case Event(r: Request, rd@RequestsData(requestsWithActors, _, _, _)) =>
      val senderActor = sender()
      stay using rd.copy(requestsWithActors = requestsWithActors.enqueue((r, senderActor)))
    case Event(CheckForRequests, rd@RequestsData(requestsWithActors, _, router, _)) if requestsWithActors.nonEmpty =>
      val ((request, actor), newQueue) = requestsWithActors.dequeue
      router.route(request, actor)
      stay using rd.copy(requestsWithActors = newQueue)
    case Event(CheckForRequests, RequestsData(_, requestsCancellable, _, _)) =>
      requestsCancellable.cancel()
      context.system.scheduler.scheduleOnce(1 second, self, TerminateNow)
      goto(Terminating) using EmptyData
    case Event(Terminated(oldRoutee), rd@RequestsData(_, _, router, _)) =>
      val newRouter = handleTerminatedRoutee(oldRoutee, router)
      stay using rd.copy(router = newRouter)
  }

  when(Terminating) {
    case Event(TerminateNow, EmptyData) =>
      context.system.terminate()
      stop()
  }

  startWith(Initial, EmptyData)

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

  case class InitialData(router: Router, supervisor: ActorSelection) extends WorkerData

  case class RequestsData(requestsWithActors: Queue[(Request, ActorRef)],
                          requestsCancellable: Cancellable,
                          router: Router,
                          supervisor: ActorSelection)
    extends WorkerData

  //State

  sealed trait WorkerState

  case object Initial extends WorkerState

  case object Registering extends WorkerState

  case object Running extends WorkerState

  case object Deregistering extends WorkerState

  case object Terminating extends WorkerState

  //Messages

  case class Startup(supervisorAddress: String)

  case object CheckForRequests

  case object TerminateNow

}
