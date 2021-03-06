package iosr.filters

import akka.actor.Actor
import com.sksamuel.scrimage.Image
import com.sksamuel.scrimage.filter.{SparkleFilter => ScrimageSparkleFilter}
import iosr.Messages.{Response, SparkleCommand}

class SparkleFilter extends Actor {
  override def receive: Receive = {
    case SparkleCommand(imageBytes, params) =>
      val senderActor = sender()
      val image = Image(imageBytes)
      senderActor ! Response(
        image.filter(ScrimageSparkleFilter(
          rays = params.rays,
          radius = params.radius,
          amount = params.amount
        )).bytes
      )
  }
}
