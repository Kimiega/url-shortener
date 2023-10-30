package ru.kimiega.urlshortener.services

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import ru.kimiega.urlshortener.configuration.PostgresConfig
import ru.kimiega.urlshortener.dtos.{ActionPerformed, GetUrlResponse, Url}
import ru.kimiega.urlshortener.repository.UrlRepository._
import ru.kimiega.urlshortener.utils.RepositoryTransactor
import ru.kimiega.urlshortener.utils.RepositoryTransactor.Transactor

object UrlService {
  sealed trait Command
  final case class CreateUrl(url: Url, replyTo: ActorRef[ActionPerformed]) extends Command
  final case class GetUrl(shortUrl: String, replyTo: ActorRef[GetUrlResponse]) extends Command
  final case class DeleteUrl(shortUrl: String, replyTo: ActorRef[ActionPerformed]) extends Command

  def apply(pgConfig: PostgresConfig): Behavior[Command] = {
    registry(RepositoryTransactor(pgConfig))
  }

  private def registry(xa: Transactor): Behavior[Command] =
    Behaviors.receiveMessage {
      case CreateUrl(url, replyTo) =>
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
    }
}
