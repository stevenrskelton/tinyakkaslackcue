package ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory

import ca.stevenskelton.tinyakkaslackqueue.SlackTask
import ca.stevenskelton.tinyakkaslackqueue.lib.SlackTaskFactory
import ca.stevenskelton.tinyakkaslackqueue.timer.TextProgressBar
import ca.stevenskelton.tinyakkaslackqueue.util.DateUtils

import java.time.{Duration, ZonedDateTime}

object SlackTaskThread {

  private val HeaderPreamble = "Scheduled Task "
  private val CreatedByPreamble = "*Created by* "
  private val ScheduledForPreamble = "*Scheduled for* "

  case class Fields(
                     name: String,
                     scheduledTime: ZonedDateTime,
                     createdBy: String,
                     notifyOnError: Seq[String],
                     notifyOnComplete: Seq[String]
                   )

  def placeholderThread(slackTaskIdentifier: SlackTaskFactory): String = {
    s"Scheduling task *${slackTaskIdentifier.name.getText}*"
  }

  //  def schedule(scheduledTask: ScheduledSlackTask, slackTaskMeta: SlackTaskMeta): SlackBlocksAsString = {
  //
  //    val createdByUser = "@Steven Skelton"
  //    val scheduledTime = scheduledTask.executionStart.format(DateTimeFormatter.ofPattern("YYYY-mm-dd hh:MM"))
  //
  //    SlackBlocksAsString(
  //      s"""{
  //    "type": "header",
  //    "text": {
  //      "type": "plain_text",
  //      "text": "$HeaderPreamble${slackTaskMeta.factory.name.getText}",
  //      "emoji": true
  //    }
  //	},{
  //    "type": "section",
  //    "text": {
  //      "type": "mrkdwn",
  //      "text": "$CreatedByPreamble$createdByUser\\n$ScheduledForPreamble$scheduledTime"
  //    },
  //    "accessory": {
  //      "type": "button",
  //      "text": {
  //        "type": "plain_text",
  //        "text": "Cancel",
  //        "emoji": true
  //      },
  //      "style": "danger",
  //      "value": "public-relations"
  //    }
  //  }""")
  //  }

  def update(slackTask: SlackTask, executionStart: ZonedDateTime): String = {
    update(slackTask, slackTask.percentComplete, executionStart.toEpochSecond, width = 40)
  }

  def update(slackTask: SlackTask, percentComplete: Float, startTimeMs: Long, width: Int = 14): String = {
    val duration = Duration.ofMillis(System.currentTimeMillis - startTimeMs)
    val bar = s"|${TextProgressBar.SlackEmoji.bar(percentComplete, width)}| ${("  " + math.round(percentComplete * 100)).takeRight(3)}%"
    val elapsed = if (startTimeMs != 0) s"\nStarted ${DateUtils.humanReadable(duration)} ago" else ""
    s"Running *${slackTask.meta.factory.name.getText}*\n$bar$elapsed"
  }

  def cancelled(slackTask: SlackTask, startTimeMs: Long): String = {
    val duration = Duration.ofMillis(System.currentTimeMillis - startTimeMs)
    s":headstone: Cancelled *${slackTask.meta.factory.name.getText}* after ${DateUtils.humanReadable(duration)}"
  }

  def completed(slackTask: SlackTask, startTimeMs: Long): String = {
    val duration = Duration.ofMillis(System.currentTimeMillis - startTimeMs)
    s":doughnut: Completed *${slackTask.meta.factory.name.getText}* in ${DateUtils.humanReadable(duration)}"
  }

}
