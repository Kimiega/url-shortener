package ru.kimiega.urlshortener.configuration

import com.typesafe.config.Config

final case class PostgresConfig (driver:String, host: String, port: String, db: String, user: String, pass: String) {
  def url: String = s"jdbc:postgresql://$host:$port/$db"
}

object PostgresConfig {
  def apply(config: Config): PostgresConfig = {
    PostgresConfig(
      config.getString("app.db.driver"),
      config.getString("app.db.host"),
      config.getString("app.db.port"),
      config.getString("app.db.database"),
      config.getString("app.db.user"),
      config.getString("app.db.pass")
    )
  }
}
