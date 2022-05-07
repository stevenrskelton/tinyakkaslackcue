package ca.stevenskelton.tinyakkaslackcue.blocks

import ca.stevenskelton.tinyakkaslackcue._
import ca.stevenskelton.tinyakkaslackcue.util.DateUtils
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

  def parse(messageItem: MessageItem, slackClient: SlackClient, slackTaskFactories: SlackTaskFactories): Option[(SlackTask, Fields)] = Try {
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

  def placeholderThread(taskName: String): String = {
    s"Scheduling task *$taskName*"
  }

  def schedule(scheduledTask: InteractiveJavaUtilTimer[SlackTask]#ScheduledTask): SlackBlocksAsString = {

    val createdByUser = "@Steven Skelton"
    val scheduledTime = scheduledTask.executionStart.format(DateTimeFormatter.ofPattern("YYYY-mm-dd hh:MM"))

    SlackBlocksAsString(
      s"""{
    "type": "header",
    "text": {
      "type": "plain_text",
      "text": "$HeaderPreamble${scheduledTask.task.name}",
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

  def update(slackTask: SlackTask, percentComplete: Float, startTimeMs: Long): String = {
    val duration = Duration.ofMillis(System.currentTimeMillis - startTimeMs)
    val bar = s"|${TextProgressBar.SlackEmoji.bar(percentComplete, 14)}| ${("  " + math.round(percentComplete * 100)).takeRight(3)}%"
    val elapsed = if (startTimeMs != 0) s"\nStarted ${DateUtils.humanReadable(duration)} ago" else ""
    s"Running *${slackTask.name}*\n$bar$elapsed"
  }

  def cancelled(slackTask: SlackTask, startTimeMs: Long): String = {
    val duration = Duration.ofMillis(System.currentTimeMillis - startTimeMs)
    s":headstone: Cancelled *${slackTask.name}* after ${DateUtils.humanReadable(duration)}"
  }

  def completed(slackTask: SlackTask, startTimeMs: Long): String = {
    val duration = Duration.ofMillis(System.currentTimeMillis - startTimeMs)
    s":doughnut: Completed *${slackTask.name}* in ${DateUtils.humanReadable(duration)}"
  }

}
