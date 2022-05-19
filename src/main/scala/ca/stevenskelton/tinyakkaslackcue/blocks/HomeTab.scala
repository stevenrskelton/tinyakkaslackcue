package ca.stevenskelton.tinyakkaslackcue.blocks

import akka.Done
import ca.stevenskelton.tinyakkaslackcue._
import ca.stevenskelton.tinyakkaslackcue.blocks.ScheduleActionModal.{ActionIdLogLevel, ActionIdNotifyOnComplete, ActionIdNotifyOnFailure, ActionIdScheduleDate, ActionIdScheduleTime}
import ca.stevenskelton.tinyakkaslackcue.util.DateUtils
import com.slack.api.methods.SlackApiTextResponse
import org.slf4j.Logger
import play.api.libs.json.{JsObject, JsValue, Json}

import java.time.{Duration, LocalDate, LocalTime, ZoneId, ZonedDateTime}
import scala.collection.SortedSet
import scala.concurrent.Future
import scala.util.Try

object HomeTab {

  val ActionIdTaskQueue = ActionId("task-queue-action")
  val ActionIdTaskSchedule = ActionId("schedule-task-action")
  val ActionIdTaskCancel = ActionId("multi_users_select-action1")

  object State extends Enumeration {
    type State = Value

    val Running, Scheduled, Success, Failure = Value
  }

//  private implicit val orderingFields = new Ordering[Fields] {
//    override def compare(x: Fields, y: Fields): Int = x.slackTaskIdentifier.name.compareTo(y.slackTaskIdentifier.name)
//  }

  def openedEvent(slackClient: SlackClient, slackTaskFactories: SlackTaskFactories, jsObject: JsObject)(implicit logger: Logger): Future[Done] = {
    //    val blocks = (jsObject \ "view" \ "blocks").as[Seq[JsValue]]
    //    if (blocks.isEmpty) {
    //      HomeTab.parseView(blocks).fold(Future.failed(_), _ => Future.successful(Done))
    val userId = SlackUserId((jsObject \ "user").as[String])
    val viewBlocks = slackTaskFactories.history
    //TODO: sort
    val blocks = SlackBlocksAsString(viewBlocks.map(_.toBlocks.value).mkString(""",{"type": "divider"},"""))
    val response = slackClient.viewsPublish(userId, "home", blocks)
    if (response.isOk) {
      logger.debug(s"Updated home view for ${userId.value}")
      Future.successful(Done)
    } else {
      logger.error(s"Home view update failed: ${response.getError}")
      Future.failed(new Exception(response.getError))
    }
    //    } else {
    //      //TODO: compare to hash
    //      Future.successful(Done)
    //    }
  }

  def parseView(blocks: Seq[JsValue]): Try[TaskHistory] = Try {
    ???
  }

  def handleSubmission(slackTriggerId: SlackTriggerId, slackUser: SlackUser, jsObject: JsObject)(implicit slackClient: SlackClient, slackTaskFactories: SlackTaskFactories, logger: Logger): Future[Done] = {
    logger.info(Json.stringify(jsObject))
    val (privateMetadata, actionStates) = ScheduleActionModal.parseViewSubmission(jsObject)
    slackTaskFactories.findByPrivateMetadata(privateMetadata).map {
      taskFactory =>

        val zonedDateTimeOpt = for{
          scheduledDate <- actionStates.get(ActionIdScheduleDate).map(_.asInstanceOf[DatePickerState].value)
          scheduledTime <-  actionStates.get(ActionIdScheduleTime).map(_.asInstanceOf[TimePickerState].value)
        } yield scheduledDate.atTime(scheduledTime).atZone(ZoneId.systemDefault())
        //TODO zone should be from Slack

        val slackTask = slackTaskFactories.tinySlackCue.scheduleSlackTask(taskFactory, zonedDateTimeOpt)
        val msg = zonedDateTimeOpt.fold("Queued")(_ => "Scheduled")
        //TODO: can we quote the task thread
        slackClient.chatPostMessageInThread(s"$msg ${slackTask.name}", slackClient.historyThread)

        Future.successful(Done)
//        actionStates(ActionIdNotifyOnComplete).asInstanceOf[MultiUsersState].users shouldBe Seq(SlackUserId("U039T9DUHGT"))
//        actionStates(ActionIdNotifyOnFailure).asInstanceOf[MultiUsersState].users shouldBe Seq(SlackUserId("U039T9DUHGT"), SlackUserId("U039TCGLX5G"))
//        actionStates(ActionIdLogLevel)

    }.getOrElse {
      val ex = new Exception(s"Could not find task ${privateMetadata.value}")
      logger.error("handleSubmission", ex)
      Future.failed(ex)
    }
  }

