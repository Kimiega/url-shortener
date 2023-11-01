package ru.kimiega.urlshortener.repository

import akka.actor.typed.ActorRef
import cats.effect.unsafe.implicits.global
import doobie.implicits._
import ru.kimiega.urlshortener.dtos.{ActionPerformed, GetUserResponse, User, UserId, Users}
import ru.kimiega.urlshortener.utils.RepositoryTransactor.Transactor


class UserRegistry(xa: Transactor) {
  def dbGetUsers(): Users = {
    Users(
      sql"SELECT login, password FROM user_registry".
      query[User].
      to[List].
      transact(xa).
      unsafeRunSync
    )
  }

  def dbGetUser(login: String): Option[User] = {
    sql"SELECT login, password FROM user_registry WHERE login = ${login}".
      query[User].
      option.
      transact(xa).
      unsafeRunSync
  }
  def dbGetUserId(login: String): Option[UserId] = {
    sql"SELECT id, login, password FROM user_registry WHERE login = ${login}".
      query[UserId].
      option.
      transact(xa).
      unsafeRunSync
  }

  def dbCreateUser(user: User): Unit = {
    sql"INSERT INTO user_registry (login, password) VALUES(${user.login}, ${user.password})".
      update.
      withUniqueGeneratedKeys[Int]("id").
      transact(xa).
      unsafeRunSync
  }

  def dbDeleteUser(login: String): Unit = {
    sql"DELETE FROM user_registry WHERE login = ${login}".
      update.
      run.
      transact(xa).
      unsafeRunSync()
  }
}
