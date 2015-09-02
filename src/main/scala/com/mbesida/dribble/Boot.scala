package com.mbesida.dribble

import akka.actor.{ActorSystem, Props}
import akka.contrib.throttle.Throttler.{SetTarget, _}
import akka.contrib.throttle.TimerBasedThrottler
import akka.io.IO
import com.mbesida.dribble.rest.DribbbleStatHttpService
import com.mbesida.dribble.service.{DribbbleClient, DribbbleService}
import com.typesafe.config.ConfigFactory
import spray.can.Http

object Boot extends App {

  implicit val config = ConfigFactory.load()
  implicit val actorSystem = ActorSystem("dribbble-stats")
  import actorSystem.dispatcher

  val throttler = actorSystem.actorOf(Props(new TimerBasedThrottler(config.getInt("app.rate-limit").msgsPerMinute)), "throttler")
  val worker = actorSystem.actorOf(Props(new DribbbleClient()), "client")

  throttler ! SetTarget(Some(worker))

  val service = new DribbbleService(throttler)

  val endpoint = actorSystem.actorOf(Props(new DribbbleStatHttpService(service)))

  IO(Http) ! Http.Bind(endpoint, config.getString("app.uri"), config.getInt("app.port"))
}
