package ru.kimiega.urlshortener.utils

import ru.kimiega.urlshortener.dtos.{ActionPerformed, Url, User, Users}
import spray.json.{DefaultJsonProtocol, JsString, JsValue, JsonFormat, deserializationError}

import java.time.LocalDateTime
import scala.util.{Failure, Success, Try}

object JsonFormats {
  import DefaultJsonProtocol._
  implicit val localDateTimeFormat: JsonFormat[LocalDateTime] =
    new JsonFormat[LocalDateTime] {
      override def write(obj: LocalDateTime): JsValue = JsString(obj.toString)

      override def read(json: JsValue): LocalDateTime = json match {
        case JsString(s) => Try(LocalDateTime.parse(s)) match {
          case Success(result) => result
          case Failure(exception) =>
            deserializationError(s"could not parse $s as LocalDateTime", exception)
        }
        case notAJsString =>
          deserializationError(s"expected a String but got a $notAJsString")
      }
    }
  implicit val userJsonFormat = jsonFormat2(User)
  implicit val usersJsonFormat = jsonFormat1(Users)

  implicit val urlJsonFormat = jsonFormat4(Url)

  implicit val actionPerformedJsonFormat = jsonFormat1(ActionPerformed)
}
