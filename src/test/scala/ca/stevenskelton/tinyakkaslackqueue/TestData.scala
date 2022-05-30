package ca.stevenskelton.tinyakkaslackqueue

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Keep, Source}
import akka.stream.{KillSwitches, UniqueKillSwitch}
import ca.stevenskelton.tinyakkaslackqueue
import ca.stevenskelton.tinyakkaslackqueue.api.{SlackClient, SlackFactories, SlackTaskFactory}
import ca.stevenskelton.tinyakkaslackqueue.timer.InteractiveJavaUtilTimer
import ca.stevenskelton.tinyakkaslackqueue.views.SlackView
import com.slack.api.methods.MethodsClient
import com.slack.api.methods.response.chat.{ChatPostMessageResponse, ChatUpdateResponse}
import com.slack.api.methods.response.conversations.ConversationsRepliesResponse
import com.slack.api.methods.response.pins.{PinsAddResponse, PinsListResponse, PinsRemoveResponse}
import com.slack.api.methods.response.views.{ViewsOpenResponse, ViewsPublishResponse, ViewsUpdateResponse}
import com.slack.api.model.block.composition.MarkdownTextObject
import com.typesafe.config.ConfigFactory
import org.slf4j.{Logger, LoggerFactory}

import java.time.{ZoneId, ZonedDateTime}
import scala.concurrent.Future

object TestData {

  val CreatedBy = SlackUserId("createdbyuserid")
  val SlackUser = new SlackUser(CreatedBy, "username", "name", "team")
  val slackTs = SlackTs("testTs")

  implicit val logger = LoggerFactory.getLogger("Specs")
  private val actorSystem = ActorSystem.create()
  private val config = ConfigFactory.defaultApplication()
  implicit val slackClient = new SlackClient {
    override def chatUpdate(text: String, slackMessage: tinyakkaslackqueue.SlackMessage): ChatUpdateResponse = ???

    override def chatUpdateBlocks(blocks: tinyakkaslackqueue.SlackBlocksAsString, slackPost: tinyakkaslackqueue.SlackMessage): ChatUpdateResponse = ???

    override def pinsAdd(slackMessage: tinyakkaslackqueue.SlackMessage): PinsAddResponse = ???

    override def pinsRemove(slackMessage: tinyakkaslackqueue.SlackMessage): PinsRemoveResponse = ???

    override def pinsList(): Iterable[PinsListResponse.MessageItem] = ???

    //    override def pinnedTasks(slackTaskFactories: SlackFactories): Iterable[(SlackTask, SlackTaskThread.Fields)] = ???

    override def chatPostMessageInThread(text: String, thread: tinyakkaslackqueue.SlackMessage): ChatPostMessageResponse = ???

    override def chatPostMessage(text: String): ChatPostMessageResponse = ???

    override def viewsPublish(userId: tinyakkaslackqueue.SlackUserId, view: SlackView): ViewsPublishResponse = ???

    //    override def viewsOpen(slackTriggerId: tinyakkaslackqueue.SlackTriggerId, view: tinyakkaslackqueue.SlackBlocksAsString): ViewsOpenResponse = ???

    override def botOAuthToken: String = ""

    //    override def historyThread: SlackTs = SlackTs.Empty

    override def client: MethodsClient = ???

    override def botUserId: SlackUserId = ???

    override def botChannel: SlackChannel = ???

    override def viewsUpdate(viewId: String, slackView: SlackView): ViewsUpdateResponse = ???

    override def viewsOpen(slackTriggerId: SlackTriggerId, slackView: SlackView): ViewsOpenResponse = ???

    override def threadReplies(messageItem: PinsListResponse.MessageItem): ConversationsRepliesResponse = ???
  }

  private class TestSlackTaskFactory(number: String) extends SlackTaskFactory[Int, Int] {

    override def name: MarkdownTextObject = MarkdownTextObject.builder().text(s"Name$number").build()

    override def description: MarkdownTextObject = MarkdownTextObject.builder().text(s"Description$number").build()

    override def distinctBy: Int => Int = identity

    override def sourceAndCount: Logger => (Source[Int, UniqueKillSwitch], Future[Int]) = _ => {
      val seq = Seq(1, 2, 3)
      (Source(seq).viaMat(KillSwitches.single)(Keep.right), Future.successful(seq.length))
    }
  }

  implicit val slackTaskFactories = new SlackFactories(slackClient, logger, actorSystem, config) {
    override protected val factories: Seq[SlackTaskFactory[_, _]] = Seq(
      new TestSlackTaskFactory("One"),
      new TestSlackTaskFactory("Two"),
      new TestSlackTaskFactory("Three")
    )
  }

  def toScheduledTask(slackTask: SlackTask): ScheduledSlackTask = new InteractiveJavaUtilTimer[SlackMessage, SlackTask](TestData.logger).ScheduledTask(
    slackTask,
    ZonedDateTime.of(2100, 1, 1, 12, 30, 0, 0, ZoneId.systemDefault()),
    isRunning = false
  )
}
