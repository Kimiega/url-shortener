package ru.kimiega.urlshortener.dtos

final case class Url(shortUrl: String, fullUrl: String, creationDate: String, authorId: Int)
final case class GetUrlResponse(maybeUrl: Option[Url])
