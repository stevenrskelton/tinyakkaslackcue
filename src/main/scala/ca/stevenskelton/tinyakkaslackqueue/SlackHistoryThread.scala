package ca.stevenskelton.tinyakkaslackqueue

import ca.stevenskelton.tinyakkaslackqueue.blocks.TaskHistoryItem
import com.slack.api.methods.response.chat.ChatPostMessageResponse
import com.slack.api.methods.response.pins.PinsListResponse.MessageItem
import com.slack.api.model.Message
import play.api.libs.json.{JsValue, Json}

import scala.jdk.CollectionConverters.ListHasAsScala
import scala.util.Try

object SlackHistoryThread {

  private def toMessage(jsValue: JsValue): String = {
    s"```${Json.stringify(jsValue)}```"
  }

  def schedule(slackTask: ScheduledSlackTask)(implicit slackFactories: SlackFactories): ChatPostMessageResponse = {
    //TODO: can we quote the task thread
    val json = Json.obj(
      "name" -> slackTask.task.meta.factory.name.getText,
      "ts" -> slackTask.task.id,
      "executionStart" -> slackTask.executionStart,
      "createdBy" -> slackTask.task.createdBy,
      "notifyOnError" -> slackTask.task.notifyOnError,
      "notifyOnComplete" -> slackTask.task.notifyOnComplete
    )
    slackFactories.slackClient.chatPostMessageInThread(toMessage(json), slackTask.task.meta.historyThread)
  }

  def history(taskHistoryItem: TaskHistoryItem)(implicit slackFactories: SlackFactories): ChatPostMessageResponse = {
    val json = Json.toJson(taskHistoryItem)
    val slackTaskMeta = slackFactories.slackTaskMetaFactories.find(_.channel == taskHistoryItem.channel).get
    slackFactories.slackClient.chatPostMessageInThread(toMessage(json), slackTaskMeta.historyThread)
  }

  def readHistory(messageItem: MessageItem)(implicit slackClient: SlackClient):Map[SlackTs, Message] = {
//    slackClient.threadReplies(messageItem).getMessages.asScala.flatMap {
//      message =>
//        Try {
//          val text = message.getText
//          if (text.startsWith("```") && text.endsWith("```")) {
//            val json = Json.parse(text.drop(3).dropRight(3))
//
//          } else {
//            None
//          }
//        }.toOption
//    }
    ???
  }

}
