package com.mbesida.dribble.service

import akka.actor.{Props, ActorSystem, Actor}
import akka.util.Timeout
import com.mbesida.dribble.model.User
import com.mbesida.dribble.service.DribbleServiceSpec.StubActor
import com.typesafe.config.ConfigFactory
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import spray.http.{Uri, HttpHeaders, StatusCodes}
import com.mbesida.dribble._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

@RunWith(classOf[JUnitRunner])
class DribbleServiceSpec extends Specification {

  implicit val conf = ConfigFactory.load()
  val system = ActorSystem("test")

  import system.dispatcher

  val service = new DribbbleService(system.actorOf(Props(new StubActor)))

  "DribbleService" should {
    "calculate top10 likers" in {
      implicit val t: Timeout = Duration(5, "seconds")
      val res = Await.result(service.top10Likers("aaa"), t.duration)
      res.size === 1
      res.head === User(152575, "charlie kubal", "ckubal", 2)
    }
  }

}

object DribbleServiceSpec {
  import ExtractorsSpec._

  class StubActor extends Actor {
    override def receive: Receive = {
      case "https://api.dribbble.com/v1/users/aaa/followers?page=1&per_page=100" => {
        val response = templateResponse(StatusCodes.OK, resourceAsString("followersTwo.json"))
        sender() ! response.copy(headers = response.headers :+
          HttpHeaders.Link(Uri("https://api.dribbble.com/v1/users/aaa/followers?page=2"), HttpHeaders.Link.next))
      }
      case "https://api.dribbble.com/v1/users/aaa/followers?page=2&per_page=100" =>
        sender() ! templateResponse(StatusCodes.OK, "[]")
      case "https://api.dribbble.com/v1/users/960932/shots" =>
        sender() ! templateResponse(StatusCodes.OK, resourceAsString("shots.json"))
      case "https://api.dribbble.com/v1/users/960926/shots" =>
        sender() ! templateResponse(StatusCodes.OK, "[]")
      case "https://api.dribbble.com/v1/shots/1925452/likes" =>
        sender() ! templateResponse(StatusCodes.OK, resourceAsString("likers.json"))
      case "https://api.dribbble.com/v1/shots/1921159/likes" =>
        sender() ! templateResponse(StatusCodes.OK, resourceAsString("likers.json"))
      case a => println(a)
    }
  }
}
