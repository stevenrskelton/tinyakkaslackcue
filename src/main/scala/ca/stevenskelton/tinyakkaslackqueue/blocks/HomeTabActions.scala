package ca.stevenskelton.tinyakkaslackqueue.blocks

import akka.Done
import ca.stevenskelton.tinyakkaslackqueue._
import ca.stevenskelton.tinyakkaslackqueue.api.SlackFactories
import ca.stevenskelton.tinyakkaslackqueue.views._
import com.slack.api.methods.SlackApiTextResponse
import org.slf4j.Logger

import java.time.{ZoneId, ZonedDateTime}
import scala.concurrent.Future

object HomeTabActions {

  def update(slackPayload: SlackPayload)(implicit logger: Logger, slackTaskFactories: SlackFactories): Future[Done] = {
    openedEvent(slackPayload.user.id)
  }

  def openedEvent(slackUserId: SlackUserId)(implicit logger: Logger, slackTaskFactories: SlackFactories): Future[Done] = {
    //TODO: sort
    val slackView = new HomeTab(slackTaskFactories.history)
    val response = slackTaskFactories.slackClient.viewsPublish(slackUserId, slackView)
    if (response.isOk) {
      logger.debug(s"Created home view for ${slackUserId.value}")
      Future.successful(Done)
    } else {
      logger.error(s"Home view creation failed: ${response.getError}")
      logger.error(s"\n```${slackView.toString}```\n")
      Future.failed(new Exception(response.getError))
    }
  }

  def handleSubmission(slackPayload: SlackPayload)(implicit slackTaskFactories: SlackFactories, logger: Logger): Future[Done] = {
    slackPayload.callbackId.getOrElse("") match {
      case CallbackId.View =>
        if (slackPayload.actionStates.get(ActionId.TaskCancel).map(o => SlackTaskThreadTs(o.asInstanceOf[DatePickerState].value.toString)).fold(false)(slackTaskFactories.cancelScheduledTask(_).isDefined)) {
          HomeTabActions.update(slackPayload)
        } else {
          val ex = new Exception(s"Could not find task ts ${slackPayload.privateMetadata.fold("")(_.value)}")
          logger.error("handleSubmission", ex)
          Future.failed(ex)
        }
      case CallbackId.Create =>
        //        val action = slackPayload.action
        //        action.actionId match {
        //          case ActionId.TaskCancel =>
        //            cancelTask()
        //          case _ =>
        slackTaskFactories.findByPrivateMetadata(slackPayload.privateMetadata.getOrElse(PrivateMetadata.Empty)).map {
          slackTaskMeta =>
            val zonedDateTimeOpt = for {
              scheduledDate <- slackPayload.actionStates.get(ActionId.ScheduleDate).map(_.asInstanceOf[DatePickerState].value)
              scheduledTime <- slackPayload.actionStates.get(ActionId.ScheduleTime).map(_.asInstanceOf[TimePickerState].value)
            } yield scheduledDate.atTime(scheduledTime).atZone(ZoneId.systemDefault())
            //TODO zone should be from Slack

            val scheduledSlackTask = slackTaskFactories.scheduleSlackTask(slackTaskMeta, zonedDateTimeOpt)
            HomeTabActions.update(slackPayload)
        }.getOrElse {
          val ex = new Exception(s"Could not find task ${slackPayload.privateMetadata.fold("")(_.value)}")
          logger.error("handleSubmission", ex)
          Future.failed(ex)
          //            }
        }
      case CallbackId(callbackId) =>
        val ex = new Exception(s"Could not find callback id $callbackId")
        logger.error("handleSubmission", ex)
        Future.failed(ex)
    }
  }

  def handleAction(slackPayload: SlackPayload)(implicit slackTaskFactories: SlackFactories, logger: Logger): Future[Done] = {
    val action = slackPayload.action
    val view: SlackView = action.actionId match {
      case ActionId.TaskQueue =>
        val privateMetadata = PrivateMetadata(action.value)
        slackTaskFactories.findByPrivateMetadata(privateMetadata).map {
          new CreateTaskModal(slackPayload.user, _, None, privateMetadata)
        }.getOrElse {
          val ex = new Exception(s"Task ${action.value} not found")
          logger.error("handleAction", ex)
          throw ex
        }
      case ActionId.TaskSchedule =>
        val privateMetadata = PrivateMetadata(action.value)
        slackTaskFactories.findByPrivateMetadata(privateMetadata).map {
          new CreateTaskModal(slackPayload.user, _, Some(ZonedDateTime.now()), privateMetadata)
        }.getOrElse {
          val ex = new Exception(s"Task ${action.value} not found")
          logger.error("handleAction", ex)
          throw ex
        }
      case ActionId.TaskView =>
        val ts = SlackTaskThreadTs(action.value)
        val list = slackTaskFactories.listScheduledTasks
        val index = list.indexWhere(_.id == ts)
        if (index == -1) {
          val ex = new Exception(s"Task ts $ts not found")
          logger.error("handleAction", ex)
          throw ex
        } else {
          new ViewTaskModal(list, index)
        }
    }
    val result: SlackApiTextResponse = slackTaskFactories.slackClient.viewsOpen(slackPayload.triggerId, view)
    if (!result.isOk) {
      logger.debug(s"\n```$view```\n")
      logger.error(result.getError)
    }
    Future.successful(Done)
  }

  def cancelTask(ts: SlackTaskThreadTs, slackPayload: SlackPayload)(implicit slackFactories: SlackFactories, logger: Logger): Future[Done] = {
    slackFactories.cancelScheduledTask(ts).map {
      cancelledTask =>
        logger.error("Cancelled Task")
        val view = new CancelTaskModal(cancelledTask)
        val update = slackFactories.slackClient.viewsUpdate(slackPayload.viewId, view)
        if (!update.isOk) {
          logger.error(view.toString)
          logger.error(update.getError)
        }
        HomeTabActions.update(slackPayload)
    }.getOrElse {
      val ex = new Exception(s"Could not find task ts ${ts.value}")
      logger.error("handleSubmission", ex)
      Future.failed(ex)
    }
  }
}
