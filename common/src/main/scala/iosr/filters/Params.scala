package iosr.filters

sealed trait RotationDirection

case object RotateLeft extends RotationDirection

case object RotateRight extends RotationDirection

sealed trait Params

case class ScaleParams(width: Int, height: Int, preserveRatio: Boolean) extends Params

case class TwirlParams(radius: Int) extends Params

case class SparkleParams(rays: Int = 50,
                         radius: Int = 25,
                         amount: Int = 50)
  extends Params

case class RotateParams(direction: RotationDirection) extends Params

case class ContrastParams(contrast: Double) extends Params
