package ca.stevenskelton.tinyakkaslackqueue

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Keep, Source}
import akka.stream.{KillSwitches, SystemMaterializer, UniqueKillSwitch}
import ca.stevenskelton.tinyakkaslackqueue
import ca.stevenskelton.tinyakkaslackqueue.api.{SlackClient, SlackTaskFactories, SlackTaskFactory}
import ca.stevenskelton.tinyakkaslackqueue.timer.InteractiveJavaUtilTimer
import ca.stevenskelton.tinyakkaslackqueue.views.SlackView
import ca.stevenskelton.tinyakkaslackqueue.views.task.TaskOptionInput
import com.slack.api.methods.MethodsClient
import com.slack.api.methods.response.chat.{ChatPostMessageResponse, ChatUpdateResponse}
import com.slack.api.methods.response.conversations.ConversationsRepliesResponse
import com.slack.api.methods.response.pins.{PinsAddResponse, PinsListResponse, PinsRemoveResponse}
import com.slack.api.methods.response.views.{ViewsOpenResponse, ViewsPublishResponse, ViewsUpdateResponse}
import com.slack.api.model.block.composition.MarkdownTextObject
import org.slf4j.{Logger, LoggerFactory}

import java.time.{ZoneId, ZonedDateTime}
import scala.concurrent.Future

object TestData {

  val CreatedBy = SlackUserId("createdbyuserid")
  val SlackUser = new SlackUser(CreatedBy, "username", "name", "team")
  val slackTs = SlackTs("testTs")

  implicit val logger = LoggerFactory.getLogger("Specs")
  private val actorSystem = ActorSystem.create()
  private implicit val materializer = SystemMaterializer(actorSystem).materializer
  implicit val slackClient = new SlackClient {
    override def chatUpdate(text: String, slackMessage: tinyakkaslackqueue.SlackMessage): ChatUpdateResponse = ???

    override def chatUpdateBlocks(blocks: tinyakkaslackqueue.SlackBlocksAsString, slackPost: tinyakkaslackqueue.SlackMessage): ChatUpdateResponse = ???

    override def pinsAdd(slackMessage: tinyakkaslackqueue.SlackMessage): PinsAddResponse = ???

    override def pinsRemove(slackMessage: tinyakkaslackqueue.SlackMessage): PinsRemoveResponse = ???

    override def viewsPublish(userId: tinyakkaslackqueue.SlackUserId, view: SlackView): ViewsPublishResponse = ???

    override def client: MethodsClient = ???

    override def viewsUpdate(viewId: String, slackView: SlackView): ViewsUpdateResponse = ???

    override def viewsOpen(slackTriggerId: SlackTriggerId, slackView: SlackView): ViewsOpenResponse = ???

    override def threadReplies(messageItem: PinsListResponse.MessageItem): ConversationsRepliesResponse = ???

    override def slackConfig: SlackClient.SlackConfig = ???

    override def pinsList(channel: SlackChannel): Iterable[PinsListResponse.MessageItem] = ???

    override def chatPostMessageInThread(text: String, thread: SlackThread): ChatPostMessageResponse = ???

    override def chatPostMessage(text: String, channel: SlackChannel): ChatPostMessageResponse = ???

    override def threadReplies(slackThread: SlackThread): ConversationsRepliesResponse = ???

    override def userZonedId(slackUserId: SlackUserId): ZoneId = ???
  }

  private class TestSlackTaskFactory(number: String) extends SlackTaskFactory[Int, Int] {

    override def name: MarkdownTextObject = MarkdownTextObject.builder().text(s"Name$number").build()

    override def description: MarkdownTextObject = MarkdownTextObject.builder().text(s"Description$number").build()

    override def distinctBy: Int => Int = identity

    override def sourceAndCount: (SlackPayload, Logger) => (Source[Int, UniqueKillSwitch], Future[Int]) = {
      case _ =>
        val seq = Seq(1, 2, 3)
        (Source(seq).viaMat(KillSwitches.single)(Keep.right), Future.successful(seq.length))
    }

    override def taskOptions(slackPayload: SlackPayload): Seq[TaskOptionInput] = Nil
  }

  implicit val slackTaskFactories = SlackFactories.initialize(SlackTaskFactories(
    new TestSlackTaskFactory("One"),
    new TestSlackTaskFactory("Two"),
    new TestSlackTaskFactory("Three")
  ))

  def toScheduledTask(slackTask: SlackTask): ScheduledSlackTask = new InteractiveJavaUtilTimer[SlackTs, SlackTask].ScheduledTask(
    slackTask,
    ZonedDateTime.of(2100, 1, 1, 12, 30, 0, 0, ZoneId.systemDefault()),
    isRunning = false
  )
}
