name := "tinyakkaslackqueue"

version := "0.1.0-SNAPSHOT"
organization := "ca.stevenskelton.tinyakkaslackqueue"

scalaVersion := "3.3.1"

val javaVersion = "16"

lazy val akkaVersion = "2.7.0"
lazy val akkaHttpVersion = "10.5.2"

lazy val app = (project in file("."))
  .settings(
    scalacOptions ++= {
      Seq(
        "-encoding",
        "UTF-8",
        "-feature",
        //        "-language:implicitConversions",
        // disabled during the migration
        // "-Xfatal-warnings"
        "-unchecked",
      )
    },
    javacOptions ++= Seq("-source", javaVersion, "-target", javaVersion),
  )

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.10.1",
  "ch.qos.logback" % "logback-classic" % "1.4.7",
  "com.slack.api" % "slack-api-client" % "1.28.0",
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http2-support" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-discovery" % akkaVersion,
  "com.typesafe.akka" %% "akka-pki" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
//  "org.scalamock" %% "scalamock" % "5.2.0" % Test,
  "org.scalatest" %% "scalatest" % "3.2.15" % Test
)
