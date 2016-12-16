package iosr.filters

import akka.actor.Actor
import com.sksamuel.scrimage.Image
import com.sksamuel.scrimage.filter.{ContrastFilter => ScrimageContrastFilter}
import iosr.Messages.{ContrastCommand, Response}

class ContrastFilter extends Actor {
  override def receive: Receive = {
    case ContrastCommand(imageBytes, params) =>
      val senderActor = sender()
      val image = Image(imageBytes)
      senderActor ! Response(
        image.filter(ScrimageContrastFilter(params.contrast)).bytes
      )
  }
}
