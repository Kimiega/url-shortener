package ru.kimiega.urlshortener.security

import akka.http.scaladsl.server.directives.Credentials
import ru.kimiega.urlshortener.configuration.BasicAuthConfig
import ru.kimiega.urlshortener.repository.UserRegistry
import ru.kimiega.urlshortener.utils.Hasher

class Authenticator(auth: BasicAuthConfig, userReg: UserRegistry) {

  def authAdmin(credentials: Credentials): Option[String] = {
    credentials match {
      case p @ Credentials.Provided(id) if id == auth.user && p.verify(auth.password) => Some(id)
      case _ => None
    }
  }

  def authUser(credentials: Credentials): Option[String] = {
    credentials match {
      case p @ Credentials.Provided(id) if {
        val user = userReg.dbGetUser(id)
        user.isDefined && p.verify(user.get.password, Hasher.hashPassword)
      } => Some(id)
      case _ => None
    }
  }
}

