package iosr.frontend.utils

import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import iosr.filters._
import org.json4s._
import org.json4s.{DefaultFormats, Formats, JValue, Serializer, TypeInfo}
import org.json4s.native.Serialization

trait JsonSupport extends Json4sSupport {
  implicit val serialization = Serialization

  implicit def json4sFormats: Formats = DefaultFormats ++ CustomSerializers.all
}

case object ParamsSerializer extends Serializer[Params] {
  private val ParamsClass = classOf[Params]

  override def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), Params] = {
    case (TypeInfo(ParamsClass, _), json) =>
      json \ "name" match {
        case JString("contrast") =>
          json \ "contrast" match {
            case JDouble(contrast) => ContrastParams(contrast)
            case _ => throw new MappingException("Missing params")
          }
        case JString("rotate") =>
          json \ "direction" match {
            case JString("left") => RotateParams(RotateLeft)
            case JString("right") => RotateParams(RotateRight)
            case JString(_) => throw new MappingException("Wrong rotation direction")
            case _ => throw new MappingException("Missing params")
          }
        case JString("scale") =>
          (json \ "width", json \ "height", json \ "preserveRatio") match {
            case (JInt(width), JInt(height), JBool(preserveRatio)) =>
              ScaleParams(width.toInt, height.toInt, preserveRatio)
            case _ => throw new MappingException("Missing params or incompatible types")
          }
        case JString("sparkle") =>
          val rays = json \ "rays" match {
            case JInt(value) => Some(value.toInt)
            case _ => None
          }
          val radius = json \ "radius" match {
            case JInt(value) => Some(value.toInt)
            case _ => None
          }
          val amount = json \ "amount" match {
            case JInt(value) => Some(value.toInt)
            case _ => None
          }
          SparkleParams(rays, radius, amount)
        case JString("twirl") =>
          json \ "radius" match {
            case JInt(radius) =>
              TwirlParams(radius.toInt)
            case _ =>
              throw new MappingException("Missing params")
          }
        case x => throw new MappingException(s"Unknown name of filter: $x")
      }
    case x => throw new MappingException(s"Can't convert $x into filter params")
  }

  override def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
    case ContrastParams(contrast) => JObject(
      "name" -> JString("contrast"),
      "contrast" -> JDouble(contrast)
    )
    case RotateParams(direction) =>
      val directionName = direction match {
        case RotateLeft => "left"
        case RotateRight => "right"
      }
      JObject(
        "name" -> JString("rotate"),
        "direction" -> JString(directionName)
      )
    case ScaleParams(width, height, preserveRatio) => JObject(
      "name" -> JString("scale"),
      "width" -> JInt(width),
      "height" -> JInt(height),
      "preserveRatio" -> JBool(preserveRatio)
    )
    case SparkleParams(rays, radius, amount) => JObject(
      "name" -> JString("sparkle"),
      "rays" -> JInt(rays),
      "radius" -> JInt(radius),
      "amount" -> JInt(amount)
    )
    case TwirlParams(radius) => JObject(
      "name" -> JString("twirl"),
      "radius" -> JInt(radius)
    )
  }
}

object CustomSerializers {
  val all = List(ParamsSerializer)
}
