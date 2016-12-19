package iosr.frontend.models

import iosr.filters.Params

case class ImageProcessingRequest(image: String,
                                  operationsParams: List[Params])

case class ImageProcessingCreated(id: String)

case class ImageProcessedResponse(image: String)
