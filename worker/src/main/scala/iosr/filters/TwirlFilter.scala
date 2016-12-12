package iosr.filters

import akka.actor.Actor
import com.sksamuel.scrimage.Image
import com.sksamuel.scrimage.filter.{ TwirlFilter => ScrimageTwirlFilter }
import iosr.filters.Messages.{ DoneMessage, TwirlMessage }

class TwirlFilter extends Actor {
  override def receive: Receive = {
    case TwirlMessage(imageBytes, params) =>
      val senderActor = sender()
      val image = Image(imageBytes)
      senderActor ! DoneMessage(
        image.filter(
          ScrimageTwirlFilter(params.radius)
        ).bytes
      )
  }
}
