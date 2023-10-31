package ru.kimiega.urlshortener.routes

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import ru.kimiega.urlshortener.dtos._
import ru.kimiega.urlshortener.security.Authenticator
import ru.kimiega.urlshortener.services.UrlService
import ru.kimiega.urlshortener.services.UrlService._

import scala.concurrent.Future

class UrlRoutes(urlService: ActorRef[UrlService.Command], authenticator: Authenticator)(implicit val system: ActorSystem[_]) {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import ru.kimiega.urlshortener.utils.JsonFormats._

  private implicit val timeout = Timeout.create(system.settings.config.getDuration("app.routes.ask-timeout"))

  def getFullUrl(shortUrl: String): Future[GetUrlResponse] =
    urlService.ask(GetUrl(shortUrl, _))

  def createUrl(url: FullUrl): Future[ActionPerformedCode] =
    urlService.ask(CreateUrl(url, _))

  def deleteUrl(shortUrl: String): Future[ActionPerformedCode] =
    urlService.ask(DeleteUrl(shortUrl, _))

  def getUserUrls(login: String): Future[Urls] =
    urlService.ask(GetUrls(login, _))

  //#all-routes
  //#users-get-post
  //#users-get-delete
  val userRoutes: Route =
  pathPrefix("u") {
    concat(
    authenticateBasic(realm = "secure   site", authenticator.apply) { login =>
      concat(
        //#users-get-delete
        pathEnd {
          concat(
            get {
             complete(getUserUrls(login))
            },
            post {
              entity(as[Link]) { link =>
                onSuccess(createUrl(FullUrl(link.link, login))) { performed =>
                  complete(performed.code, performed.transform())
                }
              }
            })
        },
       )
    },

      path(Segment) { shortUrl =>
          get {
            rejectEmptyResponse {
              onSuccess(getFullUrl(shortUrl)) { response =>
                if (response.maybeUrl.isDefined)
                  redirect(response.maybeUrl.get.fullUrl, StatusCodes.PermanentRedirect)
                else
                  complete(StatusCodes.NotFound, "No such short link")
              }
            }
            //#retrieve-user-info
          }
      }
    )
    //#users-get-delete
  }
  //#all-routes
}