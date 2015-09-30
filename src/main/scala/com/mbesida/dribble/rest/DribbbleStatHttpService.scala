package com.mbesida.dribble.rest

import com.mbesida.dribble.model.DribbbleJsonProtocol
import com.mbesida.dribble.service.DribbbleService
import spray.http.MediaTypes
import spray.httpx.SprayJsonSupport
import spray.json._
import spray.routing.{HttpService, HttpServiceActor, Route}

class DribbbleStatHttpServiceActor(override val service: DribbbleService) extends HttpServiceActor
with DribbbleStatHttpService
with DribbbleJsonProtocol
with SprayJsonSupport {

  override def receive: Receive = runRoute(route)

}

trait DribbbleStatHttpService extends HttpService
with DribbbleJsonProtocol
with SprayJsonSupport {

  implicit val ex = actorRefFactory.dispatcher

  def service: DribbbleService

  val route: Route = {
    path("top10") {
      get {
        parameter('login.as[String]) { login =>
          println(s"Starting handling $login")
          respondWithMediaType(MediaTypes.`application/json`) {
            complete {
              service.top10Likers(login).map { list =>
                println(list)
                list.toJson.toString
              }.recover {
                case e =>
                  println(e)
                  e.getMessage
              }
            }
          }
        }
      }
    }
  }
}



