package ca.stevenskelton.tinyakkaslackqueue

import ca.stevenskelton.tinyakkaslackqueue.SlackClient.ChannelThreadText
import com.slack.api.methods.request.chat.ChatPostMessageRequest
import com.slack.api.methods.request.pins.PinsAddRequest
import com.slack.api.model.Message
import com.slack.api.model.block.composition.MarkdownTextObject

trait SlackTaskIdentifier {
  def name: MarkdownTextObject
  def description: MarkdownTextObject
}

trait SlackTaskFactory extends SlackTaskIdentifier {
  def create(slackTaskMeta: SlackTaskMeta, ts: SlackTs, createdBy: SlackUserId, notifyOnError: Seq[SlackUserId], notifyOnComplete: Seq[SlackUserId]): SlackTask
}

case class SlackTaskMeta(channel: SlackChannel, historyThread: SlackTs, factory: SlackTaskFactory)
