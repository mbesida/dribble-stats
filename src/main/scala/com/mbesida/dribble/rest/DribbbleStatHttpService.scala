package com.mbesida.dribble.rest

import com.mbesida.dribble.model.DribbbleJsonProtocol
import com.mbesida.dribble.service.DribbbleService
import spray.http.{MediaTypes, MediaType}
import spray.httpx.SprayJsonSupport
import spray.json._
import spray.routing.HttpServiceActor

class DribbbleStatHttpService(service: DribbbleService) extends HttpServiceActor
  with DribbbleJsonProtocol
  with SprayJsonSupport {

  import context.dispatcher

  override def receive: Receive = runRoute {
    path("top10") {
      get {
        parameter('login.as[String]) { login =>
          println(s"Starting handling $login")
          respondWithMediaType(MediaTypes.`application/json`) {
            complete {
              service.top10Likers(login).map{ list =>
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



