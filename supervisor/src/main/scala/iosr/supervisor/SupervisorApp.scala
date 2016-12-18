package iosr.supervisor

import akka.actor.{ActorPath, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object SupervisorApp extends App {
  val config = ConfigFactory.load()
  val system = ActorSystem("Supervisor", config)

  val supervisorActor = system.actorOf(Props[SupervisorActor], "supervisorActor")
}
