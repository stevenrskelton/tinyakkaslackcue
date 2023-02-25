package ca.stevenskelton.tinyakkaslackqueue

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Keep, Source}
import akka.stream.{KillSwitches, SystemMaterializer, UniqueKillSwitch}
import ca.stevenskelton.tinyakkaslackqueue
import ca.stevenskelton.tinyakkaslackqueue.api.{SlackClient, SlackConfig, SlackTaskFactories, SlackTaskFactory}
import ca.stevenskelton.tinyakkaslackqueue.timer.InteractiveJavaUtilTimer
import ca.stevenskelton.tinyakkaslackqueue.views.SlackView
import ca.stevenskelton.tinyakkaslackqueue.views.task.TaskOptionInput
import com.slack.api.methods.MethodsClient
import com.slack.api.methods.response.chat.{ChatPostMessageResponse, ChatUpdateResponse}
import com.slack.api.methods.response.conversations.ConversationsRepliesResponse
import com.slack.api.methods.response.pins.{PinsAddResponse, PinsListResponse, PinsRemoveResponse}
import com.slack.api.methods.response.views.{ViewsOpenResponse, ViewsPublishResponse, ViewsUpdateResponse}
import com.slack.api.model.Conversation
import com.slack.api.model.block.composition.MarkdownTextObject
import com.typesafe.config.ConfigFactory
import org.slf4j.{Logger, LoggerFactory}

import java.time.{LocalDateTime, ZoneId}
import scala.concurrent.Future
import scala.util.Try
import org.scalamock.scalatest.MockFactory

object TestData extends MockFactory {

  val CreatedBy = SlackUserId("createdbyuserid")
  val SlackUser = new SlackUser(CreatedBy, "username", "name", "team")
  val slackTs = SlackTs("testTs")

  implicit val logger = LoggerFactory.getLogger("Specs")
  private val actorSystem = ActorSystem.create()
  private implicit val materializer = SystemMaterializer(actorSystem).materializer

  private val slackConfig = SlackConfig(ConfigFactory.defaultReference.resolve, logger, () => mock[MethodsClient])

  implicit val slackClient = new SlackClient {
    override def chatUpdate(text: String, slackMessage: tinyakkaslackqueue.SlackMessage): Try[ChatUpdateResponse] = ???

    override def chatUpdateBlocks(blocks: tinyakkaslackqueue.SlackBlocksAsString, slackPost: tinyakkaslackqueue.SlackMessage): Try[ChatUpdateResponse] = ???

    override def pinsAdd(slackMessage: tinyakkaslackqueue.SlackMessage): Try[PinsAddResponse] = ???

    override def pinsRemove(slackMessage: tinyakkaslackqueue.SlackMessage): Try[PinsRemoveResponse] = ???

    override def viewsPublish(userId: tinyakkaslackqueue.SlackUserId, view: SlackView): Try[ViewsPublishResponse] = ???

//    override def client: MethodsClient = ???

    override def viewsUpdate(viewId: String, slackView: SlackView): Try[ViewsUpdateResponse] = ???

    override def viewsOpen(slackTriggerId: SlackTriggerId, slackView: SlackView): Try[ViewsOpenResponse] = ???

    override def threadReplies(messageItem: PinsListResponse.MessageItem): Try[ConversationsRepliesResponse] = ???

    override def slackConfig: SlackConfig = slackConfig

    override def pinsList(channel: SlackChannel): Try[Iterable[PinsListResponse.MessageItem]] = ???

    override def chatPostMessageInThread(text: String, thread: SlackThread): Try[ChatPostMessageResponse] = ???

    override def chatPostMessage(text: String, channel: SlackChannel): Try[ChatPostMessageResponse] = ???

    override def threadReplies(slackThread: SlackThread): Try[ConversationsRepliesResponse] = ???

    override def userZonedId(slackUserId: SlackUserId): ZoneId = ???

    override def allChannels: Try[Seq[Conversation]] = ???
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
  ), ConfigFactory.defaultReference.resolve)

  def toScheduledTask(slackTask: SlackTask): ScheduledSlackTask = new InteractiveJavaUtilTimer[SlackTs, SlackTask].ScheduledTask(
    slackTask,
    LocalDateTime.of(2100, 1, 1, 12, 30, 0),
    isRunning = false
  )
}
