package ca.stevenskelton.tinyakkaslackqueue.lib

import ca.stevenskelton.tinyakkaslackqueue.{SlackTask, SlackTs, SlackUserId}
import com.slack.api.model.block.composition.MarkdownTextObject

/**
 * Inherit to implement Akka Stream tasks, typed as a SlackTask
 */
trait SlackTaskFactory {

  /**
   * A brief name for this task.
   * Will be used to create a Slack channel (modified to be a valid Slack channel name).
   * Markdown is supported, see https://api.slack.com/reference/surfaces/formatting#basics
   */
  def name: MarkdownTextObject

  /**
   * A longer description for the task.
   * Markdown is supported, see https://api.slack.com/reference/surfaces/formatting#basics
   */
  def description: MarkdownTextObject

  /**
   * Create an Akka Stream task
   * @param slackTaskMeta
   * @param ts
   * @param createdBy
   * @param notifyOnError
   * @param notifyOnComplete
   * @return
   */
  def create(slackTaskMeta: SlackTaskMeta, ts: SlackTs, createdBy: SlackUserId, notifyOnError: Seq[SlackUserId], notifyOnComplete: Seq[SlackUserId]): SlackTask

  /**
   *  Helper class to create `name` and `description`
   */
  protected def createMarkdownText(value: String): MarkdownTextObject = MarkdownTextObject.builder().text(value).build()
}


