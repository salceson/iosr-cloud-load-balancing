package iosr.filters

import akka.actor.Actor
import com.sksamuel.scrimage.Image
import com.sksamuel.scrimage.filter.{TwirlFilter => ScrimageTwirlFilter}
import iosr.Messages.{FilterDone, TwirlCommand}

class TwirlFilter extends Actor {
  override def receive: Receive = {
    case TwirlCommand(imageBytes, params) =>
      val senderActor = sender()
      val image = Image(imageBytes)
      senderActor ! FilterDone(
        image.filter(
          ScrimageTwirlFilter(params.radius)
        ).bytes
      )
  }
}
