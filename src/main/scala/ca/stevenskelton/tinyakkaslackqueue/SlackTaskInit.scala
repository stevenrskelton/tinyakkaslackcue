package ca.stevenskelton.tinyakkaslackqueue

import akka.stream.scaladsl.{Keep, Sink}
import akka.stream.{Materializer, UniqueKillSwitch}
import ca.stevenskelton.tinyakkaslackqueue.api.{SlackClient, SlackTaskFactory}
import ca.stevenskelton.tinyakkaslackqueue.logging.{SlackExceptionEvent, SlackLoggerFactory, SlackUpdatePercentCompleteEvent}
import org.slf4j.Logger

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

trait SlackTaskInit[T, B] {

  self: SlackTaskFactory[T, B] =>

  def create(slackTaskMeta: SlackTaskMeta, ts: SlackTs, createdBy: SlackUserId, notifyOnError: Seq[SlackUserId], notifyOnComplete: Seq[SlackUserId])
            (implicit slackClient: SlackClient, materializer: Materializer): SlackTask = {

    import scala.concurrent.ExecutionContext.Implicits.global

    val lts = ts
    val lcreatedBy = createdBy
    val lnotifyOnError = notifyOnError
    val lnotifiyOnComplete = notifyOnComplete

    new SlackTask {

      var killSwitchOption: Option[UniqueKillSwitch] = None

      override def run(logger: Logger): Unit = {

        val completeElements = new mutable.HashSet[B]()
        implicit val slackTaskLogger = SlackLoggerFactory.createNewSlackThread(this, logger) //, 2.seconds)
        val (source, itemCount) = sourceAndCount(slackTaskLogger)
        itemCount.foreach(i => estimatedCount = i)
        val (killswitch, result) = source.toMat(Sink.foreach {
          t =>
            val key = distinctBy(t)
            if (completeElements.add(key)) {
              completedCount = completeElements.size
              itemCount.foreach(_ => slackTaskLogger.recordEvent(SlackUpdatePercentCompleteEvent(percentComplete)))
            }
        })(Keep.both).run()
        killSwitchOption = Some(killswitch)
        result.onComplete {
          case Success(_) =>
            slackTaskLogger.recordEvent(SlackUpdatePercentCompleteEvent(1))
            isComplete = true
          case Failure(ex) =>
            slackTaskLogger.recordEvent(SlackExceptionEvent(ex))
        }
        Await.result(result, 24.hours)
      }

      override def cancel(): Boolean = {
        killSwitchOption.foreach {
          _.abort(SlackExceptionEvent.UserCancelledException)
        }
        super.cancel
      }

      override def ts: SlackTs = lts

      override def meta: SlackTaskMeta = slackTaskMeta

      override def createdBy: SlackUserId = lcreatedBy

      override def notifyOnError: Seq[SlackUserId] = lnotifyOnError

      override def notifyOnComplete: Seq[SlackUserId] = lnotifiyOnComplete
    }
  }

}
