package iosr.frontend.utils

import akka.actor.ActorSystem
import com.typesafe.config.Config
import iosr.frontend.resources.ImageResource
import iosr.frontend.services.{ImageService, ImageServiceImpl}

trait Resources extends ImageResource {
  def config: Config

  def system: ActorSystem

  override val imageService: ImageService = new ImageServiceImpl(system, config)
}
