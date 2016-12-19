package iosr.frontend.services

import java.util.{Base64, UUID}

import akka.actor.{Actor, ActorPath, ActorSystem, Props}
import com.typesafe.config.Config
import iosr.Messages.{Request, Response}
import iosr.frontend.models.ImageProcessingRequest

import scala.collection.mutable

trait ImageService {
  def sendImage(request: ImageProcessingRequest): String

  def getImage(id: String): Option[String]
}

class ImageServiceImpl(system: ActorSystem, config: Config) extends ImageService {
  println(system, config)
  private val supervisorAddress = config.getString("supervisor.address")
  private val supervisorPath = ActorPath.fromString(s"akka.tcp://Supervisor@$supervisorAddress/user/supervisorActor")

  private val imageMap = mutable.Map[String, String]()

  private val actor = system.actorOf(ImageServiceActor.props(imageMap, supervisorPath))

  override def sendImage(request: ImageProcessingRequest): String = {
    val id = UUID.randomUUID().toString
    val decodedImage = Base64.getDecoder.decode(request.image.getBytes("utf-8"))
    actor ! Request(id, decodedImage, request.operationsParams)
    id
  }

  override def getImage(id: String): Option[String] = imageMap.get(id)
}

class ImageServiceActor(imageMap: mutable.Map[String, String],
                        supervisorPath: ActorPath)
  extends Actor {
  private val supervisor = context.actorSelection(supervisorPath)

  override def receive: Receive = {
    case Response(id, image) =>
      val base64EncodedImage = new String(Base64.getEncoder.encode(image), "utf-8")
      imageMap.put(id, base64EncodedImage)
    case request: Request =>
      supervisor ! request
  }
}

object ImageServiceActor {
  def props(imageMap: mutable.Map[String, String], supervisorPath: ActorPath): Props =
    Props(classOf[ImageServiceActor], imageMap, supervisorPath)
}
