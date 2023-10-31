package ru.kimiega.urlshortener

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.{Directives, Route}
import com.typesafe.config.ConfigFactory
import ru.kimiega.urlshortener.configuration.{BasicAuthConfig, PostgresConfig}
import ru.kimiega.urlshortener.routes.{UrlRoutes, UserRoutes}
import ru.kimiega.urlshortener.security.Authenticator
import ru.kimiega.urlshortener.services.{UrlService, UserService}
import ru.kimiega.urlshortener.utils.RepositoryTransactor

import scala.util.Failure
import scala.util.Success

object MainApp {
  private def startHttpServer(routes: Route)(implicit system: ActorSystem[_]): Unit = {
    import system.executionContext

    val futureBinding = Http().newServerAt("localhost", 8080).bind(routes)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load("application.conf")
    val pgConfig = PostgresConfig(config)
    val authConfig = BasicAuthConfig(config)
    val authenticator = Authenticator(authConfig, pgConfig, RepositoryTransactor(pgConfig))
    val rootBehavior = Behaviors.setup[Nothing] { context =>
      val userRegistryActor = context.spawn(UserService(pgConfig), "UserRegistryActor")
      val urlRepositoryActor = context.spawn(UrlService(pgConfig), "UrlRepositoryActor")
      context.watch(userRegistryActor)
      context.watch(urlRepositoryActor)
      val userRoutes = new UserRoutes(userRegistryActor, authenticator)(context.system)
      val urlRoutes = new UrlRoutes(urlRepositoryActor, authenticator)(context.system)
      startHttpServer(Directives.concat(userRoutes.userRoutes, urlRoutes.userRoutes))(context.system)

      Behaviors.empty
    }
    val system = ActorSystem[Nothing](rootBehavior, "HelloAkkaHttpServer")
  }
}
