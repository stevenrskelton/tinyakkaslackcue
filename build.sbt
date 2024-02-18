name := "tinyakkaslackqueue"

version := "0.1.0-SNAPSHOT"
organization := "ca.stevenskelton.tinyakkaslackqueue"

scalaVersion := "3.3.1"

val javaVersion = "19"

val pekkoVersion = "1.0.2"

lazy val tinyakkaslackqueue = (project in file("."))
  .settings(
    scalacOptions ++= {
      Seq(
        "-encoding", "UTF-8",
        "-deprecation",
        "-feature",
        "-unchecked",
        "-Ysafe-init",
        "-Xfatal-warnings",
//        "-Wvalue-discard",
        "-explain-types",
      )
    },
    javacOptions ++= Seq("-source", javaVersion, "-target", javaVersion),
  )

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.10.4",
  "ch.qos.logback" % "logback-classic" % "1.4.14",
  "com.slack.api" % "slack-api-client" % "1.38.0",
  "org.apache.pekko" %% "pekko-stream" % pekkoVersion,
  "org.apache.pekko" %% "pekko-stream-testkit" % pekkoVersion % Test,
  "org.scalatest" %% "scalatest" % "3.3.0-alpha.1" % Test
)