  def handleAction(slackTriggerId: SlackTriggerId, slackUser: SlackUser, jsObject: JsObject)(implicit slackClient: SlackClient, slackTaskFactories: SlackTaskFactories, logger: Logger): Future[Done] = {
    logger.info(Json.stringify(jsObject))

    val slackActions: Seq[SlackAction] = (jsObject \ "actions").as[Seq[SlackAction]]
    val action = slackActions.head
    val view = action.actionId match {
      case ActionIdTaskQueue =>
        ScheduleActionModal.modal(slackUser, action.value, None, PrivateMetadata(action.value))
      case ActionIdTaskSchedule =>
        ScheduleActionModal.modal(slackUser,action.value, Some(ZonedDateTime.now()), PrivateMetadata(action.value))
      case ActionIdTaskCancel =>
        ScheduleActionModal.modal(slackUser,action.value, None, PrivateMetadata(action.value))
    }
    //    slackActions.headOption.map {
    //      slackAction =>
    //        Slack.getInstance.methods.viewsOpen((r: ViewsOpenRequest.ViewsOpenRequestBuilder) => r.token())
    //    }
    val result: SlackApiTextResponse = slackClient.viewsOpen(slackTriggerId, view)
    if (!result.isOk) {
      logger.debug(view.value)
      logger.error(result.getError)
    }
    Future.successful(Done)
  }
  //  def toAddInstanceToBlocks(existing: Seq[Fields], slackTask: SlackTask, instance: FieldsInstance): Seq[Fields] = {
  //    val updatedFields = if (existing.exists(_.name == slackTask)) {
  //      existing.map {
  //        fields =>
  //          if (fields.name == slackTask.name) {
  //            val (lastSuccess, lastFailure) = instance.state match {
  //              case State.Scheduled => (fields.lastSuccess, fields.lastFailure)
  //              case State.Success => (Some(instance), fields.lastFailure)
  //              case State.Failure => (fields.lastSuccess, Some(instance))
  //            }
  //            val nextTs = if (instance.state == State.Scheduled) {
  //              Some(fields.nextTs.map { s =>
  //                SlackTs(Seq(s.value, slackTask.ts.value).min)
  //              }.getOrElse {
  //                slackTask.ts
  //              })
  //            } else {
  //              //TODO: read from queue
  //              fields.nextTs.filterNot(_ == slackTask.ts)
  //            }
  //            Fields(
  //              name = slackTask.name,
  //              description = slackTask.description,
  //              nextTs = nextTs,
  //              lastSuccess = lastSuccess,
  //              lastFailure = lastFailure
  //            )
  //          } else {
  //            fields
  //          }
  //      }
  //    } else {
  //      val (lastSuccess, lastFailure) = instance.state match {
  //        case State.Scheduled => (None, None)
  //        case State.Success => (Some(instance), None)
  //        case State.Failure => (None, Some(instance))
  //      }
  //      existing :+ Fields(
  //        name = slackTask.name,
  //        description = slackTask.description,
  //        nextTs = None,
  //        lastSuccess = lastSuccess,
  //        lastFailure = lastFailure
  //      )
  //    }
  //    updatedFields.sorted
  //  }
}
