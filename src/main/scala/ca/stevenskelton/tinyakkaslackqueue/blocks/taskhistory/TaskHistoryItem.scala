package ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory

import ca.stevenskelton.tinyakkaslackqueue.{SlackChannel, SlackTs}
import com.slack.api.model.Message
import org.slf4j.Logger
import play.api.libs.json._

import java.time.ZonedDateTime
import scala.util.control.NonFatal

object TaskHistoryItem {
  implicit val ordering = new Ordering[TaskHistoryItem[TaskHistoryOutcomeItem]] {
    override def compare(x: TaskHistoryItem[TaskHistoryOutcomeItem], y: TaskHistoryItem[TaskHistoryOutcomeItem]): Int = x.time.compareTo(y.time)
  }

  def fromMessage(message:Message, ts: SlackTs, threadTs: SlackTs, slackChannel: SlackChannel)(implicit logger:Logger): Option[TaskHistoryItem[_]] = {
    try {
      val createdText = message.getItem.getCreated
      val createdBy = message.getItem.getUser
      implicit val reads = TaskHistoryItem.reads(ts, threadTs, slackChannel, ZonedDateTime.now())
      val text = message.getText
      if (text.startsWith("```")) {
        val json = Json.parse(text.drop(3).dropRight(3))
        reads.reads(json) match {
          case JsSuccess(taskHistoryItem, _) => Some(taskHistoryItem)
          case JsError(ex) =>
            logger.error("TaskHistoryItem.reads", ex)
            None
        }
      }else{
        None
      }
    } catch {
      case NonFatal(ex) =>
        logger.error(s"SlackTaskMeta.initialize: ${message.getText}", ex)
        None
    }
  }

  def reads(ts: SlackTs, threadTs: SlackTs, slackChannel: SlackChannel, time: ZonedDateTime): Reads[TaskHistoryItem[_]] = {
    (json: JsValue) => {
      (json \ "action").as[String] match {
        case CancelHistoryItem.Action =>
          val format = CancelHistoryItem.fmt
          format.reads(json).map {
            action => TaskHistoryItem(action, ts, threadTs, slackChannel, time)
          }
        case CreateHistoryItem.Action =>
          val format = CreateHistoryItem.fmt
          format.reads(json).map {
            action => TaskHistoryItem(action, ts, threadTs, slackChannel, time)
          }
        case RunHistoryItem.Action =>
          val format = RunHistoryItem.fmt
          format.reads(json).map {
            action => TaskHistoryItem(action, ts, threadTs, slackChannel, time)
          }
        case ErrorHistoryItem.Action =>
          val format = ErrorHistoryItem.fmt
          format.reads(json).map {
            action => TaskHistoryItem(action, ts, threadTs, slackChannel, time)
          }
        case SuccessHistoryItem.Action =>
          val format = SuccessHistoryItem.fmt
          format.reads(json).map {
            action => TaskHistoryItem(action, ts, threadTs, slackChannel, time)
          }
      }
    }
  }

  implicit val write = new Writes[TaskHistoryItem[TaskHistoryActionItem]] {
    override def writes(o: TaskHistoryItem[TaskHistoryActionItem]): JsObject = Json.obj("action" -> o.action.action) ++ o.toJson
  }
}

case class TaskHistoryItem[+T <: TaskHistoryActionItem](
                                                        action: T,
                                                        ts: SlackTs,
                                                        threadTs: SlackTs,
                                                        channel: SlackChannel,
                                                        time: ZonedDateTime
                                                      )(implicit fmt: OFormat[T]) {
  def toJson: JsObject = fmt.writes(action)
  def toSlackMessage: String = s"```${Json.prettyPrint(toJson)}```"
}
