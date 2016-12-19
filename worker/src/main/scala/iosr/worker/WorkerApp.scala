package iosr.worker

import akka.actor.{ActorPath, ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import iosr.worker.WorkerActor.Startup

import scala.concurrent.duration._
import scala.language.postfixOps

object WorkerApp extends App {
  val config = ConfigFactory.load()
  val system = ActorSystem("WorkerApp", config)

  val workerActor = system.actorOf(Props[WorkerActor], "workerActor")

  val supervisorAddress = config.getString("supervisor.address")
  val supervisorPath = ActorPath.fromString(s"akka.tcp://Supervisor@$supervisorAddress/user/supervisorActor")

  val monitoringAddress = config.getString("monitoring.address")
  val monitoringPath = ActorPath.fromString(s"akka.tcp://Monitoring@$monitoringAddress/user/monitoringActor")

  val delay = config.getInt("monitoring.delay") seconds

  workerActor ! Startup(supervisorPath, monitoringPath, delay)
}
