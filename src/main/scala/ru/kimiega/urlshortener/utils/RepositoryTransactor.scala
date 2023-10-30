package ru.kimiega.urlshortener.utils

import cats.effect.IO
import doobie.Transactor
import ru.kimiega.urlshortener.configuration.PostgresConfig

object RepositoryTransactor {
  type Transactor = doobie.Transactor.Aux[IO, Unit]

  def apply(pgConfig: PostgresConfig): Transactor = {
    Transactor.fromDriverManager[IO](
      "org.postgresql.Driver", pgConfig.url, pgConfig.user, pgConfig.pass
    )
  }
}
