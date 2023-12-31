import sbt.Keys.mappings

lazy val akkaHttpVersion = "10.5.0"
lazy val akkaVersion = "2.8.5"

fork := true

mappings in (Compile, packageDoc) := Seq()

lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging)
  .settings(
  inThisBuild(
    List(organization := "ru.kimiega", scalaVersion := "2.13.12")
  ),
  name := "urlshortener",
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "org.tpolecat" %% "doobie-core" % "1.0.0-RC1",
    "org.tpolecat" %% "doobie-postgres" % "1.0.0-RC1",
    "com.outr" %% "hasher" % "1.2.2"
  )
)
