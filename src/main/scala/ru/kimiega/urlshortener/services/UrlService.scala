package ru.kimiega.urlshortener.services

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.model.StatusCodes
import ru.kimiega.urlshortener.dtos.{ActionPerformedCode, FullUrl, GetUrlResponse, Url, Urls}
import ru.kimiega.urlshortener.repository.{UrlRepository, UserRegistry}

import java.time.LocalDateTime
import scala.util.matching.Regex

object UrlService {
  private val URL_PATTERN: Regex = "^((http(s)?):\\/\\/)(www\\.)?[a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&\\/\\/=]*)$".r
  private val ABC_DICT: String = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
  private var lastUrl: Option[List[Char]] = None

  sealed trait Command

  final case class GetUrls(login: String, replyTo: ActorRef[Urls]) extends Command

  final case class CreateUrl(fullUrl: FullUrl, replyTo: ActorRef[ActionPerformedCode]) extends Command

  final case class GetUrl(shortUrl: String, replyTo: ActorRef[GetUrlResponse]) extends Command

  final case class DeleteUrl(shortUrl: String, replyTo: ActorRef[ActionPerformedCode]) extends Command

  def apply(urlRep: UrlRepository, userRep: UserRegistry): Behavior[Command] = {
    registry(urlRep, userRep)
  }

  private def registry(urlRep: UrlRepository, userRep: UserRegistry): Behavior[Command] =
    Behaviors.receiveMessage {
      case CreateUrl(fullUrl, replyTo) =>
        if (URL_PATTERN.matches(fullUrl.fullUrl)) {
          val userId = userRep.dbGetUserId(fullUrl.author)
          if (userId.isDefined) {
            val newAddress = getNewAddress(urlRep)
            if (urlRep.dbGetUrl(newAddress).isDefined)
              urlRep.dbDeleteUrl(newAddress)
            val url = Url(newAddress, fullUrl.fullUrl, LocalDateTime.now().toString, userId.get.id)
            urlRep.dbCreateUrl(url)
            replyTo ! ActionPerformedCode(s"ShortUrl /u/${url.shortUrl} created.", StatusCodes.OK)
          }
          else
            replyTo ! ActionPerformedCode(s"Your login is not exist in system.", StatusCodes.Forbidden)
        }
        else
          replyTo ! ActionPerformedCode(s"Your link ${fullUrl.fullUrl} is not match url pattern.", StatusCodes.BadRequest)
        Behaviors.same

      case GetUrl(shortUrl, replyTo) =>
        replyTo ! GetUrlResponse(urlRep.dbGetUrl(shortUrl))
        Behaviors.same

      case DeleteUrl(shortUrl, replyTo) =>
        replyTo ! ActionPerformedCode(s"ShortUrl $shortUrl deleted.", StatusCodes.OK)
        urlRep.dbDeleteUrl(shortUrl)
        Behaviors.same

      case GetUrls(login, replyTo) =>
        replyTo ! urlRep.dbGetUrls(login)
        Behaviors.same
    }

  private def getNewAddress(urlRep: UrlRepository): String = {
    if (lastUrl.isEmpty) {
      try {
        val l = urlRep.dbGetLastUrl()
        if (l.isDefined)
          lastUrl = Option(l.get.shortUrl.toCharArray.toList)
        else
          lastUrl = Option(List(ABC_DICT.head))
      } catch {
        case e: Throwable => lastUrl = Option(List(ABC_DICT.head))
      }
    }
      if (lastUrl.get.forall(_ == ABC_DICT.last)) {
        lastUrl = Option((for (_ <- 0 to lastUrl.get.length) yield ABC_DICT.head).toList)
        if (lastUrl.get.length > 10)
          lastUrl = Option(List(ABC_DICT.head))
      }
      if (lastUrl.get.last == ABC_DICT.last) {
        var flag = true
        for ((c, index) <- lastUrl.get.reverse.zipWithIndex if flag) {
          if (c == ABC_DICT.last)
            lastUrl = Option(lastUrl.get.patch(index, Seq(ABC_DICT.head), 1))
          else {
            lastUrl = Option(lastUrl.get.patch(index, Seq(ABC_DICT.charAt(ABC_DICT.indexOf(c) + 1)), 1))
            flag = false
          }
        }
      }
      else
        lastUrl = Option(lastUrl.get.patch(lastUrl.get.length - 1, Seq(ABC_DICT.charAt(ABC_DICT.indexOf(lastUrl.get.last) + 1)), 1))
    lastUrl.get.foldLeft("")(_+_)
  }
}
