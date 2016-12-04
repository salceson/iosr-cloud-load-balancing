package iosr.filters.scalefilter

import java.io.File

import akka.actor.Actor
import com.sksamuel.scrimage.Image

class ScaleFilter extends Actor {

  import ScaleFilter.ScaleCommand
  import iosr.filters.common.Done

  override def receive: Receive = {
    case ScaleCommand(from, to, maxWidth, maxHeight, preserveRatio) =>
      val senderActor = sender()
      val image = Image.fromFile(from)
      val scaledImage = if (preserveRatio) {
        val ratio = image.width.asInstanceOf[Double] / image.height.asInstanceOf[Double]
        if (ratio > 1.0d) {
          image.scaleTo(maxWidth, (maxWidth / ratio).asInstanceOf[Int])
        } else {
          image.scaleTo((maxHeight * ratio).asInstanceOf[Int], maxHeight)
        }
      } else {
        image.scaleTo(maxWidth, maxHeight)
      }
      scaledImage.output(to)
      senderActor ! Done
  }

  //  private def
}

object ScaleFilter {

  case class ScaleCommand(fromFile: File, toFile: File, maxWidth: Int, maxHeight: Int, preserveRatio: Boolean)

}
