package ca.stevenskelton.tinyakkaslackqueue.example

import akka.stream.scaladsl.{Flow, Keep, Source}
import akka.stream.{KillSwitches, Materializer, UniqueKillSwitch}
import ca.stevenskelton.tinyakkaslackqueue.api.{SlackClient, SlackTaskFactory}
import org.slf4j.Logger

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

class TestSlackTaskFactory(duration: FiniteDuration)(implicit slackClient: SlackClient, materializer: Materializer) extends SlackTaskFactory[Int, Int] {

  override val name = createMarkdownText("test")

  override val description = createMarkdownText(s"${duration.toSeconds.toString} seconds @ 1 second")

  override def distinctBy: Int => Int = identity

  override def sourceAndCount: Logger => (Source[Int, UniqueKillSwitch], Future[Int]) = {
    implicit logger =>
      val totalSeconds = duration.toSeconds.toInt
      val totalCount = Future.successful(totalSeconds)
      val start = System.currentTimeMillis
      val source = Source(1 to totalSeconds)
        .async.viaMat(KillSwitches.single)(Keep.right)
        .via(Flow.fromFunction {
          i =>
            Thread.sleep(1000)
            if (i % 10 == 0) {
              val skew = System.currentTimeMillis - start - (i * 1000)
              logger.info(s"Skew of ${skew}ms at $i second")
            }
            i
        })
      (source, totalCount)
  }
}