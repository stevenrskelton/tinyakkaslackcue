package ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory

import ca.stevenskelton.tinyakkaslackqueue.{SlackQueueThread, SlackTaskThread, SlackTs, TaskLogChannel}
import com.slack.api.model.Message
import org.slf4j.Logger
import play.api.libs.json._

import java.time.ZonedDateTime
import scala.util.control.NonFatal

object TaskHistoryItem {
  implicit val ordering: Ordering[TaskHistoryItem[TaskHistoryOutcomeItem]] = new Ordering[TaskHistoryItem[TaskHistoryOutcomeItem]] {
    override def compare(x: TaskHistoryItem[TaskHistoryOutcomeItem], y: TaskHistoryItem[TaskHistoryOutcomeItem]): Int = x.time.compareTo(y.time)
  }

  def fromHistoryThreadMessage(message: Message, taskChannel: TaskLogChannel, historyThread: SlackQueueThread)(implicit logger: Logger): Option[TaskHistoryItem[_]] = {
    try {
      implicit val reads = TaskHistoryItem.reads(taskChannel, historyThread, ZonedDateTime.now())
      val text = message.getText
      if (text.startsWith("```")) {
        val json = Json.parse(text.drop(3).dropRight(3))
        reads.reads(json) match {
          case JsSuccess(taskHistoryItem, _) => Some(taskHistoryItem)
          case JsError(ex) =>
            logger.error("TaskHistoryItem.reads", ex)
            None
        }
      } else {
        None
      }
    } catch {
      case NonFatal(ex) =>
        logger.error(s"SlackTaskMeta.initialize: ${message.getText}", ex)
        None
    }
  }

  def reads(taskLogChannel: TaskLogChannel, historyThreadTs: SlackQueueThread, time: ZonedDateTime): Reads[TaskHistoryItem[_]] = {
    (json: JsValue) => {
      val slackTaskThreadTs = new SlackTaskThread(SlackTs((json \ "ts").as[String]), taskLogChannel)
      (json \ "action").as[String] match {
        case CancelHistoryItem.Action =>
          val format = CancelHistoryItem.fmt
          format.reads(json).map {
            action => TaskHistoryItem(action, slackTaskThreadTs, historyThreadTs, time)
          }
        case CreateHistoryItem.Action =>
          val format = CreateHistoryItem.fmt
          format.reads(json).map {
            action => TaskHistoryItem(action, slackTaskThreadTs, historyThreadTs, time)
          }
        case RunHistoryItem.Action =>
          val format = RunHistoryItem.fmt
          format.reads(json).map {
            action => TaskHistoryItem(action, slackTaskThreadTs, historyThreadTs, time)
          }
        case ErrorHistoryItem.Action =>
          val format = ErrorHistoryItem.fmt
          format.reads(json).map {
            action => TaskHistoryItem(action, slackTaskThreadTs, historyThreadTs, time)
          }
        case CancelledHistoryItem.Action =>
          val format = CancelledHistoryItem.fmt
          format.reads(json).map {
            action => TaskHistoryItem(action, slackTaskThreadTs, historyThreadTs, time)
          }
        case SuccessHistoryItem.Action =>
          val format = SuccessHistoryItem.fmt
          format.reads(json).map {
            action => TaskHistoryItem(action, slackTaskThreadTs, historyThreadTs, time)
          }
      }
    }
  }
}

case class TaskHistoryItem[+T <: TaskHistoryActionItem](
                                                         action: T,
                                                         taskId: SlackTaskThread,
                                                         historyThread: SlackQueueThread,
                                                         time: ZonedDateTime
                                                       )(implicit fmt: OFormat[T]) {
  def toHistoryThreadMessage: String = {
    val common = Json.obj("action" -> action.action, "ts" -> taskId.ts.value)
    s"```${Json.prettyPrint(common ++ fmt.writes(action))}```"
  }

  def toTaskThreadMessage: String = s"Task History ${action.action}"
}
