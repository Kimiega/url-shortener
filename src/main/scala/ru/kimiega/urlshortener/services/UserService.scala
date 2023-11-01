package ru.kimiega.urlshortener.services

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.StatusCodes
import ru.kimiega.urlshortener.configuration.PostgresConfig
import ru.kimiega.urlshortener.dtos.{ActionPerformed, ActionPerformedCode, GetUserResponse, User, Users}
import ru.kimiega.urlshortener.repository.UserRegistry
import ru.kimiega.urlshortener.utils.{Hasher, RepositoryTransactor}
import ru.kimiega.urlshortener.utils.RepositoryTransactor.Transactor

object UserService {
  sealed trait Command
  final case class GetUsers(replyTo: ActorRef[Users]) extends Command
  final case class CreateUser(user: User, replyTo: ActorRef[ActionPerformedCode]) extends Command
  final case class GetUser(login: String, replyTo: ActorRef[GetUserResponse]) extends Command
  final case class DeleteUser(login: String, replyTo: ActorRef[ActionPerformedCode]) extends Command

  def apply(userRep: UserRegistry): Behavior[Command] = {
    registry(userRep)
  }

  private def registry(userRep: UserRegistry): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetUsers(replyTo) =>
        replyTo ! userRep.dbGetUsers()
        Behaviors.same

      case CreateUser(user, replyTo) =>
        try {
          userRep.dbCreateUser(User(user.login, Hasher.hashPassword(user.password)))
          replyTo ! ActionPerformedCode(s"User ${user.login} created.", StatusCodes.Created)
        }
        catch {
          case _: Throwable => replyTo ! ActionPerformedCode(s"User ${user.login} couldn't be created.", StatusCodes.BadRequest)
        }
        Behaviors.same

      case GetUser(name, replyTo) =>
        replyTo !  GetUserResponse(userRep.dbGetUser(name))
        Behaviors.same

      case DeleteUser(login, replyTo) =>
        replyTo ! ActionPerformedCode(s"User $login deleted.", StatusCodes.OK)
        userRep.dbDeleteUser(login)
        Behaviors.same
    }
}
