package ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory

import ca.stevenskelton.tinyakkaslackqueue.{SlackChannel, SlackTs}
import play.api.libs.json.{Format, JsObject, JsResult, JsValue, Json, Reads, Writes}

import java.time.ZonedDateTime

object TaskHistoryItem {
  implicit val ordering = new Ordering[TaskHistoryItem[_]] {
    override def compare(x: TaskHistoryItem[_], y: TaskHistoryItem[_]): Int = x.time.compareTo(y.time)
  }
  def reads(ts: SlackTs, threadTs: SlackTs, slackChannel: SlackChannel, time: ZonedDateTime): Reads[TaskHistoryItem[_]] = {
//    val ts = SlackTs.Empty
//    val threadTs = SlackTs.Empty
//    val slackChannel = SlackChannel("")
//    val time = ZonedDateTime.now()

    (json: JsValue) => {
      (json \ "action").as[String] match {
        case "cancel" =>
          val format = CancelHistoryItem.fmt
          format.reads(json).map {
            action => TaskHistoryItem(action, ts, threadTs, slackChannel, time)
          }
        case "create" =>
          val format = CreateHistoryItem.fmt
          format.reads(json).map {
            action => TaskHistoryItem(action, ts, threadTs, slackChannel, time)
          }
        case "run" =>
          val format = RunHistoryItem.fmt
          format.reads(json).map {
            action => TaskHistoryItem(action, ts, threadTs, slackChannel, time)
          }
        case "success" =>
          val format = SuccessHistoryItem.fmt
          format.reads(json).map {
            action => TaskHistoryItem(action, ts, threadTs, slackChannel, time)
          }
      }
    }
  }
  implicit val write = new Writes[TaskHistoryItem[_]]{
    override def writes(o: TaskHistoryItem[_]): JsObject = o match {
      case o: CancelHistoryItem => Json.obj("action" -> "cancel") ++ CancelHistoryItem.fmt.writes(o)
      case o: CreateHistoryItem => Json.obj("action" -> "create") ++ CreateHistoryItem.fmt.writes(o)
      case o: RunHistoryItem => Json.obj("action" -> "run") ++ RunHistoryItem.fmt.writes(o)
      case o: SuccessHistoryItem => Json.obj("action" -> "success") ++ SuccessHistoryItem.fmt.writes(o)
    }
  }
}

trait TaskHistoryActionItem {}

case class TaskHistoryItem[T <: TaskHistoryActionItem](
                                                        action: T,
                                                        ts: SlackTs,
                                                        threadTs: SlackTs,
                                                        channel: SlackChannel,
                                                        time: ZonedDateTime
                                                      )(implicit fmt: Format[T])
