package ca.stevenskelton.tinyakkaslackqueue.example

import akka.stream.scaladsl.{Flow, Keep, Source}
import akka.stream.{KillSwitches, Materializer, UniqueKillSwitch}
import ca.stevenskelton.tinyakkaslackqueue._
import com.slack.api.model.block.composition.MarkdownTextObject
import org.slf4j.Logger

import scala.concurrent.Future

class TestSlackTaskFactory(implicit slackClient: SlackClient, materializer: Materializer) extends SlackTaskFactory {

  override val name = MarkdownTextObject.builder().text("test").build()

  override val description = MarkdownTextObject.builder().text("2 minutes @ 1 second").build()

  override def create(slackTaskMeta: SlackTaskMeta, ts: SlackTs, createdBy: SlackUserId, notifyOnError: Seq[SlackUserId], notifyOnComplete: Seq[SlackUserId]): SlackTask = {
    val innerTs = ts
    val innerCreatedBy = createdBy
    val innerNotifyOnError = notifyOnError
    val innerNotifyOnComplete = notifyOnComplete

    new SlackLoggedStreamTask[Int, Int] {

      override def distinctBy: Int => Int = identity

      override def sourceAndCount: Logger => (Source[Int, UniqueKillSwitch], Future[Int]) = {
        implicit logger =>
          val totalCount = Future.successful(100)
          val start = System.currentTimeMillis
          val source = Source(1 to 120)
            .async.viaMat(KillSwitches.single)(Keep.right)
            .via(Flow.fromFunction {
              i =>
                Thread.sleep(1000)
                if(i % 10 == 0){
                  val skew = System.currentTimeMillis - start - (i * 1000)
                  logger.info(s"Skew of ${skew}ms at $i second")
                }
                i
            })
          (source, totalCount)
      }

      override def ts: SlackTs = innerTs

      override def createdBy: SlackUserId = innerCreatedBy

      override def notifyOnError: Seq[SlackUserId] = innerNotifyOnError

      override def notifyOnComplete: Seq[SlackUserId] = innerNotifyOnComplete

      override def meta: SlackTaskMeta = slackTaskMeta
    }
  }
}