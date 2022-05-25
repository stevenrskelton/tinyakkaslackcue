package ca.stevenskelton.tinyakkaslackqueue

import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{Materializer, UniqueKillSwitch}
import ca.stevenskelton.tinyakkaslackqueue.logging.{SlackExceptionEvent, SlackLoggerFactory, SlackUpdatePercentCompleteEvent}
import org.slf4j.Logger

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

abstract class SlackLoggedStreamTask[T, B](implicit slackClient: SlackClient, val materializer: Materializer) extends SlackTask {

  def distinctBy: T => B

  def sourceAndCount: Logger => (Source[T, UniqueKillSwitch], Future[Int])

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
          itemCount.foreach(total => slackTaskLogger.recordEvent(SlackUpdatePercentCompleteEvent(percentComplete)))
        }
    })(Keep.both).run()
    killSwitchOption = Some(killswitch)
    result.onComplete {
      case Success(_) => slackTaskLogger.recordEvent(SlackUpdatePercentCompleteEvent(1))
      case Failure(ex) => slackTaskLogger.recordEvent(SlackExceptionEvent(ex))
    }
    Await.result(result, 24.hours)
  }

  override def cancel(): Boolean = {
    killSwitchOption.foreach {
      o =>
        o.abort(SlackExceptionEvent.UserCancelledException)
        //o.shutdown()
    }
    super.cancel
  }
}