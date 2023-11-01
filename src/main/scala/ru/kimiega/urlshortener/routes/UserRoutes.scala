package ru.kimiega.urlshortener.routes

import akka.actor.typed.{ActorRef, ActorSystem}
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import ru.kimiega.urlshortener.dtos.ActionPerformed
import ru.kimiega.urlshortener.dtos._
import ru.kimiega.urlshortener.security.Authenticator
import ru.kimiega.urlshortener.services.UserService
import ru.kimiega.urlshortener.services.UserService._

import scala.concurrent.Future

class UserRoutes(userRegistry: ActorRef[UserService.Command], authenticator: Authenticator)(implicit val system: ActorSystem[_]) {

  import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
  import ru.kimiega.urlshortener.utils.JsonFormats._

  private implicit val timeout = Timeout.create(system.settings.config.getDuration("app.routes.ask-timeout"))

  def getUsers(): Future[Users] =
    userRegistry.ask(GetUsers)

  def getUser(name: String): Future[GetUserResponse] =
    userRegistry.ask(GetUser(name, _))

  def createUser(user: User): Future[ActionPerformedCode] =
    userRegistry.ask(CreateUser(user, _))

  def deleteUser(name: String): Future[ActionPerformedCode] =
    userRegistry.ask(DeleteUser(name, _))

  val userRoutes: Route =
  pathPrefix("users") {
    concat(
      authenticateBasic(realm = "secure   site", authenticator.authAdmin) {_ =>
        concat(
        pathEnd {
          concat(
            get {
              complete(getUsers())
            },
            post {
              entity(as[User]) { user =>
                onSuccess(createUser(user)) { performed =>
                  complete(performed.code, performed.transform())
                }
              }
            }
          )
        },
          path(Segment) { login =>
            concat(
              get {
                rejectEmptyResponse {
                  onSuccess(getUser(login)) { response =>
                    complete(response.maybeUser)
                  }
                }
              },
              delete {
                onSuccess(deleteUser(login)) { performed =>
                  complete((performed.code, performed.transform()))
                }
              })
          }
        )
      },
      authenticateBasic(realm = "secure   site", authenticator.authUser) { login =>
        path(Segment) { name =>
          if (name != login) complete(StatusCodes.Forbidden, "You are trying to access an account that is not yours")
          else
          concat(
            get {
              rejectEmptyResponse {
                onSuccess(getUser(name)) { response =>
                  complete(response.maybeUser)
                }
              }
            },
            delete {
              onSuccess(deleteUser(name)) { performed =>
                complete((performed.code, performed.transform()))
              }
            })
        }
      }
    )
  }
}