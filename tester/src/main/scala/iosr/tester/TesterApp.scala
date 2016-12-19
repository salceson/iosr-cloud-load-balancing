package iosr.tester

import java.io.BufferedInputStream
import java.util.Base64

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.typesafe.config.ConfigFactory

import scala.concurrent.Future
import scala.language.postfixOps

object TesterApp extends App {
  val config = ConfigFactory.load()
  implicit val system = ActorSystem("Tester", config)
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  val supervisorAddress = config.getString("supervisor.address").split(":")
  val host = supervisorAddress(0)
  val port = supervisorAddress(1).toInt

  val imageResource = getClass.getClassLoader.getResourceAsStream("test.jpg")
  val bis = new BufferedInputStream(imageResource)
  val imageBytes = try {
    Stream.continually(bis.read).takeWhile(-1 !=).map(_.toByte).toArray
  } finally {
    bis.close()
  }
  val image = new String(Base64.getEncoder.encode(imageBytes), "utf-8")

  val connectionFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] =
    Http().outgoingConnection(host, port)
  val request =
    s"""
       |{
       |  "image": "$image",
       |  "params": [
       |    {
       |      "name": "scale",
       |      "width": 200,
       |      "height": 200,
       |      "preserveRatio": true
       |    },
       |    {
       |      "name": "rotate",
       |      "direction": "left"
       |    },
       |    {
       |      "name": "contrast",
       |      "contrast": 0.5
       |    },
       |    {
       |      "name": "sparkle"
       |    },
       |    {
       |      "name": "twirl",
       |      "radius": 20
       |    }
       |  ]
       |}
    """.stripMargin
  val responseFuture = Source.single(HttpRequest(
    method = HttpMethods.POST,
    entity = HttpEntity(
      ContentTypes.`application/json`,
      request
    ),
    uri = "/images"
  ))
    .via(connectionFlow)
    .runWith(Sink.head)
  responseFuture map { response =>
    system.log.info(s"Got response: $response")
    system.terminate()
  } recover {
    case e: Throwable =>
      system.log.error(e, "Failed to connect")
      system.terminate()
  }
}