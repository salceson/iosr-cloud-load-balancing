package iosr.filters

import akka.actor.Actor
import com.sksamuel.scrimage.Image
import iosr.filters.Messages.{Response, RotateCommand}

class RotateFilter extends Actor {
  override def receive: Receive = {
    case RotateCommand(imageBytes, params) =>
      val senderActor = sender()
      val image = Image(imageBytes)
      val rotated = params.direction match {
        case RotateLeft => image.rotateLeft
        case RotateRight => image.rotateRight
      }
      senderActor ! Response(rotated.bytes)
  }
}
