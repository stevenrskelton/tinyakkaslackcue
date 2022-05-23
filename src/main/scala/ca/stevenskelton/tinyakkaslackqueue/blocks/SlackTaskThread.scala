package ca.stevenskelton.tinyakkaslackqueue.blocks

import ca.stevenskelton.tinyakkaslackqueue._
import ca.stevenskelton.tinyakkaslackqueue.util.DateUtils
import com.slack.api.methods.response.pins.PinsListResponse.MessageItem
import com.slack.api.model.block.{HeaderBlock, SectionBlock}

import java.time.format.DateTimeFormatter
import java.time.{Duration, ZonedDateTime}
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.Try

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

  def parse(messageItem: MessageItem, slackTaskFactories: SlackFactories): Option[(SlackTask, Fields)] = Try {
    val created = messageItem.getCreated
    val message = messageItem.getMessage
    val blocks = message.getBlocks.asScala.toList
    val header = blocks(0).asInstanceOf[HeaderBlock]
    val taskName = header.getText.getText.drop(HeaderPreamble.length)
    slackTaskFactories.factories.find(_.name == taskName).map {
      slackFactory =>
        val slackTask = slackFactory.create(SlackTs(message),
          SlackUserId.Empty, notifyOnError = Nil, notifyOnComplete = Nil
        )

        val section = blocks(1).asInstanceOf[SectionBlock]
        val sectionFields = section.getText.getText.split("\n")
        val createdByRaw = sectionFields(0)
        val scheduledForRaw = sectionFields(1)

        val createdBy = createdByRaw.drop(CreatedByPreamble.length)
        val scheduledFor = scheduledForRaw.drop(ScheduledForPreamble.length)
        (slackTask, Fields(taskName, ZonedDateTime.now(), "", Nil, Nil))
    }
  }.toOption.flatten

  def placeholderThread(slackTaskIdentifier: SlackTaskIdentifier): String = {
    s"Scheduling task *${slackTaskIdentifier.name.getText}*"
  }

  def schedule(scheduledTask: ScheduledSlackTask): SlackBlocksAsString = {

    val createdByUser = "@Steven Skelton"
    val scheduledTime = scheduledTask.executionStart.format(DateTimeFormatter.ofPattern("YYYY-mm-dd hh:MM"))

    SlackBlocksAsString(
      s"""{
    "type": "header",
    "text": {
      "type": "plain_text",
      "text": "$HeaderPreamble${scheduledTask.task.name.getText}",
      "emoji": true
    }
	},{
    "type": "section",
    "text": {
      "type": "mrkdwn",
      "text": "$CreatedByPreamble$createdByUser\\n$ScheduledForPreamble$scheduledTime"
    },
    "accessory": {
      "type": "button",
      "text": {
        "type": "plain_text",
        "text": "Cancel",
        "emoji": true
      },
      "style": "danger",
      "value": "public-relations"
    }
  }""")
  }

  def update(scheduledTask: ScheduledSlackTask): String = {
    update(scheduledTask.task, scheduledTask.task.percentComplete, scheduledTask.executionStart.toEpochSecond, width = 40)
  }

  def update(slackTask: SlackTask, percentComplete: Float, startTimeMs: Long, width: Int = 14): String = {
    val duration = Duration.ofMillis(System.currentTimeMillis - startTimeMs)
    val bar = s"|${TextProgressBar.SlackEmoji.bar(percentComplete, width)}| ${("  " + math.round(percentComplete * 100)).takeRight(3)}%"
    val elapsed = if (startTimeMs != 0) s"\nStarted ${DateUtils.humanReadable(duration)} ago" else ""
    s"Running *${slackTask.name.getText}*\n$bar$elapsed"
  }

  def cancelled(slackTask: SlackTask, startTimeMs: Long): String = {
    val duration = Duration.ofMillis(System.currentTimeMillis - startTimeMs)
    s":headstone: Cancelled *${slackTask.name.getText}* after ${DateUtils.humanReadable(duration)}"
  }

  def completed(slackTask: SlackTask, startTimeMs: Long): String = {
    val duration = Duration.ofMillis(System.currentTimeMillis - startTimeMs)
    s":doughnut: Completed *${slackTask.name.getText}* in ${DateUtils.humanReadable(duration)}"
  }

}
