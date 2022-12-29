package ca.stevenskelton.tinyakkaslackqueue.example

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes.NotFound
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.SystemMaterializer
import ca.stevenskelton.tinyakkaslackqueue.SlackFactories
import ca.stevenskelton.tinyakkaslackqueue.api._
import ca.stevenskelton.tinyakkaslackqueue.logging.SlackLoggerFactory
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

object Main extends App {

  private implicit val config = ConfigFactory.defaultApplication().resolve()

  private implicit val httpActorSystem = ActorSystem("HTTPServer", config)

  val slackConfig = SlackClient.initialize(config)

  val backupLogger = LoggerFactory.getLogger("HTTPServer")
  private implicit val httpLogger = SlackLoggerFactory.logToSlack(backupLogger.getName, slackConfig, backup = Some(backupLogger), mirror = Some(backupLogger))(SystemMaterializer(httpActorSystem).materializer)

  implicit val slackClient: SlackClient = SlackClientImpl(slackConfig, slackConfig.client)

  implicit val materializer = SystemMaterializer(httpActorSystem).materializer
  val host = config.getString("tinyakkaslackqueue.env.host")
  val port = config.getInt("tinyakkaslackqueue.env.http.port")

  val slackTaskFactories = SlackTaskFactories(
    new TestSlackTaskFactory(30.seconds)
  )

  implicit val slackFactories = SlackFactories.initialize(slackTaskFactories)
  //  slackTaskFactories.slackTaskMetaFactories

  val slackRoutes = new SlackRoutes()

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
