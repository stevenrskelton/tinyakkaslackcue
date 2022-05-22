package ca.stevenskelton.tinyakkaslackcue

import akka.actor.ActorSystem
import ca.stevenskelton.tinyakkaslackcue
import ca.stevenskelton.tinyakkaslackcue.blocks.{SlackTaskThread, SlackView}
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
    override def chatUpdate(text: String, ts: tinyakkaslackcue.SlackTs): ChatUpdateResponse = ???
    override def chatUpdateBlocks(blocks: tinyakkaslackcue.SlackBlocksAsString, ts: tinyakkaslackcue.SlackTs): ChatUpdateResponse = ???
    override def pinsAdd(ts: tinyakkaslackcue.SlackTs): PinsAddResponse = ???
    override def pinsRemove(ts: tinyakkaslackcue.SlackTs): PinsRemoveResponse = ???
    override def pinsList(): Iterable[PinsListResponse.MessageItem] = ???
    override def pinnedTasks(slackTaskFactories: SlackTaskFactories): Iterable[(SlackTask, SlackTaskThread.Fields)] = ???
    override def chatPostMessageInThread(text: String, thread: tinyakkaslackcue.SlackTs): ChatPostMessageResponse = ???
    override def chatPostMessage(text: String): ChatPostMessageResponse = ???
    override def viewsPublish(userId: tinyakkaslackcue.SlackUserId, view: SlackView): ViewsPublishResponse = ???
    override def viewsOpen(slackTriggerId: tinyakkaslackcue.SlackTriggerId, view: tinyakkaslackcue.SlackBlocksAsString): ViewsOpenResponse = ???
    override def botOAuthToken: String = ""
    override def historyThread: SlackTs = SlackTs.Empty
  }
  private class TestSlackTaskFactory(number:String) extends SlackTaskFactory(){
    val self = this
    override def create(ts: tinyakkaslackcue.SlackTs, createdBy: tinyakkaslackcue.SlackUserId, notifyOnError: Seq[tinyakkaslackcue.SlackUserId], notifyOnComplete: Seq[tinyakkaslackcue.SlackUserId]): SlackTask = {
      new SlackTask {
        override def name: String = self.name
        override def description: tinyakkaslackcue.Mrkdwn = self.description
        override def ts: tinyakkaslackcue.SlackTs = SlackTs(Random.alphanumeric.take(5).toString)
        override def createdBy: tinyakkaslackcue.SlackUserId = CreatedBy
        override def notifyOnError: Seq[tinyakkaslackcue.SlackUserId] = Seq(
          new SlackUserId(Random.alphanumeric.take(5).toString),
          new SlackUserId(Random.alphanumeric.take(5).toString)
        )
        override def notifyOnComplete: Seq[tinyakkaslackcue.SlackUserId] = Seq(
          new SlackUserId(Random.alphanumeric.take(5).toString),
          new SlackUserId(Random.alphanumeric.take(5).toString),
          new SlackUserId(Random.alphanumeric.take(5).toString)
        )
        override def run(logger: Logger): Unit = ()
      }
    }
    override def name: String = s"Name$number"
    override def description: tinyakkaslackcue.Mrkdwn = tinyakkaslackcue.Mrkdwn(s"Description$number")
  }

  implicit val slackTaskFactories = new SlackTaskFactories(slackClient, logger, actorSystem, config){
    override def factories: Seq[SlackTaskFactory] = Seq(new TestSlackTaskFactory("One"), new TestSlackTaskFactory("Two"), new TestSlackTaskFactory("Three"))
  }

  def toScheduledTask(slackTask: SlackTask):InteractiveJavaUtilTimer[SlackTask]#ScheduledTask = new InteractiveJavaUtilTimer[SlackTask](TestData.logger).ScheduledTask(
    slackTask,
    ZonedDateTime.of(2100, 1, 1, 12, 30, 0, 0, ZoneId.systemDefault()),
    isRunning = false
  )
}
