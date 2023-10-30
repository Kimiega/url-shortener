package ru.kimiega.urlshortener.services

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import ru.kimiega.urlshortener.configuration.PostgresConfig
import ru.kimiega.urlshortener.dtos.{ActionPerformed, GetUserResponse, User, Users}
import ru.kimiega.urlshortener.repository.UserRegistry._
import ru.kimiega.urlshortener.utils.{Hasher, RepositoryTransactor}
import ru.kimiega.urlshortener.utils.RepositoryTransactor.Transactor

object UserService {
  sealed trait Command
  final case class GetUsers(replyTo: ActorRef[Users]) extends Command
  final case class CreateUser(user: User, replyTo: ActorRef[ActionPerformed]) extends Command
  final case class GetUser(login: String, replyTo: ActorRef[GetUserResponse]) extends Command
  final case class DeleteUser(login: String, replyTo: ActorRef[ActionPerformed]) extends Command

  def apply(pgConfig: PostgresConfig): Behavior[Command] = {
    registry(RepositoryTransactor(pgConfig))
  }

  private def registry(xa: Transactor): Behavior[Command] =
    Behaviors.receiveMessage {
      case GetUsers(replyTo) =>
        replyTo ! dbGetUsers(xa)
        Behaviors.same

      case CreateUser(user, replyTo) =>
        try {
          dbCreateUser(xa, User(user.login, Hasher.apply(user.password)))
          replyTo ! ActionPerformed(s"User ${user.login} created.")
        }
        catch {
          case _: Throwable => replyTo ! ActionPerformed(s"User ${user.login} couldn't be created.")
        }
        Behaviors.same

      case GetUser(name, replyTo) =>
        replyTo !  GetUserResponse(dbGetUser(xa, name))
        Behaviors.same

      case DeleteUser(login, replyTo) =>
        replyTo ! ActionPerformed(s"User $login deleted.")
        dbDeleteUser(xa, login)
        Behaviors.same
    }
}
