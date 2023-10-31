package ru.kimiega.urlshortener.services

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.StatusCodes
import ru.kimiega.urlshortener.configuration.PostgresConfig
import ru.kimiega.urlshortener.dtos.{ActionPerformed, ActionPerformedCode, FullUrl, GetUrlResponse, Url, Urls}
import ru.kimiega.urlshortener.repository.UrlRepository._
import ru.kimiega.urlshortener.repository.UserRegistry
import ru.kimiega.urlshortener.utils.{Hasher, RepositoryTransactor}
import ru.kimiega.urlshortener.utils.RepositoryTransactor.Transactor

import java.time.LocalDateTime
import scala.util.matching.Regex

object UrlService {
  private val URL_PATTERN: Regex = "^((http(s)?):\\/\\/)(www\\.)[a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)$".r

  sealed trait Command
  final case class GetUrls(login: String, replyTo: ActorRef[Urls]) extends Command
  final case class CreateUrl(fullUrl: FullUrl, replyTo: ActorRef[ActionPerformedCode]) extends Command
  final case class GetUrl(shortUrl: String, replyTo: ActorRef[GetUrlResponse]) extends Command
  final case class DeleteUrl(shortUrl: String, replyTo: ActorRef[ActionPerformedCode]) extends Command

  def apply(pgConfig: PostgresConfig): Behavior[Command] = {
    registry(RepositoryTransactor(pgConfig))
  }

  private def registry(xa: Transactor): Behavior[Command] =
    Behaviors.receiveMessage {
      case CreateUrl(fullUrl, replyTo) =>
        if (URL_PATTERN.matches(fullUrl.fullUrl)) {
          val userId = UserRegistry.dbGetUserId(xa, fullUrl.author)
          if (userId.isDefined) {
            val url = Url(Hasher.hashUrl(fullUrl.fullUrl, 1).substring(0, 5), fullUrl.fullUrl, LocalDateTime.now().toString, userId.get.id)
            dbCreateUrl(xa, url)
            replyTo ! ActionPerformedCode(s"ShortUrl ${url.shortUrl} for FullUrl ${url.fullUrl} created.", StatusCodes.OK)
          }
          else
            replyTo ! ActionPerformedCode(s"Your login is not exist in system.", StatusCodes.Forbidden)
        }
        else
          replyTo ! ActionPerformedCode(s"Your link ${fullUrl.fullUrl} is not match url pattern.", StatusCodes.BadRequest)
        Behaviors.same

      case GetUrl(shortUrl, replyTo) =>
        replyTo !  GetUrlResponse(dbGetUrl(xa, shortUrl))
        Behaviors.same

      case DeleteUrl(shortUrl, replyTo) =>
        replyTo ! ActionPerformedCode(s"ShortUrl $shortUrl deleted.", StatusCodes.OK)
        dbDeleteUrl(xa, shortUrl)
        Behaviors.same

      case GetUrls(login, replyTo) =>
        replyTo ! dbGetUrls(xa, login)
        Behaviors.same
    }
}
