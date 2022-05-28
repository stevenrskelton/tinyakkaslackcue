package ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory

import ca.stevenskelton.tinyakkaslackqueue.{SlackChannel, SlackTs}
import play.api.libs.json._

import java.time.ZonedDateTime

object TaskHistoryItem {
  implicit val ordering = new Ordering[TaskHistoryItem[TaskHistoryOutcomeItem]] {
    override def compare(x: TaskHistoryItem[TaskHistoryOutcomeItem], y: TaskHistoryItem[TaskHistoryOutcomeItem]): Int = x.time.compareTo(y.time)
  }

  def reads(ts: SlackTs, threadTs: SlackTs, slackChannel: SlackChannel, time: ZonedDateTime): Reads[TaskHistoryItem[_]] = {
    //    val ts = SlackTs.Empty
    //    val threadTs = SlackTs.Empty
    //    val slackChannel = SlackChannel("")
    //    val time = ZonedDateTime.now()

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

case class TaskHistoryItem[T <: TaskHistoryActionItem](
                                                        action: T,
                                                        ts: SlackTs,
                                                        threadTs: SlackTs,
                                                        channel: SlackChannel,
                                                        time: ZonedDateTime
                                                      )(implicit fmt: OFormat[T]) {
  def toJson: JsObject = fmt.writes(action)
}
