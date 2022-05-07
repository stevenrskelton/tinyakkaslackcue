package ca.stevenskelton.tinyakkaslackcue

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, Multipart}
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.server.Directives.{as, complete, entity, extractExecutionContext, formField}
import akka.http.scaladsl.server.directives.MarshallingDirectives.{as, entity}
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import ca.stevenskelton.tinyakkaslackcue.blocks.HomeTab
import ca.stevenskelton.tinyakkaslackcue.eventhandlers.AppHomeOpened
import org.slf4j.Logger
import play.api.libs.json.{JsObject, JsValue, Json}

import scala.concurrent.{ExecutionContext, Future}

class SlackActionRoute(implicit slackClient: SlackClient, slackTaskFactories: SlackTaskFactories, materializer: Materializer, logger: Logger) {

  val route: Route = Directives.post {
    formField("payload") { payload => {
      for {
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
    } map {
      _ => complete(HttpResponse(OK))
    } getOrElse {
        logger.error(s"SA:\n```$payload```")
        throw new NotImplementedError(s"Slack message unknown type $payload")
      }
    }
      //       match {
      //        case Some("block_actions") =>
      //          //        val eventObject = (jsObject \ "event").as[JsObject]
      //          //        val flow = (eventObject \ "type").as[String] match {
      //          //          case "app_home_opened" => AppHomeOpened(slackClient, slackTaskFactories, eventObject)
      //          //          case unknown => throw new NotImplementedError(s"Slack event $unknown not implemented: ${Json.stringify(jsObject)}")
      //          //        }
      //          //        extractExecutionContext {
      //          //          ec => complete(flow.map(_ => HttpResponse(OK))(ec))
      //          //        }
      //          (jsObject \ "view" \ "type").asOpt[String] match {
      //            case Some("home") =>
      //            case viewType =>
      //          }
      //
      //          val action = (jsObject \ "actions").as[Seq[JsValue]].headOption
      //
      //          logger.info(s"SA: block_actions\n```${Json.stringify(jsObject)}```")
      //          complete(HttpResponse(OK))
      //        case Some(t) =>
      //          throw new NotImplementedError(s"Slack message type $t not implemented: ${Json.stringify(jsObject)}")
      //        case None =>
      //          throw new NotImplementedError(s"Slack message unknown type ${Json.stringify(jsObject)}")
  }

}
