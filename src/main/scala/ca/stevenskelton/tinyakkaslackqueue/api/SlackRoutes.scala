package ca.stevenskelton.tinyakkaslackqueue.api

import akka.Done
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Directives.{as, complete, entity, extractExecutionContext, formField}
import akka.http.scaladsl.server.{Directives, Route}
import akka.http.scaladsl.unmarshalling.FromRequestUnmarshaller
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import ca.stevenskelton.tinyakkaslackqueue.blocks._
import ca.stevenskelton.tinyakkaslackqueue.views._
import ca.stevenskelton.tinyakkaslackqueue.{SlackChannel, SlackPayload, SlackTs, SlackUserId}
import com.slack.api.methods.SlackApiTextResponse
import org.slf4j.Logger
import play.api.libs.json.{JsObject, Json}

import java.time.{ZoneId, ZonedDateTime}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class SlackRoutes(implicit slackFactories: SlackFactories) {

  import slackFactories.logger

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

  private def publishHomeTab(slackUserId: SlackUserId, slackHomeTab: SlackHomeTab)(implicit logger: Logger, slackFactories: SlackFactories): Future[Done] = {
    val response = slackFactories.slackClient.viewsPublish(slackUserId, slackHomeTab)
    if (response.isOk) {
      logger.debug(s"Created home view for ${slackUserId.value}")
      Future.successful(Done)
    } else {
      logger.error(s"Home view creation failed: ${response.getError}")
      logger.error(s"\n```${slackHomeTab.toString}```\n")
      Future.failed(new Exception(response.getError))
    }
  }

  private def cancelTask(ts: SlackTs, slackPayload: SlackPayload)(implicit slackFactories: SlackFactories, logger: Logger): Try[HomeTab] = {
    slackFactories.cancelScheduledTask(ts).map {
      cancelledTask =>
        logger.error("Cancelled Task")
        val view = new CancelTaskModal(cancelledTask)
        val update = slackFactories.slackClient.viewsUpdate(slackPayload.viewId, view)
        if (!update.isOk) {
          logger.error(view.toString)
          logger.error(update.getError)
        }
        Success(new HomeTab())
    }.getOrElse {
      val ex = new Exception(s"Could not find task ts ${ts.value}")
      logger.error("handleSubmission", ex)
      Failure(ex)
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
            publishHomeTab(slackUserId, new HomeTab())
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

      val handler: Try[SlackView] = slackPayload.payloadType match {
        case SlackPayload.BlockActions =>
          val viewType = (jsObject \ "view" \ "type").asOpt[String]
          if (viewType.contains("home")) {
            val action = slackPayload.action
            action.actionId match {
              case ActionId.HomeTabRefresh => Success(new HomeTab())
              case ActionId.TaskCancel => cancelTask(SlackTs(action.value), slackPayload)
              case ActionId.HomeTabTaskHistory =>
                slackFactories.findByChannel(SlackChannel(action.value)).map {
                  slackTaskMeta => new HomeTabTaskHistory(slackTaskMeta.history(Nil))
                }
              case ActionId.TaskQueue =>
                slackFactories.findByChannel(SlackChannel(action.value)).map {
                  slackTaskMeta => new CreateTaskModal(slackPayload.user, slackTaskMeta, None)
                }
              case ActionId.TaskSchedule =>
                slackFactories.findByChannel(SlackChannel(action.value)).map {
                  slackTaskMeta => new CreateTaskModal(slackPayload.user, slackTaskMeta, Some(ZonedDateTime.now()))
                }
              case ActionId.ModalQueuedTaskView =>
                val ts = SlackTs(action.value)
                val list = slackFactories.listScheduledTasks
                val index = list.indexWhere(_.id == ts)
                if (index == -1) {
                  val ex = new Exception(s"Task ts $ts not found")
                  logger.error("handleAction", ex)
                  Failure(ex)
                } else {
                  Success(new ViewTaskModal(list, index))
                }
              case ActionId.HomeTabConfiguration => Success(new HomeTabConfigure())
              case ActionId.RedirectToTaskThread => Success(SlackOkResponse)
            }
          } else if (slackPayload.callbackId.contains(CallbackId.View)) {
            slackPayload.actionStates.get(ActionId.TaskCancel).map {
              state => Success(SlackTs(state.asInstanceOf[ButtonState].value))
            }.getOrElse {
              val action = slackPayload.action
              if (action.actionId == ActionId.TaskCancel) {
                Success(SlackTs(action.value))
              } else {
                val ex = new Exception(s"Could not find action ${ActionId.TaskCancel.value}")
                logger.error("handleSubmission", ex)
                Failure(ex)
              }
            }.flatMap {
              cancelTask(_, slackPayload)
            }
          } else {
            logger.debug(s"where does this come from")
            Success(new HomeTab())
          }
        case SlackPayload.ViewSubmission if slackPayload.callbackId.contains(CallbackId.View) =>
          if (slackPayload.actionStates.get(ActionId.TaskCancel).map(o => SlackTs(o.asInstanceOf[DatePickerState].value.toString)).fold(false)(slackFactories.cancelScheduledTask(_).isDefined)) {
            Success(new HomeTab())
          } else {
            val ex = new Exception(s"Could not find task ts ${slackPayload.privateMetadata.fold("")(_.value)}")
            logger.error("handleSubmission", ex)
            Failure(ex)
          }
        case SlackPayload.ViewSubmission if slackPayload.callbackId.contains(CallbackId.Create) =>
          slackFactories.findByPrivateMetadata(slackPayload.privateMetadata.getOrElse(PrivateMetadata.Empty)).map {
            slackTaskMeta =>
              val zonedDateTimeOpt = for {
                scheduledDate <- slackPayload.actionStates.get(ActionId.DataScheduleDate).map(_.asInstanceOf[DatePickerState].value)
                scheduledTime <- slackPayload.actionStates.get(ActionId.DataScheduleTime).map(_.asInstanceOf[TimePickerState].value)
              } yield scheduledDate.atTime(scheduledTime).atZone(ZoneId.systemDefault())
              //TODO zone should be from Slack

              val scheduledSlackTask = slackFactories.scheduleSlackTask(slackPayload.user.id, slackTaskMeta, zonedDateTimeOpt)
              Success(new HomeTab())
          }.getOrElse {
            val ex = new Exception(s"Could not find task ${slackPayload.privateMetadata.fold("")(_.value)}")
            logger.error("handleSubmission", ex)
            Failure(ex)
          }
        case x =>
          val ex = new NotImplementedError(s"Slack type $x, for:\n```$payload```")
          logger.error("slackActionRoute", ex)
          Failure(ex)
      }
      val view = handler match {
        case Success(SlackOkResponse) => Future.successful(Done)
        case Success(homeTab: SlackHomeTab) => publishHomeTab(slackPayload.user.id, homeTab)
        case Success(slackModal: SlackModal) =>
          val result: SlackApiTextResponse = slackFactories.slackClient.viewsOpen(slackPayload.triggerId, slackModal)
          if (!result.isOk) {
            if (result.getError == "missing_scope") {
              logger.error(s"Missing permission scope: ${result.getNeeded}")
            } else {
              logger.debug(s"\n```$slackModal```\n")
              logger.error(result.getError)
            }
          }
          Future.successful(Done)
        case Failure(ex) => Future.failed(ex)
      }
      complete {
        import scala.concurrent.ExecutionContext.Implicits.global
        view.map {
          _ => HttpResponse(OK)
        }
      }
    }
  }

}
