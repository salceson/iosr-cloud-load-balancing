package iosr.frontend.utils

import akka.http.scaladsl.marshalling.{ToResponseMarshallable, ToResponseMarshaller}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, Route}

trait Resource extends Directives with JsonSupport {
  def created[T: ToResponseMarshaller](response: T): Route = {
    complete(StatusCodes.Created, ToResponseMarshallable(response))
  }
}
