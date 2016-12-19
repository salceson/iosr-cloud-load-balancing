package iosr.filters

import akka.actor.Actor
import com.sksamuel.scrimage.Image
import com.sksamuel.scrimage.filter.{SparkleFilter => ScrimageSparkleFilter}
import iosr.Messages.{FilterDone, SparkleCommand}

class SparkleFilter extends Actor {
  override def receive: Receive = {
    case SparkleCommand(imageBytes, params) =>
      val senderActor = sender()
      val image = Image(imageBytes)
      senderActor ! FilterDone(
        image.filter(ScrimageSparkleFilter(
          rays = params.rays,
          radius = params.radius,
          amount = params.amount
        )).bytes
      )
  }
}
