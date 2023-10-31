package ru.kimiega.urlshortener.security

import akka.http.scaladsl.server.directives.Credentials
import ru.kimiega.urlshortener.configuration.{BasicAuthConfig, PostgresConfig}
import ru.kimiega.urlshortener.repository.UserRegistry
import ru.kimiega.urlshortener.utils.Hasher
import ru.kimiega.urlshortener.utils.RepositoryTransactor.Transactor

case class Authenticator(auth: BasicAuthConfig, pg : PostgresConfig, xa: Transactor) {

  def apply(credentials: Credentials): Option[String] = {
    credentials match {
      case p @ Credentials.Provided(id) if id == auth.user && p.verify(auth.password) => Some(id)
      case p @ Credentials.Provided(id) if {
        val user = UserRegistry.dbGetUser(xa, id)
        user.isDefined && p.verify(user.get.password, Hasher.hashPassword)
      } => Some(id)
      case _ => None
    }
  }
}

