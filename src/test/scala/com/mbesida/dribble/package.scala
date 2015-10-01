package com.mbesida

import scala.io.Source


package object dribble {
  def resourceAsString(resourcePath: String): String = {
    val resource = getClass.getResourceAsStream(s"/$resourcePath")
    val source = Source.fromInputStream(resource)
    source.mkString
  }
}
