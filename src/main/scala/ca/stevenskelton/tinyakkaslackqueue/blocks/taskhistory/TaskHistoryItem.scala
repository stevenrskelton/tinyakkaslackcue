package ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory

import ca.stevenskelton.tinyakkaslackqueue.{SlackHistoryThreadTs, SlackTaskThreadTs}
import com.slack.api.model.Message
import org.slf4j.Logger
import play.api.libs.json._

import java.time.ZonedDateTime
import scala.util.control.NonFatal

object TaskHistoryItem {
  implicit val ordering = new Ordering[TaskHistoryItem[TaskHistoryOutcomeItem]] {
    override def compare(x: TaskHistoryItem[TaskHistoryOutcomeItem], y: TaskHistoryItem[TaskHistoryOutcomeItem]): Int = x.time.compareTo(y.time)
  }

  def fromHistoryThreadMessage(message: Message)(implicit logger: Logger): Option[TaskHistoryItem[_]] = {
    try {
      val createdText = message.getItem.getCreated
      val createdBy = message.getItem.getUser
      implicit val reads = TaskHistoryItem.reads(SlackHistoryThreadTs(message), ZonedDateTime.now())
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

  def reads(historyThreadTs: SlackHistoryThreadTs, time: ZonedDateTime): Reads[TaskHistoryItem[_]] = {
    (json: JsValue) => {
      val slackTaskThreadTs = SlackTaskThreadTs((json \ "ts").as[String])
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
                                                         taskId: SlackTaskThreadTs,
                                                         historyThread: SlackHistoryThreadTs,
                                                         time: ZonedDateTime
                                                       )(implicit fmt: OFormat[T]) {
  def toHistoryThreadMessage: String = s"```${Json.prettyPrint(Json.obj("action" -> action.action) ++ fmt.writes(action))}```"
  def toTaskThreadMessage: String = s"Task History ${action.action}"
}
