package ca.stevenskelton.tinyakkaslackqueue.lib

import akka.stream.{Materializer, UniqueKillSwitch}
import akka.stream.scaladsl.{Keep, Sink, Source}
import ca.stevenskelton.tinyakkaslackqueue.logging.{SlackExceptionEvent, SlackLoggerFactory, SlackUpdatePercentCompleteEvent}
import ca.stevenskelton.tinyakkaslackqueue.{SlackClient, SlackFactories, SlackTask, SlackTs, SlackUserId}
import com.slack.api.model.block.composition.MarkdownTextObject
import org.slf4j.Logger

import scala.collection.mutable
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

/**
 * Inherit to implement Akka Stream tasks, typed as a SlackTask
 *
 * @tparam T Source will create items of type T
 * @tparam B If T should be grouped together, for example by T.b, then set B != T
 */
trait SlackTaskFactory[T, B] {

  /**
   * A brief name for this task.
   * Will be used to create a Slack channel (modified to be a valid Slack channel name).
   * Markdown is supported, see https://api.slack.com/reference/surfaces/formatting#basics
   */
  def name: MarkdownTextObject

  /**
   * A longer description for the task.
   * Markdown is supported, see https://api.slack.com/reference/surfaces/formatting#basics
   */
  def description: MarkdownTextObject

  /**
   * Group T together by field B
   */
  def distinctBy: T => B

  /**
   * Create a source of T.
   * Use `.async.viaMat(KillSwitches.single)(Keep.right)` to add an kill switch
   */
  def sourceAndCount: Logger => (Source[T, UniqueKillSwitch], Future[Int])

  /**
   *  Helper class to create `name` and `description`
   */
  protected def createMarkdownText(value: String): MarkdownTextObject = MarkdownTextObject.builder().text(value).build()

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


