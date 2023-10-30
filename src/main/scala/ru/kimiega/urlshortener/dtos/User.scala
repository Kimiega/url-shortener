package ru.kimiega.urlshortener.dtos

import scala.collection.immutable

final case class User(login: String, password: String)
final case class Users(users: immutable.Seq[User])
final case class GetUserResponse(maybeUser: Option[User])
