package ca.stevenskelton.tinyakkaslackcue.blocks

import akka.Done
import ca.stevenskelton.tinyakkaslackcue._
import com.slack.api.methods.SlackApiTextResponse
import org.slf4j.Logger
import play.api.libs.json.{JsObject, Json}

import java.time.{ZoneId, ZonedDateTime}
import java.util.UUID
import scala.concurrent.Future

object HomeTab {

  def update(slackUserId: SlackUserId, slackTaskFactories: SlackTaskFactories)(implicit logger: Logger): Future[Done] = {
    //TODO: sort
    val slackView = SlackView.createHomeTab(slackTaskFactories.history)
    val response = slackTaskFactories.slackClient.viewsPublish(slackUserId, slackView)
    if (response.isOk) {
      logger.debug(s"Updated home view for ${slackUserId.value}")
      Future.successful(Done)
    } else {
      logger.error(s"Home view update failed: ${response.getError}")
      logger.error(s"\n```${slackView.toString}```\n")
      Future.failed(new Exception(response.getError))
    }
  }

  def openedEvent(slackTaskFactories: SlackTaskFactories, jsObject: JsObject)(implicit logger: Logger): Future[Done] = {
    val slackUserId = SlackUserId((jsObject \ "user").as[String])
    update(slackUserId, slackTaskFactories)
  }

  def handleSubmission(slackPayload: SlackPayload)(implicit slackTaskFactories: SlackTaskFactories, logger: Logger): Future[Done] = {
    slackPayload.callbackId.get match {
      case CallbackId.View =>
        if(slackPayload.actionStates.get(ActionId.TaskCancel).map(o => UUID.fromString(o.asInstanceOf[DatePickerState].value.toString)).fold(false)(slackTaskFactories.tinySlackCue.cancelScheduledTask(_))){
          HomeTab.update(slackPayload.user.id, slackTaskFactories)
        } else {
          val ex = new Exception(s"Could not find task uuid ${slackPayload.privateMetadata.get.value}")
          logger.error("handleSubmission", ex)
          Future.failed(ex)
        }
      case CallbackId.Create =>
        slackTaskFactories.findByPrivateMetadata(slackPayload.privateMetadata.get).map {
          taskFactory =>
            val zonedDateTimeOpt = for{
              scheduledDate <- slackPayload.actionStates.get(ActionId.ScheduleDate).map(_.asInstanceOf[DatePickerState].value)
              scheduledTime <- slackPayload.actionStates.get(ActionId.ScheduleTime).map(_.asInstanceOf[TimePickerState].value)
            } yield scheduledDate.atTime(scheduledTime).atZone(ZoneId.systemDefault())
            //TODO zone should be from Slack

            val slackTask = slackTaskFactories.tinySlackCue.scheduleSlackTask(taskFactory, zonedDateTimeOpt)
            val msg = zonedDateTimeOpt.fold("Queued")(_ => "Scheduled")
            //TODO: can we quote the task thread
            slackTaskFactories.slackClient.chatPostMessageInThread(s"$msg ${slackTask.name}", slackTaskFactories.slackClient.historyThread)

            HomeTab.update(slackPayload.user.id, slackTaskFactories)
          //        actionStates(ActionIdNotifyOnComplete).asInstanceOf[MultiUsersState].users shouldBe Seq(SlackUserId("U039T9DUHGT"))
          //        actionStates(ActionIdNotifyOnFailure).asInstanceOf[MultiUsersState].users shouldBe Seq(SlackUserId("U039T9DUHGT"), SlackUserId("U039TCGLX5G"))
          //        actionStates(ActionIdLogLevel)

        }.getOrElse {
          val ex = new Exception(s"Could not find task ${slackPayload.privateMetadata.get.value}")
          logger.error("handleSubmission", ex)
          Future.failed(ex)
        }
      case CallbackId(callbackId) =>
        val ex = new Exception(s"Could not find callback id $callbackId")
        logger.error("handleSubmission", ex)
        Future.failed(ex)
    }
  }

  def handleAction(slackTriggerId: SlackTriggerId, slackUser: SlackUser, jsObject: JsObject)(implicit slackClient: SlackClient, slackTaskFactories: SlackTaskFactories, logger: Logger): Future[Done] = {
    logger.info(Json.stringify(jsObject))

    val slackActions: Seq[SlackAction] = (jsObject \ "actions").as[Seq[SlackAction]]
    val action = slackActions.head
    val view = action.actionId match {
      case ActionId.TaskQueue =>
        ScheduleActionModal.createModal(slackUser, action.value, None, PrivateMetadata(action.value))
      case ActionId.TaskSchedule =>
        ScheduleActionModal.createModal(slackUser,action.value, Some(ZonedDateTime.now()), PrivateMetadata(action.value))
      case ActionId.TaskCancel =>
        ScheduleActionModal.createModal(slackUser,action.value, None, PrivateMetadata(action.value))
      case ActionId.TaskView =>
        val uuid = action.value
        slackTaskFactories.tinySlackCue.listScheduledTasks.find(_.uuid.toString == uuid).map {
          scheduledTask => ScheduleActionModal.viewModal(scheduledTask)
        }.getOrElse {
          val ex = new Exception(s"Task UUID $uuid not found")
          logger.error("handleAction", ex)
          throw ex
        }
//      case ActionIdTaskThread =>
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
