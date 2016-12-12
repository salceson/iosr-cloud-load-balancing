package iosr.filters

import akka.actor.Actor
import com.sksamuel.scrimage.Image
import com.sksamuel.scrimage.filter.{ SparkleFilter => ScrimageSparkleFilter }
import iosr.filters.Messages.{ DoneMessage, SparkleMessage }

class SparkleFilter extends Actor {
  override def receive: Receive = {
    case SparkleMessage(imageBytes, params) =>
      val senderActor = sender()
      val image = Image(imageBytes)
      senderActor ! DoneMessage(
        image.filter(ScrimageSparkleFilter(
          rays = params.rays,
          radius = params.radius,
          amount = params.amount
        )).bytes
      )
  }
}
