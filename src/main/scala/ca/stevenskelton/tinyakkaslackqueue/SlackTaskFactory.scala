package ca.stevenskelton.tinyakkaslackqueue

import com.slack.api.model.block.composition.MarkdownTextObject

trait SlackTaskIdentifier {
  def name: MarkdownTextObject
  def description: MarkdownTextObject
}

trait SlackTaskFactory extends SlackTaskIdentifier {
  def create(ts: SlackTs, createdBy: SlackUserId, notifyOnError: Seq[SlackUserId], notifyOnComplete: Seq[SlackUserId]): SlackTask
}