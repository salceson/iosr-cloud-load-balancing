package iosr.frontend.resources

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import iosr.frontend.models.{ImageProcessedResponse, ImageProcessingCreated, ImageProcessingRequest}
import iosr.frontend.services.ImageService
import iosr.frontend.utils.Resource

import scala.concurrent.Future

trait ImageResource extends Resource {
  def imageService: ImageService

  def routes: Route = pathPrefix("images") {
    pathEnd {
      post {
        entity(as[ImageProcessingRequest]) { request =>
          created(ImageProcessingCreated(
            imageService.sendImage(request)
          ))
        }
      }
    } ~
      path(Segment) { id =>
        get {
          onSuccess(Future.successful(imageService.getImage(id))) {
            case Some(image) => complete(ImageProcessedResponse(image))
            case None => complete(StatusCodes.NotFound)
          }
        }
      }
  }
}
