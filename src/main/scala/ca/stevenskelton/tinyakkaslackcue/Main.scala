package ca.stevenskelton.tinyakkaslackcue

import akka.actor.ActorSystem
import akka.stream.SystemMaterializer
import ca.stevenskelton.tinyakkaslackcue.http.HttpServer
import ca.stevenskelton.tinyakkaslackcue.logging.SlackLoggerFactory
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits._

class Main extends App {

  private implicit val config = ConfigFactory.defaultApplication().resolve()

  private val httpActorSystem = ActorSystem("HTTPServer", config)

  private val httpLogger = SlackLoggerFactory.logToSlack(LoggerFactory.getLogger("HTTPServer"))(SlackClient(config), SystemMaterializer(httpActorSystem).materializer)

  val httpServer = HttpServer.bindPublic(config)(httpActorSystem, httpLogger)
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
