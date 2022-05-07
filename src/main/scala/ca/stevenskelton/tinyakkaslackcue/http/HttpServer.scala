package ca.stevenskelton.tinyakkaslackcue.http

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes.NotFound
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.{Http, model}
import akka.stream.SystemMaterializer
import ca.stevenskelton.tinyakkaslackcue.{SlackActionRoute, SlackClient, SlackEventRoute}
import com.typesafe.config.Config
import org.slf4j.Logger

import scala.concurrent.Future

object HttpServer {
  val RouteBaseDirectory = "/admin"
  val RouteStockPrice = s"$RouteBaseDirectory/stockprice"
  val RouteEconomicCalendar = s"$RouteBaseDirectory/economiccalendar"
  val RouteEarningsCalendar = s"$RouteBaseDirectory/earningscalendar"
  val RouteTasks = s"$RouteBaseDirectory/tasks"

  val RouteBaseDirectoryPath = "admin"
  val RouteStockPricePath = RouteBaseDirectoryPath / "stockprice"
  val RouteEconomicCalendarPath = RouteBaseDirectoryPath / "economiccalendar"
  val RouteEarningsCalendarPath = RouteBaseDirectoryPath / "earningscalendar"
  val RouteTasksPath = RouteBaseDirectoryPath / "tasks"

  def exists(request: HttpRequest): Boolean = request.uri.path.toString() match {
    case "/" => true
    case "/favicon.ico" => true
    case "/releases" => true
    case "/stocks" => true
    case "/tasks" => true
    case "/slack/events" => true
    case _ => false
  }

  val notFound: HttpResponse = model.HttpResponse(model.StatusCodes.NotFound)

//  def bindSecure(config: Config)(implicit slackTaskFactories: SlackTaskFactories, actorSystem: ActorSystem, scraperDirectories: ScraperDirectories, logger: Logger): Future[Http.ServerBinding] = bindPublic(config)

  def bindPublic(config: Config)(implicit actorSystem: ActorSystem, logger: Logger): Future[Http.ServerBinding] = {
    implicit val materializer = SystemMaterializer(actorSystem).materializer
    implicit val _config = config
    val host = config.getString("env.host")
    val port = config.getInt("env.http.port")

    implicit val slackClient = SlackClient(config)
    implicit val slackTaskFactories = SlackClient.taskFactories

    val slackEvent = new SlackEventRoute(slackClient, slackTaskFactories)
    val slackAction = new SlackActionRoute()

    val publicRoutes = concat(
      path("slack" / "event")(slackEvent.route),
      path("slack" / "action")(slackAction.route)
    )

    Http()(actorSystem).newServerAt(host, port).bind(concat(publicRoutes, Route.seal {
      extractRequestContext {
        context =>
          complete {
            logger.info(s"404 ${context.request.method.value} ${context.unmatchedPath}")
            HttpResponse(NotFound, entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, "Not Found"))
          }
      }
    }))
  }
}
