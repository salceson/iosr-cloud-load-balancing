package iosr.frontend

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import iosr.frontend.utils.Resources

import scala.concurrent.duration._
import scala.language.postfixOps

object FrontendApp extends App {
  val appConfig = ConfigFactory.load()

  val host = appConfig.getString("http.host")
  val port = appConfig.getInt("http.port")

  implicit val actorSystem = ActorSystem("Frontend")
  implicit val materializer = ActorMaterializer()

  implicit val ec = actorSystem.dispatcher
  implicit val timeout = Timeout(10 seconds)

  val resources = new Resources {
    override def config: Config = appConfig

    override def system: ActorSystem = actorSystem
  }

  Http().bindAndHandle(handler = resources.routes, interface = host, port = port) map { binding =>
    actorSystem.log.info(s"Server started. Listening on ${binding.localAddress}")
  } recover { case e: Exception =>
    actorSystem.log.error(e, s"Unable to bind to $host:$port")
  }
}
