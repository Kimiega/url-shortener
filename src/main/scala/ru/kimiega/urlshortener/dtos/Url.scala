package ru.kimiega.urlshortener.dtos

import scala.collection.immutable

final case class Url(shortUrl: String, fullUrl: String, creationDate: String, authorId: Int)
final case class Link(link: String)
final case class FullUrl(fullUrl: String, author: String)
final case class Urls(urls: immutable.Seq[Url])
final case class GetUrlResponse(maybeUrl: Option[Url])
