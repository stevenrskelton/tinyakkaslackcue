package ca.stevenskelton.tinyakkaslackcue

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes.NotFound
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.SystemMaterializer
import ca.stevenskelton.tinyakkaslackcue.blocks.TaskHistory
import ca.stevenskelton.tinyakkaslackcue.logging.SlackLoggerFactory
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

class Main extends App {

  private implicit val config = ConfigFactory.defaultApplication().resolve()

  private implicit val httpActorSystem = ActorSystem("HTTPServer", config)

  implicit val slackClient = SlackClient.initialize(config)

  private implicit val httpLogger = SlackLoggerFactory.logToSlack(LoggerFactory.getLogger("HTTPServer"))(slackClient, SystemMaterializer(httpActorSystem).materializer)

  implicit val materializer = SystemMaterializer(httpActorSystem).materializer
  val host = config.getString("env.host")
  val port = config.getInt("env.http.port")

  implicit val slackTaskFactories = new SlackTaskFactories(slackClient, httpLogger, httpActorSystem, config) {
    override def factories: Seq[SlackTaskFactory] = Nil
  }

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
