package ru.kimiega.urlshortener.services

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import ru.kimiega.urlshortener.configuration.PostgresConfig
import ru.kimiega.urlshortener.dtos.{ActionPerformed, FullUrl, GetUrlResponse, Url, Urls}
import ru.kimiega.urlshortener.repository.UrlRepository._
import ru.kimiega.urlshortener.utils.{Hasher, RepositoryTransactor}
import ru.kimiega.urlshortener.utils.RepositoryTransactor.Transactor

import java.time.LocalDateTime

object UrlService {
  sealed trait Command
  final case class GetUrls(login: String, replyTo: ActorRef[Urls]) extends Command
  final case class CreateUrl(fullUrl: FullUrl, replyTo: ActorRef[ActionPerformed]) extends Command
  final case class GetUrl(shortUrl: String, replyTo: ActorRef[GetUrlResponse]) extends Command
  final case class DeleteUrl(shortUrl: String, replyTo: ActorRef[ActionPerformed]) extends Command

  def apply(pgConfig: PostgresConfig): Behavior[Command] = {
    registry(RepositoryTransactor(pgConfig))
  }

  private def registry(xa: Transactor): Behavior[Command] =
    Behaviors.receiveMessage {
      case CreateUrl(fullUrl, replyTo) =>
        val url = Url(Hasher.apply(fullUrl.fullUrl).substring(0, 5), fullUrl.fullUrl, LocalDateTime.now().toString, 8)
        replyTo ! ActionPerformed(s"ShortUrl ${url.shortUrl}\nfor FullUrl ${url.fullUrl}\ncreated.")
        dbCreateUrl(xa, url)
        Behaviors.same

      case GetUrl(shortUrl, replyTo) =>
        replyTo !  GetUrlResponse(dbGetUrl(xa, shortUrl))
        Behaviors.same

      case DeleteUrl(shortUrl, replyTo) =>
        replyTo ! ActionPerformed(s"ShortUrl $shortUrl deleted.")
        dbDeleteUrl(xa, shortUrl)
        Behaviors.same

      case GetUrls(login, replyTo) =>
        replyTo ! dbGetUrls(login, xa)
        Behaviors.same
    }
}
