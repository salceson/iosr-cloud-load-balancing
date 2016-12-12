package iosr.worker

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import iosr.worker.WorkerActor.Startup

object WorkerApp extends App {
  val config = ConfigFactory.load()
  val system = ActorSystem("WorkerApp", config)
  val workerActor = system.actorOf(Props[WorkerActor])
  workerActor ! Startup(config.getString("supervisor.address"))
}
