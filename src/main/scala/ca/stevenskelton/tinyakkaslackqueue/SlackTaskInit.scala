package ca.stevenskelton.tinyakkaslackqueue

import akka.stream.scaladsl.{Keep, Sink}
import akka.stream.{Materializer, UniqueKillSwitch}
import ca.stevenskelton.tinyakkaslackqueue.api.{SlackClient, SlackTaskFactory}
import ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory.{ErrorHistoryItem, SuccessHistoryItem}
import ca.stevenskelton.tinyakkaslackqueue.logging.{SlackExceptionEvent, SlackLoggerFactory, SlackUpdatePercentCompleteEvent}
import org.slf4j.Logger

import java.time.ZonedDateTime
import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

trait SlackTaskInit[T, B] {

  self: SlackTaskFactory[T, B] =>

  def create(slackTaskMeta: SlackTaskMeta, taskThread: SlackTaskThread, createdBy: SlackUserId, notifyOnError: Seq[SlackUserId], notifyOnComplete: Seq[SlackUserId])
            (implicit slackClient: SlackClient, materializer: Materializer): SlackTask = {

    import materializer.executionContext

    val lcreatedBy = createdBy
    val lnotifyOnError = notifyOnError
    val lnotifiyOnComplete = notifyOnComplete

    new SlackTask {

      var killSwitchOption: Option[UniqueKillSwitch] = None

      override def run(logger: Logger): Unit = {
        implicit val slackTaskLogger = SlackLoggerFactory.createNewSlackThread(this, logger)
        runStart = Some(ZonedDateTime.now())

        val completeElements = new mutable.HashSet[B]()
        val (source, itemCount) = sourceAndCount(slackTaskLogger)
        itemCount.foreach {
          i =>
            estimatedCount = i
            slackTaskMeta.historyAddRun(slackTaskThread, estimatedCount)
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
            slackTaskMeta.historyAddOutcome(SuccessHistoryItem(totalItemCount, runStart.get), slackTaskThread)
          case Failure(ex) =>
            slackTaskLogger.recordEvent(SlackExceptionEvent(ex))
            slackTaskMeta.historyAddOutcome(ErrorHistoryItem(ex.getClass.getName, ex.getMessage, runStart.get), slackTaskThread)
        }
        Await.result(result, 24.hours)
      }

      override def cancel(): Boolean = {
        killSwitchOption.foreach {
          _.abort(SlackExceptionEvent.UserCancelledException)
        }
        super.cancel
      }

      override def slackTaskThread: SlackTaskThread = taskThread

      override def meta: SlackTaskMeta = slackTaskMeta

      override def createdBy: SlackUserId = lcreatedBy

      override def notifyOnError: Seq[SlackUserId] = lnotifyOnError

      override def notifyOnComplete: Seq[SlackUserId] = lnotifiyOnComplete
    }
  }

}
