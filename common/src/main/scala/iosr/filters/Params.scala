package iosr.filters

sealed trait Params {
  def filterName: String
}

case class ScaleParams(width: Int, height: Int, preserveRatio: Boolean) extends Params {
  override def filterName = "scale"
}

case class TwirlParams(radius: Int) extends Params {
  override def filterName: String = "twirl"
}

case class SparkleParams(rays: Int = 50,
                         radius: Int = 25,
                         amount: Int = 50) extends Params {
  override def filterName: String = "sparkle"
}
