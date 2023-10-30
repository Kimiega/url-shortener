package ru.kimiega.urlshortener.repository

import akka.actor.typed.ActorRef
import cats.effect.unsafe.implicits.global
import doobie.implicits._
import ru.kimiega.urlshortener.dtos.{ActionPerformed, GetUserResponse, User, Users}
import ru.kimiega.urlshortener.utils.RepositoryTransactor.Transactor


object UserRegistry {
  def dbGetUsers(xa: Transactor): Users = {
    Users(
      sql"SELECT login, password FROM user_registry".
      query[User].
      to[List].
      transact(xa).
      unsafeRunSync
    )
  }

  def dbGetUser(xa: Transactor, login: String): Option[User] = {
    sql"SELECT login, password FROM user_registry WHERE login = ${login}".
      query[User].
      option.
      transact(xa).
      unsafeRunSync
  }

  def dbCreateUser(xa: Transactor, user: User): Unit = {
    sql"INSERT INTO user_registry (login, password) VALUES(${user.login}, ${user.password})".
      update.
      withUniqueGeneratedKeys[Int]("id").
      transact(xa).
      unsafeRunSync
  }

  def dbDeleteUser(xa: Transactor, login: String): Unit = {
    sql"DELETE FROM user_registry WHERE login = ${login}".
      update.
      run.
      transact(xa).
      unsafeRunSync()
  }
}
