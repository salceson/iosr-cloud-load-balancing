package iosr.filters

import akka.actor.Actor
import com.sksamuel.scrimage.Image
import iosr.filters.Messages.{ Response, ScaleCommand }

class ScaleFilter extends Actor {
  override def receive: Receive = {
    case ScaleCommand(imageBytes, params) =>
      val senderActor = sender()
      val image = Image(imageBytes)
      val scaledImage = if (params.preserveRatio) {
        val ratio = image.width.asInstanceOf[Double] / image.height.asInstanceOf[Double]
        if (ratio > 1.0d) {
          image.scaleTo(params.width, (params.width / ratio).asInstanceOf[Int])
        } else {
          image.scaleTo((params.height * ratio).asInstanceOf[Int], params.height)
        }
      } else {
        image.scaleTo(params.width, params.height)
      }
      senderActor ! Response(scaledImage.bytes)
  }
}
