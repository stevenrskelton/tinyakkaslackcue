package ca.stevenskelton.tinyakkaslackqueue.example

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes.NotFound
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.http.scaladsl.server.Directives.*
import akka.http.scaladsl.server.Route
import akka.stream.{Materializer, SystemMaterializer}
import ca.stevenskelton.tinyakkaslackqueue.SlackFactories
import ca.stevenskelton.tinyakkaslackqueue.api.*
import ca.stevenskelton.tinyakkaslackqueue.logging.{SlackLogger, SlackLoggerFactory}
import com.slack.api.Slack
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

import scala.concurrent.ExecutionContext.Implicits.*
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

object Main extends App {

  private implicit val config: Config = ConfigFactory.defaultApplication().resolve()

  private implicit val httpActorSystem: ActorSystem = ActorSystem("HTTPServer", config)

  val backupLogger = LoggerFactory.getLogger("HTTPServer")

  val slackConfig = SlackConfig(config, backupLogger, () => Slack.getInstance.methods)

  implicit val materializer: Materializer = SystemMaterializer(httpActorSystem).materializer

  private implicit val httpLogger: SlackLogger = SlackLoggerFactory.logToSlack(
    backupLogger.getName, Level.INFO, slackConfig, backup = Some(backupLogger), mirror = Some(backupLogger)
  )

  implicit val slackClient: SlackClient = SlackClientImpl(slackConfig)

  val host = config.getString("tinyakkaslackqueue.env.host")
  val port = config.getInt("tinyakkaslackqueue.env.http.port")

  val slackTaskFactories = SlackTaskFactories(
    new TestSlackTaskFactory(30.seconds)
  )

  implicit val slackFactories: SlackFactories = SlackFactories.initialize(slackTaskFactories, config)
  //  slackTaskFactories.slackTaskMetaFactories

  val slackRoutes = new SlackRoutes(slackTaskFactories, slackClient, config)

  val publicRoutes = concat(
    path("slack" / "event")(slackRoutes.slackEventRoute),
    path("slack" / "action")(slackRoutes.slackActionRoute)
  )

  val httpServer: Future[Http.ServerBinding] = Http()(httpActorSystem).newServerAt(host, port).bind(concat(publicRoutes, Route.seal {
    extractRequestContext {
      context =>
        complete {
          httpLogger.info(s"404 ${context.request.method.value} ${context.unmatchedPath}")
          HttpResponse(NotFound, entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, "Not Found"))
        }
    }
  }))

  httpServer.map {
    httpBinding =>
      val address = httpBinding.localAddress
      httpLogger.info("HTTP server bound to {}:{}", address.getHostString, address.getPort)
      httpBinding.whenTerminated.onComplete {
        _ =>
          httpActorSystem.terminate()
          System.exit(0)
      }
  }.recover {
    ex =>
      httpLogger.error("Failed to bind endpoint, terminating system", ex)
      httpActorSystem.terminate()
      System.exit(1)
  }

}
