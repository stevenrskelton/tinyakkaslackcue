name := "tinyakkaslackcue"

version := "0.1.0-SNAPSHOT"
organization := "ca.stevenskelton.tinyakkaslackcue"

scalaVersion := "2.13.8"

val javaVersion = "11"

lazy val akkaVersion = "2.6.19"
lazy val akkaHttpVersion = "10.2.9"
lazy val akkaGrpcVersion = "2.1.3"

lazy val app = (project in file("."))
  .settings(
    scalacOptions += s"-target:jvm-$javaVersion",
    javacOptions ++= Seq("-source", javaVersion, "-target", javaVersion)
  )

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.8.2",
  "ch.qos.logback" % "logback-classic" % "1.2.11",
  "com.slack.api" % "slack-api-client" % "1.21.1",
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http2-support" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-discovery" % akkaVersion,
  "com.typesafe.akka" %% "akka-pki" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.2.9" % Test
)
