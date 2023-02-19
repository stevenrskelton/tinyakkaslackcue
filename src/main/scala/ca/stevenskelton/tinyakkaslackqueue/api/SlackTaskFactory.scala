package ca.stevenskelton.tinyakkaslackqueue.api

import akka.stream.UniqueKillSwitch
import akka.stream.scaladsl.Source
import ca.stevenskelton.tinyakkaslackqueue.util.DateUtils
import ca.stevenskelton.tinyakkaslackqueue.views.task.TaskOptionInput
import ca.stevenskelton.tinyakkaslackqueue.{SlackPayload, SlackTaskInit}
import com.slack.api.model.block.composition.MarkdownTextObject
import com.typesafe.config.Config
import org.slf4j.Logger

import java.time.{LocalDateTime, ZoneId, ZonedDateTime}
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

  /**
   * Returns the next scheduled execution of this event, if applicable
   */
  def nextRunDate(config: Config)(implicit logger: Logger): Option[ZonedDateTime] = {
    val className = getClass.getName
    val name = className.drop(className.lastIndexOf(".")).replace(".", "").toLowerCase
    val path = s"tinyakkaslackqueue.$name"
    if(config.hasPath(path)) {
      val taskConfig = config.getConfig(path)
      val schedule = ScheduleConfiguration(taskConfig)
      if (schedule.isEmpty) {
        logger.info(s"Schedule empty for task $name")
        None
      } else {
        logger.info(s"Schedule for task $name: ${ScheduleConfiguration.stringify(schedule)}")
        Some(ScheduleConfiguration.next(ZonedDateTime.now(DateUtils.NewYorkZoneId), schedule))
      }
    } else {
      logger.info(s"No schedule for task $name")
      None
    }
  }

}


