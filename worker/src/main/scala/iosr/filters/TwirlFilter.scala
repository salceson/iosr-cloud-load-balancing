package iosr.filters

import akka.actor.Actor
import com.sksamuel.scrimage.Image
import com.sksamuel.scrimage.filter.{ TwirlFilter => ScrimageTwirlFilter }
import iosr.filters.Messages.{ Response, TwirlCommand }

class TwirlFilter extends Actor {
  override def receive: Receive = {
    case TwirlCommand(imageBytes, params) =>
      val senderActor = sender()
      val image = Image(imageBytes)
      senderActor ! Response(
        image.filter(
          ScrimageTwirlFilter(params.radius)
        ).bytes
      )
  }
}
