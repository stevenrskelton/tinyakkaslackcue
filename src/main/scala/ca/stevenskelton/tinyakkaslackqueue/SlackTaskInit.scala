package ca.stevenskelton.tinyakkaslackqueue

import akka.stream.scaladsl.{Keep, Sink}
import akka.stream.{Materializer, UniqueKillSwitch}
import ca.stevenskelton.tinyakkaslackqueue.api.{SlackClient, SlackTaskFactory}
import ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory.{ErrorHistoryItem, SuccessHistoryItem}
import ca.stevenskelton.tinyakkaslackqueue.logging._
import org.slf4j.Logger
import org.slf4j.event.Level

import java.time.ZonedDateTime
import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

trait SlackTaskInit[T, B] {

  self: SlackTaskFactory[T, B] =>

  def create(
              slackPayload: SlackPayload, slackTaskMeta: SlackTaskMeta, taskThread: SlackTaskThread,
              createdBySlackUser: SlackUserId, logLevel: Level, mainLogger: Logger
            )
            (implicit materializer: Materializer): SlackTask = {

    import materializer.executionContext

    val pLogLevel = logLevel

    new SlackTask {

      var killSwitchOption: Option[UniqueKillSwitch] = None

      override def run(): Unit = {
        implicit val slackClient: SlackClient = slackTaskMeta.slackClient
        implicit val slackTaskLogger: SlackLogger = SlackLoggerFactory.createNewSlackThread(this, mainLogger)
        runStart = Some(ZonedDateTime.now())

        val completeElements = new mutable.HashSet[B]()
        val (source, itemCount) = sourceAndCount(slackPayload, slackTaskLogger)
        itemCount.foreach {
          i =>
            estimatedCount = i
            SlackResponseException.logError(slackTaskMeta.historyAddRun(slackTaskThread, estimatedCount), mainLogger)
        }
        val (killswitch, result) = source.toMat(Sink.fold(0) {
          (_, t) =>
            val key = distinctBy(t)
            if (completeElements.add(key)) {
              completedCount = completeElements.size
              itemCount.foreach(_ => slackTaskLogger.recordEvent(SlackUpdatePercentCompleteEvent(percentComplete)))
            }
            completeElements.size
        })(Keep.both).run()
        killSwitchOption = Some(killswitch)
        result.onComplete {
          case Success(totalItemCount) =>
            slackTaskLogger.recordEvent(SlackUpdatePercentCompleteEvent(1))
            isComplete = true
            mainLogger.info(s"Completed $totalItemCount for ${slackTaskMeta.factory.name.getText}")
            SlackResponseException.logError(slackTaskMeta.historyAddOutcome(SuccessHistoryItem(totalItemCount, runStart.get), slackTaskThread), mainLogger)
          case Failure(ex) =>
            mainLogger.error(s"Failed ${slackTaskMeta.factory.name}", ex)
            slackTaskLogger.recordEvent(SlackExceptionEvent(ex))
            SlackResponseException.logError(slackTaskMeta.historyAddOutcome(ErrorHistoryItem(ex.getClass.getName, ex.getMessage, runStart.get), slackTaskThread), mainLogger)
        }
        Await.result(result, 24.hours)
      }

      override def cancel(): Boolean = {
        killSwitchOption.foreach {
          _.abort(SlackExceptionEvent.UserCancelledException)
        }
        super.cancel()
      }

      override val slackTaskThread: SlackTaskThread = taskThread

      override val meta: SlackTaskMeta = slackTaskMeta

      override val createdBy: SlackUserId = createdBySlackUser

      override val logLevel: Level = pLogLevel
    }
  }

}
