package iosr.worker

import akka.actor.{ActorPath, ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import iosr.worker.WorkerActor.Startup

object WorkerApp extends App {
  val config = ConfigFactory.load()
  val system = ActorSystem("WorkerApp", config)
  val workerActor = system.actorOf(Props[WorkerActor])
  val supervisorAddress = config.getString("supervisor.address")
  val supervisorPath = ActorPath.fromString(s"akka://$supervisorAddress/user/supervisorActor")
  workerActor ! Startup(supervisorPath)
}
