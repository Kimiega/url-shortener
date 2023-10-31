package ru.kimiega.urlshortener.utils

import com.roundeights.hasher.Implicits._
import com.typesafe.config.ConfigFactory

import scala.language.postfixOps

object Hasher {
  private val salt = ConfigFactory.load("application.conf").getString("app.security.salt")
  def hashPassword(password: String): String = password.salt(salt).sha256.hex

  def hashUrl(url: String, countTry: Int): String = url.salt(countTry.toString).sha256.hex
}
