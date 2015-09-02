package com.mbesida.dribble

import spray.json.DefaultJsonProtocol

package object model {

  case class DribbbleUser(id: Long, name: String, username: String, shots_url: String)

  case class Shot(id: Long, title: String, likes_url: String)

  case class User(id: Long, name: String, username: String, likes: Int)

  trait DribbbleJsonProtocol extends DefaultJsonProtocol {
    implicit val dribbbleUserFormat = jsonFormat4(DribbbleUser)
    implicit val shotFormat = jsonFormat3(Shot)
    implicit val userFormat = jsonFormat4(User)

  }

  object DribbbleJsonProtocol extends DribbbleJsonProtocol
}
