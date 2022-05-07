package ca.stevenskelton.tinyakkaslackcue

import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Directives.{as, complete, entity, extractExecutionContext, formField}
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import ca.stevenskelton.tinyakkaslackcue.blocks.HomeTab
import ca.stevenskelton.tinyakkaslackcue.eventhandlers.AppHomeOpened
import org.slf4j.Logger
import play.api.libs.json.{JsObject, Json}

import scala.concurrent.{ExecutionContext, Future}

class SlackRoutes(implicit slackClient: SlackClient, slackTaskFactories: SlackTaskFactories, materializer: Materializer, logger: Logger) {

  private val unmarshaller = new FromRequestUnmarshaller[(String, JsObject)] {
    override def apply(value: HttpRequest)(implicit ec: ExecutionContext, materializer: Materializer): Future[(String, JsObject)] = {
      value.entity.getDataBytes().runWith(Sink.fold(ByteString.empty)(_ ++ _), materializer).map {
        byteString =>
          val body = byteString.utf8String
          logger.debug(s"SE:\n```$body```")
          val jsObject = Json.parse(byteString.utf8String).as[JsObject]
          ((jsObject \ "type").as[String], jsObject)
      }
    }
  }

  val slackEventRoute: Route = Directives.post {
    entity(as[(String, JsObject)](unmarshaller)) {
      case ("url_verification", jsObject) => complete((jsObject \ "challenge").as[String])
      case ("event_callback", jsObject) =>
        val eventObject = (jsObject \ "event").as[JsObject]
        logger.info(s"EventCallback ${Json.stringify(eventObject)}")
        val flow = (eventObject \ "type").as[String] match {
          case "app_home_opened" => AppHomeOpened(slackClient, slackTaskFactories, eventObject)
          case unknown => throw new NotImplementedError(s"Slack event $unknown not implemented: ${Json.stringify(jsObject)}")
        }
        extractExecutionContext {
          ec => complete(flow.map(_ => HttpResponse(OK))(ec))
        }
      case (t, jsObject) =>
        throw new NotImplementedError(s"Slack message type $t not implemented: ${Json.stringify(jsObject)}")
    }
  }

  val slackActionRoute: Route = Directives.post {
    formField("payload") { payload =>
      logger.info(s"Action payload: $payload")

      val handler = for {
        jsObject <- Json.parse(payload).asOpt[JsObject]
        actionType <- (jsObject \ "type").asOpt[String]
        viewType <- (jsObject \ "view" \ "type").asOpt[String]
        triggerId <- (jsObject \ "trigger_id").asOpt[String].map(SlackTriggerId(_))
        slackUser <- (jsObject \ "user").asOpt[SlackUser]
      } yield (actionType, viewType) match {
        case ("block_actions", "home") => HomeTab.handleAction(triggerId, slackUser, jsObject)
        case ("view_submission", "modal") => HomeTab.handleSubmission(triggerId, slackUser, jsObject)
        case (x, y) => throw new NotImplementedError(s"Slack payload $x, for view $y not implemented: $payload")
      }

      handler.map {
        _ => complete(HttpResponse(OK))
      } getOrElse {
        logger.error(s"SA:\n```$payload```")
        throw new NotImplementedError(s"Slack message unknown type $payload")
      }
    }
  }
}
