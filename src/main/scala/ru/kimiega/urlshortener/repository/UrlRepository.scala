package ru.kimiega.urlshortener.repository

import cats.effect.unsafe.implicits.global
import doobie.implicits._
import ru.kimiega.urlshortener.utils.RepositoryTransactor
import RepositoryTransactor.Transactor
import ru.kimiega.urlshortener.dtos.{Url, Urls}

class UrlRepository(xa: Transactor) {
  def dbCreateUrl(url: Url): Unit = {
    sql"INSERT INTO url_repository (shortUrl, fullUrl, creationDate, authorId) VALUES(${url.shortUrl}, ${url.fullUrl}, ${url.creationDate}, ${url.authorId})".
      update.
      withUniqueGeneratedKeys[Int]("id").
      transact(xa).
      unsafeRunSync
  }
  def dbGetUrl(shortUrl: String): Option[Url] = {
    sql"SELECT shortUrl, fullUrl, creationDate, authorId FROM url_repository WHERE shortUrl = ${shortUrl}".
      query[Url].
      option.
      transact(xa).
      unsafeRunSync()
  }
  def dbDeleteUrl(shortUrl: String): Unit = {
    sql"DELETE FROM url_repository WHERE shortUrl = ${shortUrl}".
      update.
      run.
      transact(xa).
      unsafeRunSync()
  }
  def dbGetUrls(login: String): Urls = {
    Urls(
      sql"SELECT shortUrl, fullUrl, creationDate, authorId FROM url_repository JOIN user_registry on url_repository.authorid = user_registry.id WHERE user_registry.login = ${login}".
        query[Url].
        to[List].
        transact(xa).
        unsafeRunSync
    )
  }
  def dbGetLastUrl(): Option[Url] = {
    val id = sql"SELECT last_value FROM public.url_repository_id_seq".
      query[Int].
      option.
      transact(xa).
      unsafeRunSync()
    if (id.isDefined)
    sql"SELECT shortUrl, fullUrl, creationDate, authorId FROM url_repository WHERE id = ${id}".
      query[Url].
      option.
      transact(xa).
      unsafeRunSync()
    else
      None
  }
}
