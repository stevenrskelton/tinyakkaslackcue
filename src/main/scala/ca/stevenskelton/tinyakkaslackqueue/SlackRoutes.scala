package ca.stevenskelton.tinyakkaslackqueue

import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Directives.{as, complete, entity, extractExecutionContext, formField}
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import ca.stevenskelton.tinyakkaslackqueue.blocks._
import org.slf4j.Logger
import play.api.libs.json.{JsObject, Json}

import scala.concurrent.{ExecutionContext, Future}

class SlackRoutes(implicit slackClient: SlackClient, slackTaskFactories: SlackFactories, materializer: Materializer, logger: Logger) {

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
        logger.info(s"EventCallback\n```${Json.stringify(eventObject)}```")
        val flow = (eventObject \ "type").as[String] match {
          case "app_home_opened" =>
            val slackUserId = SlackUserId((eventObject \ "user").as[String])
            HomeTabActions.openedEvent(slackUserId)
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
      logger.info(s"Action payload:\n```$payload```\n")
      val jsObject = Json.parse(payload).as[JsObject]
      val slackPayload = SlackPayload(jsObject)

      val handler = slackPayload.payloadType match {
        case SlackPayload.BlockActions =>
          val viewType = (jsObject \ "view" \ "type").asOpt[String]
          if (viewType.contains("home")) {
            val action = slackPayload.action
            action.actionId match {
              case ActionId.TabRefresh =>
                HomeTabActions.update(slackPayload)
              case ActionId.TaskCancel =>
                HomeTabActions.cancelTask(SlackTs(action.value),  slackPayload)
              case _ =>
                HomeTabActions.handleAction(slackPayload)
            }
          } else if (slackPayload.callbackId.contains(CallbackId.View)) {
            slackPayload.actionStates.get(ActionId.TaskCancel).map { state =>
              val ts = SlackTs(state.asInstanceOf[ButtonState].value)
              HomeTabActions.cancelTask(ts, slackPayload)
            }.getOrElse {
              val action = slackPayload.action
              if (action.actionId == ActionId.TaskCancel) {
                val ts = SlackTs(slackPayload.actions.head.value)
                HomeTabActions.cancelTask(ts,  slackPayload)
              } else {
                val ex = new Exception(s"Could not find action ${ActionId.TaskCancel.value}")
                logger.error("handleSubmission", ex)
                Future.failed(ex)
              }
            }
          } else {
            HomeTabActions.update(slackPayload)
          }
        case SlackPayload.ViewSubmission =>
          HomeTabActions.handleSubmission(slackPayload)
        case x =>
          val ex = new NotImplementedError(s"Slack type $x, for:\n```$payload```")
          logger.error("slackActionRoute", ex)
          Future.failed(ex)
      }
      complete {
        import scala.concurrent.ExecutionContext.Implicits.global
        handler.map {
          _ => HttpResponse(OK)
        }
      }
    }
  }

}
