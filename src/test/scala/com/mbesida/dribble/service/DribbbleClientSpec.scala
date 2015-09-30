package com.mbesida.dribble.service

import akka.actor.{ActorSystem, Props}
import akka.pattern._
import akka.util.Timeout
import com.netaporter.precanned.dsl.basic._
import com.typesafe.config.ConfigFactory
import org.specs2.mutable.Specification
import spray.http.{HttpHeaders, HttpResponse, OAuth2BearerToken, StatusCodes}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.Source

class DribbbleClientSpec extends Specification{

  implicit val config = ConfigFactory.load()
  implicit val system = ActorSystem("dribble-client-system")

  val dribbbleApi = httpServerMock(system).bind(9000).block

  dribbbleApi.expect(get, pathStartsWith("/normal"),
    header(HttpHeaders.Authorization(OAuth2BearerToken(config.getString("app.auth.client-access-token")))))
    .andRespondWith(resource("/normal_response.json"))

  dribbbleApi.expect(get, pathStartsWith("/badCred"))
    .andRespondWith(resource("/badCred.json") compose status(StatusCodes.Unauthorized))

  dribbbleApi.expect(get, pathStartsWith("/bad")).andRespondWith(status(StatusCodes.InternalServerError))

  def readResponse(s: String): String = {
    val resource = getClass.getResourceAsStream(s)
    val source = Source.fromInputStream(resource)
    source.mkString
  }

  "Dribble client" should {

    val client = system.actorOf(Props(new DribbbleClient))
    implicit val t: Timeout = Duration(3, "seconds")

    "respond with normal response" in {
      val response = Await.result((client ? "http://127.0.0.1:9000/normal").mapTo[HttpResponse], t.duration)
      response.status mustEqual StatusCodes.OK
      response.entity.asString mustEqual readResponse("/normal_response.json")
    }

    "respond with bad response" in {
      val response = Await.result((client ? "http://127.0.0.1:9000/bad").mapTo[HttpResponse], t.duration)
      response.status mustEqual StatusCodes.InternalServerError
    }

    "respond with bad credentials response" in {
      val response = Await.result((client ? "http://127.0.0.1:9000/badCred").mapTo[HttpResponse], t.duration)
      response.status mustEqual StatusCodes.Unauthorized
      response.entity.asString mustEqual readResponse("/badCred.json")
    }
  }

}
