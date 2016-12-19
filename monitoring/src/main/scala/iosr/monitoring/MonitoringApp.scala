package iosr.monitoring

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import iosr.monitoring.MonitoringActor.Start

object MonitoringApp extends App {
  val config = ConfigFactory.load()
  val system = ActorSystem("Monitoring", config)

  val dockerClientActor = system.actorOf(DockerClientActor.props(config), "dockerClientActor")
  val monitoringActor = system.actorOf(MonitoringActor.props(config, dockerClientActor), "monitoringActor")

  monitoringActor ! Start
}
