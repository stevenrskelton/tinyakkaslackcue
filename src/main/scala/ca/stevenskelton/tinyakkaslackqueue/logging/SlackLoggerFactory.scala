package ca.stevenskelton.tinyakkaslackqueue.logging

import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import ca.stevenskelton.tinyakkaslackqueue._
import ca.stevenskelton.tinyakkaslackqueue.api.SlackClient
import ca.stevenskelton.tinyakkaslackqueue.blocks.{TaskCancelled, TaskFailure, TaskRunning, TaskSuccess, logLevelEmoji}
import ca.stevenskelton.tinyakkaslackqueue.timer.TextProgressBar
import ca.stevenskelton.tinyakkaslackqueue.util.DateUtils
import com.slack.api.methods.request.chat.ChatPostMessageRequest
import org.slf4j.Logger
import org.slf4j.event.{Level, LoggingEvent}

import java.time.Duration
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration.DurationInt

object SlackLoggerFactory {

  def logEvent(loggingEvent: LoggingEvent): String = {
    //    val sMarker = Option(loggingEvent.getMarkers.asScala.hea.getMarker)
    val sArgs = Option(loggingEvent.getArgumentArray).getOrElse(Array.empty)
    val text = if (sArgs.isEmpty) loggingEvent.getMessage else String.format(loggingEvent.getMessage, sArgs)
    val emoji = logLevelEmoji(loggingEvent.getLevel)
    val exception = Option(loggingEvent.getThrowable).fold("")(ex => s"${ex.getMessage} ${ex.getStackTrace}")
    s"$emoji $text$exception"
  }

  def logToSlack(name: String, logLevel: Level, slackConfig: SlackClient.SlackConfig, backup: Option[Logger] = None, mirror: Option[Logger] = None)(implicit materializer: Materializer): SlackLogger = {
    val text = s"Log for $name"
    val slackThread = slackConfig.client.chatPostMessage((r: ChatPostMessageRequest.ChatPostMessageRequestBuilder) => r.token(slackConfig.botOAuthToken).channel(slackConfig.botChannel.id).text(text))
    val sink = Sink.foreach[Seq[LoggingEvent]] {
      loggingEvents =>
        val filteredLevelEvents = loggingEvents.filter(_.getLevel.compareTo(logLevel) >= 0)
        if (filteredLevelEvents.nonEmpty) {
          val message = filteredLevelEvents.map(logEvent).mkString("\n")
          slackConfig.client.chatPostMessage((r: ChatPostMessageRequest.ChatPostMessageRequestBuilder) =>
            r.token(slackConfig.botOAuthToken).channel(slackConfig.botChannel.id).text(message).threadTs(slackThread.getTs)
          )
        }
    }
    val sourceQueue = Source.queue[LoggingEvent](1, OverflowStrategy.fail).groupedWithin(1000, 5.seconds).to(sink).run()
    new SlackLogger(getName = name, sourceQueue, backup, mirror)
  }

  def createNewSlackThread(slackTask: SlackTask, mainLogger: Logger)(implicit slackClient: SlackClient, materializer: Materializer): SlackLogger = {

    val startTimeMs = System.currentTimeMillis

    val sink = Sink.foreach[Seq[LoggingEvent]] {
      loggingEvents =>
        if (loggingEvents.nonEmpty) {

          var percentCompleteEvent: Option[SlackUpdatePercentCompleteEvent] = None
          var exceptionEvent: Option[Throwable] = None
          val messageEvents = loggingEvents.flatMap {
            case loggingEvent: SlackUpdatePercentCompleteEvent =>
              percentCompleteEvent = Some(loggingEvent)
              None
            case loggingEvent: SlackExceptionEvent =>
              exceptionEvent = Some(loggingEvent.getThrowable)
              Some(logEvent(loggingEvent))
            case loggingEvent =>
              Some(logEvent(loggingEvent))
          }
          if (messageEvents.nonEmpty) {
            val message = messageEvents.mkString("\n")
            val posted = slackClient.chatPostMessageInThread(message, slackTask.slackTaskThread)
            if (!posted.isOk) {
              mainLogger.error(s"Could not post to Slack: ${posted.getChannel} : ${posted.getError}")
            }
          }
          exceptionEvent match {
            case Some(ex) if ex == SlackExceptionEvent.UserCancelledException =>
              slackClient.chatUpdate(cancelled(slackTask, startTimeMs), slackTask.slackTaskThread)
            case Some(ex) =>
              slackClient.chatUpdate(failed(slackTask, ex, startTimeMs), slackTask.slackTaskThread)
            //              slackClient.pinsRemove(slackTask.ts)
            case None =>
              percentCompleteEvent.foreach {
                event =>
                  slackClient.chatUpdate(update(slackTask, event.percent, startTimeMs), slackTask.slackTaskThread)
              }
          }
        }
    }
    val sourceQueue = Source.queue[LoggingEvent](1, OverflowStrategy.fail).groupedWithin(1000, 5.seconds).to(sink).run()
    sourceQueue.watchCompletion.map {
      _ =>
        slackClient.chatUpdate(completed(slackTask, startTimeMs), slackTask.slackTaskThread)
      //        slackClient.pinsRemove(slackTask.slackTaskThread)
    }
    new SlackLogger(getName = s"${slackTask.meta.factory.name.getText}-${slackTask.id.value}", sourceQueue, backup = Some(mainLogger), mirror = Some(mainLogger))
  }

  private def update(slackTask: SlackTask, percentComplete: Float, startTimeMs: Long, width: Int = 14): String = {
    val duration = Duration.ofMillis(System.currentTimeMillis - startTimeMs)
    val bar = s"|${TextProgressBar.SlackEmoji.bar(percentComplete, width)}| ${("  " + math.round(percentComplete * 100)).takeRight(3)}%"
    val elapsed = if (startTimeMs != 0) s"\nStarted ${DateUtils.humanReadable(duration)} ago" else ""
    s"$TaskRunning Running *${slackTask.meta.factory.name.getText}*\n$bar$elapsed"
  }

  private def cancelled(slackTask: SlackTask, startTimeMs: Long): String = {
    val duration = Duration.ofMillis(System.currentTimeMillis - startTimeMs)
    s"$TaskCancelled Cancelled *${slackTask.meta.factory.name.getText}* after ${DateUtils.humanReadable(duration)}"
  }

  private def failed(slackTask: SlackTask, ex: Throwable, startTimeMs: Long): String = {
    val duration = Duration.ofMillis(System.currentTimeMillis - startTimeMs)
    s"$TaskFailure Exception *${slackTask.meta.factory.name.getText}* after ${DateUtils.humanReadable(duration)} with ${ex.getClass.getName}:${ex.getMessage}"
  }

  private def completed(slackTask: SlackTask, startTimeMs: Long): String = {
    val duration = Duration.ofMillis(System.currentTimeMillis - startTimeMs)
    s"$TaskSuccess Completed *${slackTask.meta.factory.name.getText}* in ${DateUtils.humanReadable(duration)}"
  }
}

