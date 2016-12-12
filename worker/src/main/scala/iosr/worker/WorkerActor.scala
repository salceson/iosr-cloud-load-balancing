package iosr.worker

import akka.actor.{ActorRef, Cancellable, FSM, Props, Terminated}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Routee, Router}
import iosr.filters.Messages.{RegisterWorker, RegisterWorkerAck, Request}
import iosr.worker.WorkerActor._

import scala.concurrent.duration._
import scala.language.postfixOps

class WorkerActor extends FSM[WorkerState, WorkerData] {

  import context.dispatcher

  when(Initial) {
    case Event(Startup(supervisorAddress), EmptyData) =>
      val supervisor = context.actorSelection(s"akka://$supervisorAddress/user/SupervisorActor")
      supervisor ! RegisterWorker
      val router = initRouter(5)
      goto(Registering) using InitialData(router)
  }

  when(Registering) {
    case Event(RegisterWorkerAck, InitialData(router)) =>
      val requestsCancellable = context.system.scheduler.schedule(10 millis, 10 millis, self, CheckForRequests)
      goto(Running) using RequestsData(List(), requestsCancellable, router)
    case Event(Terminated(oldRoutee), InitialData(router)) =>
      val newRouter = handleTerminatedRoutee(oldRoutee, router)
      stay using InitialData(newRouter)
  }

  when(Running) {
    case Event(r: Request, rd@RequestsData(requestsWithActors, _, _)) =>
      val senderActor = sender()
      stay using rd.copy(requestsWithActors = (r, senderActor) :: requestsWithActors)
    case Event(CheckForRequests, rd@RequestsData((request, actor) :: rest, _, router)) =>
      router.route(request, actor)
      stay using rd.copy(requestsWithActors = rest)
    case Event(Terminated(oldRoutee), rd@RequestsData(_, _, router)) =>
      val newRouter = handleTerminatedRoutee(oldRoutee, router)
      stay using rd.copy(router = newRouter)
    //TODO: Handle deregistration
  }

  //TODO: Deregistration state

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

  case class InitialData(router: Router) extends WorkerData

  case class RequestsData(requestsWithActors: List[(Request, ActorRef)],
                          requestsCancellable: Cancellable,
                          router: Router) extends WorkerData

  //State

  sealed trait WorkerState

  case object Initial extends WorkerState

  case object Registering extends WorkerState

  case object Running extends WorkerState

  case object Deregistering extends WorkerState

  //Messages

  case class Startup(supervisorAddress: String)

  case object CheckForRequests

}
