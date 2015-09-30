package com.mbesida.dribble.rest

import akka.actor.ActorRefFactory
import com.mbesida.dribble.model._
import com.mbesida.dribble.service.DribbbleService
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import spray.http.ContentTypes
import spray.json._
import spray.testkit.Specs2RouteTest

import scala.concurrent.{ExecutionContext, Future}


class DribbbleStatHttpServiceSpec extends Specification with Specs2RouteTest with Mockito with DribbbleJsonProtocol {

  "DribbleStatService" should {
    "return result as application/json" in {
      val mockService: DribbbleService = mock[DribbbleService]

      val service = new DribbbleStatHttpService {
        override def service: DribbbleService = mockService
        override implicit def actorRefFactory: ActorRefFactory = system
      }

      mockService.top10Likers("user") returns Future.successful(List(
        User(1, "name1", "surname1", 20),
        User(2, "name2", "surname2", 10),
        User(3, "name3", "surname3", 5)
      ))

      Get("/top10?login=user") ~> service.route ~> check {
        contentType === ContentTypes.`application/json`
        val users = responseAs[String].parseJson.convertTo[List[User]]
        users.size === 3
        users.head === User(1, "name1", "surname1", 20)
      }
    }

    "return error message in case of errors as application/json" in {
      val mockService: DribbbleService = mock[DribbbleService]

      val service = new DribbbleStatHttpService {
        override def service: DribbbleService = mockService
        override implicit def actorRefFactory: ActorRefFactory = system
      }

      mockService.top10Likers("user") returns Future.failed(new RuntimeException("Some error happened"))
      Get("/top10?login=user") ~> service.route ~> check {
        contentType === ContentTypes.`application/json`
        responseAs[String] === "Some error happened"
      }
    }
  }


}
