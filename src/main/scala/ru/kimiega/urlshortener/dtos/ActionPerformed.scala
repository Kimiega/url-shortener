package ru.kimiega.urlshortener.dtos

import akka.http.scaladsl.model.StatusCode

final case class ActionPerformedCode(description: String, code: StatusCode) {
  def transform(): ActionPerformed = ActionPerformed(description)
}
final case class ActionPerformed(description: String)