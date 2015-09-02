package com.mbesida.dribble.service

import com.mbesida.dribble.model.{DribbbleUser, Shot}
import spray.http.HttpHeaders.Link
import spray.http.{Uri, HttpResponse}
import spray.json._

import scala.util.{Failure, Try}


object Extractors {

  type Extraction[T] = (Vector[T], Option[String])

  def extractFollowers(resp: HttpResponse)
                      (implicit jsonReader: JsonReader[DribbbleUser]): Try[Extraction[DribbbleUser]] = {
    extractData(resp)(js => js.asJsObject.fields("follower").convertTo[DribbbleUser])
  }

  def extractShots(resp: HttpResponse)
                  (implicit jsonReader: JsonReader[Shot]): Try[Extraction[Shot]] = {
    extractData(resp)(js => js.convertTo[Shot])
  }

  def extractLikers(resp: HttpResponse)
                   (implicit jsonReader: JsonReader[DribbbleUser]): Try[Extraction[DribbbleUser]] = {
    extractData(resp)(js => js.asJsObject.fields("user").convertTo[DribbbleUser])
  }


  private def extractData[T: JsonReader](resp: HttpResponse)(extractor: JsValue => T): Try[Extraction[T]] = {
    if (resp.status.isSuccess) {
      resp.entity.asString.parseJson match {
        case JsArray(values) =>
          Try((values.map(js => extractor(js))), findNextPageUrl(resp)).recoverWith {
            case e => Failure(new RuntimeException( s"""{"message":${e.getMessage}"""))
          }
        case _ => failureResponse(resp)
      }
    } else failureResponse(resp)
  }

  private def failureResponse(resp: HttpResponse): Try[Nothing] = {
    Failure(new RuntimeException(resp.entity.asString))
  }

  def findNextPageUrl(resp: HttpResponse): Option[String] = for {
    links <- resp.headers.collect { case Link(linkValues) => linkValues.filter(_.params.contains(Link.next))}.headOption
    next <- links.headOption
  } yield {
      val nextUri = next.uri.copy(query = Uri.Query(next.uri.query.toMap + ("per_page" -> "100")))
      nextUri.toString()
    }

}
