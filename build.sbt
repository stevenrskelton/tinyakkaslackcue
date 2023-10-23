name := "tinyakkaslackqueue"

version := "0.1.0-SNAPSHOT"
organization := "ca.stevenskelton.tinyakkaslackqueue"

scalaVersion := "3.3.1"

val javaVersion = "17"

lazy val PekkoVersion = "1.0.1"
//lazy val akkaHttpVersion = "10.5.2"

lazy val app = (project in file("."))
  .settings(
    scalacOptions ++= {
      Seq(
        "-encoding", "UTF-8",
        "-deprecation",
        "-feature",
        "-unchecked",
        "-language:experimental.macros",
        "-language:higherKinds",
        "-language:implicitConversions",
        "-Ykind-projector",
        //        "-Yexplicit-nulls",
        "-Ysafe-init",
//        "-Wvalue-discard",
//        "-source:3.0-migration",
        // "-Xfatal-warnings"
      )
    },
    javacOptions ++= Seq("-source", javaVersion, "-target", javaVersion),
  )

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.10.1",
  "ch.qos.logback" % "logback-classic" % "1.4.7",
  "com.slack.api" % "slack-api-client" % "1.28.0",
//  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
//  "com.typesafe.akka" %% "akka-http2-support" % akkaHttpVersion,
//  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "org.apache.pekko" %% "pekko-stream" % PekkoVersion,
//  "com.typesafe.akka" %% "akka-discovery" % akkaVersion,
//  "com.typesafe.akka" %% "akka-pki" % akkaVersion,
//  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "org.apache.pekko" %% "pekko-stream-testkit" % PekkoVersion % Test,
//  "org.scalamock" %% "scalamock" % "5.2.0" % Test,
  "org.scalatest" %% "scalatest" % "3.2.15" % Test
)
