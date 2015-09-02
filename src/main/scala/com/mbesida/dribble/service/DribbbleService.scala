package com.mbesida.dribble.service

import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import akka.pattern._
import akka.util.Timeout
import com.mbesida.dribble.model.{DribbbleJsonProtocol, DribbbleUser, Shot, User}
import com.mbesida.dribble.service.Extractors._
import com.typesafe.config.Config
import spray.http.HttpResponse

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try

class DribbbleService(client: ActorRef)(implicit config: Config, exec: ExecutionContext) extends DribbbleJsonProtocol  {

  def top10Likers(username: String): Future[List[User]] = {
    val initialUrl = config.getString("app.followers-uri").format(username)

    for {
      followers <- followers(initialUrl)
      shotsOfFollowers <- Future.sequence(followers.map(follower =>shots(follower.shots_url)))
      likes <- Future.sequence(shotsOfFollowers.flatten.map(shot => likers(shot.likes_url)))
      allLikersMap = likes.flatten.groupBy(du => User(du.id, du.name, du.username, 0))
      sortedLikers = allLikersMap.mapValues(_.size).toList.sortWith((prev, next) => prev._2 > next._2)
    } yield sortedLikers.take(10).map{case (user, count) => user.copy(likes = count)}

  }

  def followers(url: String): Future[List[DribbbleUser]] = customFecth(url)(r => extractFollowers(r))

  def shots(url: String): Future[List[Shot]] = customFecth(url)(r => extractShots(r))

  def likers(url: String): Future[List[DribbbleUser]] = customFecth(url)(r => extractLikers(r))

  private def customFecth[T](url: String)(extraction: HttpResponse => Try[Extraction[T]]): Future[List[T]] = {

    implicit val timeout: Timeout = config.getDuration("app.query-timeout", TimeUnit.MILLISECONDS)

    def rec(next: Option[String], agg: Future[List[T]]): Future[List[T]] = {
      next match {
        case None => agg
        case Some(u) => for {
          httpResp <- (client ? u).mapTo[HttpResponse]
          (data, nextPage) <- Promise[Extraction[T]].complete(extraction(httpResp)).future
          result <- rec(nextPage, agg.map(_ ++ data))
        } yield result
      }
    }

    rec(Some(url), Future(List.empty))
  }

}
