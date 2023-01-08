package ca.stevenskelton.tinyakkaslackqueue.api

import akka.stream.UniqueKillSwitch
import akka.stream.scaladsl.Source
import ca.stevenskelton.tinyakkaslackqueue.views.task.TaskOptionInput
import ca.stevenskelton.tinyakkaslackqueue.{SlackPayload, SlackTaskInit}
import com.slack.api.model.block.composition.MarkdownTextObject
import org.slf4j.Logger

import scala.concurrent.Future

/**
 * Inherit to implement Akka Stream tasks, typed as a SlackTask
 *
 * @tparam T Source will create items of type T
 * @tparam B If T should be grouped together, for example by T.b, then set B != T
 */
trait SlackTaskFactory[T, B] extends SlackTaskInit[T, B] {

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
   * Group T together by field B
   */
  def distinctBy: T => B

  /**
   * Create a source of T.
   * Use `.async.viaMat(KillSwitches.single)(Keep.right)` to add an kill switch
   */
  def sourceAndCount: (SlackPayload, Logger) => (Source[T, UniqueKillSwitch], Future[Int])

  /**
   * Input options completed by user on creation
   *
   * @return
   */
  def taskOptions(slackPayload: SlackPayload): Seq[TaskOptionInput]

  /**
   * Helper class to create `name` and `description`
   */
  protected def createMarkdownText(value: String): MarkdownTextObject = MarkdownTextObject.builder().text(value).build()

}


