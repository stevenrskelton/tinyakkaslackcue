package ca.stevenskelton.tinyakkaslackqueue

import ca.stevenskelton.tinyakkaslackqueue.blocks.TaskHistoryItem
import com.slack.api.methods.response.chat.ChatPostMessageResponse
import play.api.libs.json.{JsValue, Json}

object SlackHistoryThread {

  private def toMessage(jsValue: JsValue): String = {
    s"```${Json.stringify(jsValue)}```"
  }

  def schedule(slackTask: ScheduledSlackTask)(implicit slackFactories: SlackFactories): ChatPostMessageResponse = {
    //TODO: can we quote the task thread
    val json = Json.obj(
      "name" -> slackTask.task.name.getText,
      "ts" -> slackTask.id,
      "executionStart" -> slackTask.executionStart,
      "createdBy" -> slackTask.task.createdBy,
      "notifyOnError" -> slackTask.task.notifyOnError,
      "notifyOnComplete" -> slackTask.task.notifyOnComplete
    )
    slackFactories.slackClient.chatPostMessageInThread(toMessage(json), slackFactories.slackClient.historyThread)
  }

  def history(taskHistoryItem: TaskHistoryItem)(implicit slackFactories: SlackFactories): ChatPostMessageResponse = {
    val json = Json.toJson(taskHistoryItem)
    slackFactories.slackClient.chatPostMessageInThread(toMessage(json), slackFactories.slackClient.historyThread)
  }

}
