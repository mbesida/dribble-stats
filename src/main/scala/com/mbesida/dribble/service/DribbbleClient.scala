package com.mbesida.dribble.service

import akka.actor.Actor
import akka.pattern._
import com.typesafe.config.Config
import spray.client.pipelining._
import spray.http._

import scala.concurrent.Future

class DribbbleClient(implicit config: Config) extends Actor {

  import context.dispatcher

  val pipeline: HttpRequest => Future[HttpResponse] = {
    addCredentials(OAuth2BearerToken(config.getString("app.auth.client-access-token"))) ~> sendReceive
  }

  override def receive: Receive = {
    case uri: String =>
      println(s"Going to query $uri")
      val origin = sender()
      pipeline(Get(uri)) pipeTo origin
  }

}

