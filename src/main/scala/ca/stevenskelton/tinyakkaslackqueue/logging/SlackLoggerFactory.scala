package ca.stevenskelton.tinyakkaslackqueue.logging

import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import ca.stevenskelton.tinyakkaslackqueue._
import ca.stevenskelton.tinyakkaslackqueue.api.SlackClient
import ca.stevenskelton.tinyakkaslackqueue.blocks.logLevelEmoji
import ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory.SlackTaskThread
import com.slack.api.methods.MethodsClient
import com.slack.api.methods.request.chat.ChatPostMessageRequest
import org.slf4j.Logger
import org.slf4j.event.LoggingEvent

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration.DurationInt

object SlackLoggerFactory {

  def logEvent(loggingEvent: LoggingEvent): String = {
    val sMarker = Option(loggingEvent.getMarker)
    val sArgs = Option(loggingEvent.getArgumentArray).getOrElse(Array.empty)
    val text = if (sArgs.isEmpty) loggingEvent.getMessage else String.format(loggingEvent.getMessage, sArgs)
    val emoji = logLevelEmoji(loggingEvent.getLevel)
    val exception = Option(loggingEvent.getThrowable).fold("")(ex => s"${ex.getMessage} ${ex.getStackTrace}")
    s"$emoji $text$exception"
  }

  def logToSlack(base: Logger, slackConfig: SlackClient.SlackConfig)(implicit materializer: Materializer): SlackLogger = {
    val text = s"Log for ${base.getName}"
    val slackThread = slackConfig.client.chatPostMessage((r: ChatPostMessageRequest.ChatPostMessageRequestBuilder) => r.token(slackConfig.botOAuthToken).channel(slackConfig.botChannel.value).text(text))
    val sink = Sink.foreach[Seq[LoggingEvent]] {
      loggingEvents =>
        if (loggingEvents.nonEmpty) {
          val message = loggingEvents.map(logEvent).mkString("\n")
          slackConfig.client.chatPostMessage((r: ChatPostMessageRequest.ChatPostMessageRequestBuilder) =>
            r.token(slackConfig.botOAuthToken).channel(slackConfig.botChannel.value).text(message).threadTs(slackThread.getTs)
          )
        }
    }
    val sourceQueue = Source.queue[LoggingEvent](1, OverflowStrategy.fail).groupedWithin(1000, 5.seconds).to(sink).run()
    new SlackLogger(getName = base.getName, sourceQueue, base)
  }

  def createNewSlackThread(slackTask: SlackTask, base: Logger)(implicit slackClient: SlackClient, materializer: Materializer): SlackLogger = {

    val startTimeMs = System.currentTimeMillis

    val sink = Sink.foreach[Seq[LoggingEvent]] {
      loggingEvents =>
        if (loggingEvents.nonEmpty) {

          var percentCompleteEvent: Option[SlackUpdatePercentCompleteEvent] = None
          var exceptionEvent: Option[SlackExceptionEvent] = None
          val messageEvents = loggingEvents.flatMap {
            case loggingEvent: SlackUpdatePercentCompleteEvent =>
              percentCompleteEvent = Some(loggingEvent)
              None
            case loggingEvent: SlackExceptionEvent =>
              exceptionEvent = Some(loggingEvent)
              Some(logEvent(loggingEvent))
            case loggingEvent =>
              Some(logEvent(loggingEvent))
          }
          if (messageEvents.nonEmpty) {
            val message = messageEvents.mkString("\n")
            slackClient.chatPostMessageInThread(message, slackTask.ts)
          }
          exceptionEvent match {
            case Some(_) =>
              slackClient.chatUpdate(SlackTaskThread.cancelled(slackTask, startTimeMs), slackTask.ts)
            //              slackClient.pinsRemove(slackTask.ts)
            case None =>
              percentCompleteEvent.foreach {
                event =>
                  slackClient.chatUpdate(SlackTaskThread.update(slackTask, event.percent, startTimeMs), slackTask.ts)
              }
          }
        }
    }
    val sourceQueue = Source.queue[LoggingEvent](1, OverflowStrategy.fail).groupedWithin(1000, 5.seconds).to(sink).run()
    sourceQueue.watchCompletion.map {
      _ =>
        slackClient.chatUpdate(SlackTaskThread.completed(slackTask, startTimeMs), slackTask.ts)
        slackClient.pinsRemove(slackTask.ts)
    }
    new SlackLogger(getName = slackTask.meta.channel.value, sourceQueue, base)
  }

}

