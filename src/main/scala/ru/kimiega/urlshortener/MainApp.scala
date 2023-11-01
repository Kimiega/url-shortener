package ru.kimiega.urlshortener

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.{Directives, Route}
import com.typesafe.config.ConfigFactory
import ru.kimiega.urlshortener.configuration.{BasicAuthConfig, PostgresConfig}
import ru.kimiega.urlshortener.repository.{UrlRepository, UserRegistry}
import ru.kimiega.urlshortener.routes.{UrlRoutes, UserRoutes}
import ru.kimiega.urlshortener.security.Authenticator
import ru.kimiega.urlshortener.services.{UrlService, UserService}
import ru.kimiega.urlshortener.utils.RepositoryTransactor

import scala.util.Failure
import scala.util.Success

object MainApp {
  private def startHttpServer(routes: Route, host: String, port: Int)(implicit system: ActorSystem[_]): Unit = {
    import system.executionContext

    val futureBinding = Http().newServerAt(host, port).bind(routes)
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
    val xa = RepositoryTransactor(pgConfig)
    val urlRepository = new UrlRepository(xa)
    val userRegistry = new UserRegistry(xa)
    val authenticator = new Authenticator(authConfig, userRegistry)
    val host = config.getString("app.server.host")
    val port = config.getInt("app.server.port")

    val rootBehavior = Behaviors.setup[Nothing] { context =>
      val userRegistryActor = context.spawn(UserService(userRegistry), "UserRegistryActor")
      val urlRepositoryActor = context.spawn(UrlService(urlRepository, userRegistry), "UrlRepositoryActor")

      context.watch(userRegistryActor)
      context.watch(urlRepositoryActor)

      val userRoutes = new UserRoutes(userRegistryActor, authenticator)(context.system)
      val urlRoutes = new UrlRoutes(urlRepositoryActor, authenticator)(context.system)
      startHttpServer(Directives.concat(userRoutes.userRoutes, urlRoutes.userRoutes), host, port)(context.system)

      Behaviors.empty
    }

    val system = ActorSystem[Nothing](rootBehavior, "HelloAkkaHttpServer")
  }
}
