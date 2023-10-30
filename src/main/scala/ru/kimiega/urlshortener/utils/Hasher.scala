package ru.kimiega.urlshortener.utils

import com.roundeights.hasher.Implicits._
import com.typesafe.config.ConfigFactory

import scala.language.postfixOps

object Hasher {
  private val salt = ConfigFactory.load("application.conf").getString("app.security.salt")
  def apply(string: String): String = string.salt(salt).sha256.hex
}
