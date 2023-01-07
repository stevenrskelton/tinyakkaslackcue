package ca.stevenskelton.tinyakkaslackqueue.example

import akka.stream.scaladsl.{Flow, Keep, Source}
import akka.stream.{KillSwitches, Materializer, UniqueKillSwitch}
import ca.stevenskelton.tinyakkaslackqueue.SlackPayload
import ca.stevenskelton.tinyakkaslackqueue.api.{SlackClient, SlackTaskFactory}
import ca.stevenskelton.tinyakkaslackqueue.views.task.TaskOptionInput
import com.slack.api.model.block.composition.MarkdownTextObject
import org.slf4j.Logger

import scala.concurrent.Future
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class TestSlackTaskFactory(duration: FiniteDuration, updateInterval: FiniteDuration = 1.second)(implicit slackClient: SlackClient, materializer: Materializer) extends SlackTaskFactory[Int, Int] {

  override val name: MarkdownTextObject = createMarkdownText(s"test-${duration.toMillis}-${updateInterval.toMillis}")

  override val description: MarkdownTextObject = createMarkdownText(s"${duration.toSeconds.toString} seconds @ ${updateInterval.toMillis}ms")

  override def distinctBy: Int => Int = identity

  override def sourceAndCount: (SlackPayload, Logger) => (Source[Int, UniqueKillSwitch], Future[Int]) = {
    case (payload, logger) =>
      implicit val log = logger

      val totalSeconds = duration.toSeconds.toInt
      val totalCount = Future.successful(totalSeconds)
      val start = System.currentTimeMillis
      val source = Source(1 to totalSeconds)
        .async.viaMat(KillSwitches.single)(Keep.right)
        .via(Flow.fromFunction {
          i =>
            Thread.sleep(updateInterval.toMillis)
            if (i % 10 == 0) {
              val skew = System.currentTimeMillis - start - (i * 1000)
              logger.info(s"Skew of ${skew}ms at $i second")
            }
            i
        })
      (source, totalCount)
  }

  override def taskOptions(slackPayload: SlackPayload): Seq[TaskOptionInput] = Nil
}