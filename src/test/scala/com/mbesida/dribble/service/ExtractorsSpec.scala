package com.mbesida.dribble.service

import com.mbesida.dribble._
import com.mbesida.dribble.model.{DribbbleJsonProtocol, DribbbleUser, Shot}
import com.mbesida.dribble.service.Extractors._
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import spray.http._

import scala.util.{Failure, Try}

@RunWith(classOf[JUnitRunner])
class ExtractorsSpec extends Specification with DribbbleJsonProtocol {

  import ExtractorsSpec._

  "Extractors" should {
    "extract correct" in {

      "single paged followers data" in {
        singlePagedTemplate[DribbbleUser](extractFollowers, "followers.json", 3, _.name, "Xiyuan Gou")
      }

      "multi paged followers data" in {
        multiPagedTemplate(extractFollowers, "followers.json", "https://someurl/followers", 3)
      }
      "single paged shots data" in {
        singlePagedTemplate[Shot](extractShots, "shots.json", 2, _.title, "Chronicled")
      }
      "multi paged shots data" in {
        multiPagedTemplate(extractShots, "shots.json", "https://someurl/shots", 2)
      }
      "single paged likers data" in {
        singlePagedTemplate[DribbbleUser](extractLikers, "likers.json", 1, _.name, "charlie kubal")
      }
      "multi paged shots data" in {
        multiPagedTemplate(extractLikers, "likers.json", "https://someurl/likes", 1)
      }
    }
    "extract bad response" in {
      val res = extractFollowers(templateResponse(StatusCodes.BadGateway, "Something bad happened"))
      res must beFailedTry
      val Failure(ex: RuntimeException) = res
      ex.getMessage === "Something bad happened"

    }
    "return failed result if invalid json api occured" in {
      val res = extractFollowers(templateResponse(StatusCodes.OK, resourceAsString("buggyContract1.json")))
      res must beFailedTry
      val Failure(ex: RuntimeException) = res
      ex.getMessage === "Response doesn't conform to js array"
    }
    "return failed result if required section is absent" in {
      val res = extractFollowers(templateResponse(StatusCodes.OK, resourceAsString("buggyContract2.json")))
      res must beFailedTry
    }
  }

  def singlePagedTemplate[T](func: HttpResponse => Try[Extraction[T]], path: String,
                             count: Int, projectFunc: T => String, expected: String) =  {
    val res = func(templateResponse(StatusCodes.OK, resourceAsString(path)))
    res must beSuccessfulTry
    val (data, nextPage) = res.get
    nextPage === None
    data.size === count
    data.map(projectFunc) must contain(expected)
  }

  def multiPagedTemplate[T](func: HttpResponse => Try[Extraction[T]], path: String, nextPageLink: String, count: Int) = {
    val response = templateResponse(StatusCodes.OK, resourceAsString(path))
    val res = func(response.copy(headers = response.headers :+ HttpHeaders.Link(Uri(nextPageLink), HttpHeaders.Link.next)))
    res must beSuccessfulTry
    val (data, nextPage) = res.get
    data.size === count
    nextPage === Some(s"${nextPageLink}?per_page=100")
  }
}

object ExtractorsSpec {
  def templateResponse(code: StatusCode, data: String): HttpResponse = {
    HttpResponse(code, HttpEntity(ContentTypes.`application/json`, data))
  }


}
