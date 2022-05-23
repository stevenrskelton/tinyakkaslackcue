package ca.stevenskelton.tinyakkaslackqueue

import akka.actor.ActorSystem
import ca.stevenskelton.tinyakkaslackqueue
import ca.stevenskelton.tinyakkaslackqueue.blocks.SlackTaskThread
import ca.stevenskelton.tinyakkaslackqueue.timer.InteractiveJavaUtilTimer
import ca.stevenskelton.tinyakkaslackqueue.views.SlackView
import com.slack.api.methods.response.chat.{ChatPostMessageResponse, ChatUpdateResponse}
import com.slack.api.methods.response.pins.{PinsAddResponse, PinsListResponse, PinsRemoveResponse}
import com.slack.api.methods.response.views.{ViewsOpenResponse, ViewsPublishResponse}
import com.typesafe.config.ConfigFactory
import org.slf4j.{Logger, LoggerFactory}

import java.time.{Instant, ZoneId, ZonedDateTime}
import scala.util.Random

object TestData {

  val CreatedBy = SlackUserId("createdbyuserid")
  val SlackUser = new SlackUser(CreatedBy, "username", "name", "team")
  val slackTs = SlackTs("testTs")

  implicit val logger = LoggerFactory.getLogger("Specs")
  private val actorSystem = ActorSystem.create()
  private val config = ConfigFactory.defaultApplication()
  implicit val slackClient = new SlackClient {
    override def chatUpdate(text: String, ts: tinyakkaslackqueue.SlackTs): ChatUpdateResponse = ???
    override def chatUpdateBlocks(blocks: tinyakkaslackqueue.SlackBlocksAsString, ts: tinyakkaslackqueue.SlackTs): ChatUpdateResponse = ???
    override def pinsAdd(ts: tinyakkaslackqueue.SlackTs): PinsAddResponse = ???
    override def pinsRemove(ts: tinyakkaslackqueue.SlackTs): PinsRemoveResponse = ???
    override def pinsList(): Iterable[PinsListResponse.MessageItem] = ???
    override def pinnedTasks(slackTaskFactories: SlackFactories): Iterable[(SlackTask, SlackTaskThread.Fields)] = ???
    override def chatPostMessageInThread(text: String, thread: tinyakkaslackqueue.SlackTs): ChatPostMessageResponse = ???
    override def chatPostMessage(text: String): ChatPostMessageResponse = ???
    override def viewsPublish(userId: tinyakkaslackqueue.SlackUserId, view: SlackView): ViewsPublishResponse = ???
    override def viewsOpen(slackTriggerId: tinyakkaslackqueue.SlackTriggerId, view: tinyakkaslackqueue.SlackBlocksAsString): ViewsOpenResponse = ???
    override def botOAuthToken: String = ""
    override def historyThread: SlackTs = SlackTs.Empty
  }
  private class TestSlackTaskFactory(number:String) extends SlackTaskFactory(){
    val self = this
    override def create(ts: tinyakkaslackqueue.SlackTs, createdBy: tinyakkaslackqueue.SlackUserId, notifyOnError: Seq[tinyakkaslackqueue.SlackUserId], notifyOnComplete: Seq[tinyakkaslackqueue.SlackUserId]): SlackTask = {
      new SlackTask {
        override def name: String = self.name
        override def description: tinyakkaslackqueue.Mrkdwn = self.description
        override def ts: tinyakkaslackqueue.SlackTs = SlackTs(Random.alphanumeric.take(5).toString)
        override def createdBy: tinyakkaslackqueue.SlackUserId = CreatedBy
        override def notifyOnError: Seq[tinyakkaslackqueue.SlackUserId] = Seq(
          new SlackUserId(Random.alphanumeric.take(5).toString),
          new SlackUserId(Random.alphanumeric.take(5).toString)
        )
        override def notifyOnComplete: Seq[tinyakkaslackqueue.SlackUserId] = Seq(
          new SlackUserId(Random.alphanumeric.take(5).toString),
          new SlackUserId(Random.alphanumeric.take(5).toString),
          new SlackUserId(Random.alphanumeric.take(5).toString)
        )
        override def run(logger: Logger): Unit = ()
      }
    }
    override def name: String = s"Name$number"
    override def description: tinyakkaslackqueue.Mrkdwn = tinyakkaslackqueue.Mrkdwn(s"Description$number")
  }

  implicit val slackTaskFactories = new SlackFactories(slackClient, logger, actorSystem, config){
    override def factories: Seq[SlackTaskFactory] = Seq(new TestSlackTaskFactory("One"), new TestSlackTaskFactory("Two"), new TestSlackTaskFactory("Three"))
  }

  def toScheduledTask(slackTask: SlackTask):InteractiveJavaUtilTimer[SlackTs,SlackTask]#ScheduledTask = new InteractiveJavaUtilTimer[SlackTs,SlackTask](TestData.logger).ScheduledTask(
    slackTask,
    ZonedDateTime.of(2100, 1, 1, 12, 30, 0, 0, ZoneId.systemDefault()),
    isRunning = false
  )
}
