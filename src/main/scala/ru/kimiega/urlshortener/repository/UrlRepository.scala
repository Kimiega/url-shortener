package ru.kimiega.urlshortener.repository

import cats.effect.unsafe.implicits.global
import doobie.implicits._
import ru.kimiega.urlshortener.utils.RepositoryTransactor
import RepositoryTransactor.Transactor
import ru.kimiega.urlshortener.dtos.{Url, Urls}

object UrlRepository {
  def dbCreateUrl(xa: Transactor, url: Url): Unit = {
    sql"INSERT INTO url_repository (shortUrl, fullUrl, creationDate, authorId) VALUES(${url.shortUrl}, ${url.fullUrl}, ${url.creationDate}, ${url.authorId})".
      update.
      withUniqueGeneratedKeys[Int]("id").
      transact(xa).
      unsafeRunSync
  }
  def dbGetUrl(xa: Transactor, shortUrl: String): Option[Url] = {
    sql"SELECT shortUrl, fullUrl, creationDate, authorId FROM url_repository WHERE shortUrl = ${shortUrl}".
      query[Url].
      option.
      transact(xa).
      unsafeRunSync()
  }
  def dbDeleteUrl(xa: Transactor, shortUrl: String): Unit = {
    sql"DELETE FROM url_repository WHERE shortUrl = ${shortUrl}".
      update.
      run.
      transact(xa).
      unsafeRunSync()
  }
  def dbGetUrls(login: String, xa: Transactor): Urls = {
    Urls(
      sql"SELECT shortUrl, fullUrl, creationDate, authorId FROM url_repository JOIN user_registry ur on url_repository.authorid = ur.id WHERE ur.login == ${login}".
        query[Url].
        to[List].
        transact(xa).
        unsafeRunSync
    )
  }
}
