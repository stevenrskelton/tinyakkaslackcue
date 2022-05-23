package ca.stevenskelton.tinyakkaslackqueue

import ca.stevenskelton.tinyakkaslackqueue.blocks.SlackTaskThread
import ca.stevenskelton.tinyakkaslackqueue.blocks.SlackTaskThread.Fields
import ca.stevenskelton.tinyakkaslackqueue.views.SlackView
import com.slack.api.Slack
import com.slack.api.methods.MethodsClient
import com.slack.api.methods.request.chat.{ChatPostMessageRequest, ChatUpdateRequest}
import com.slack.api.methods.request.conversations.{ConversationsHistoryRequest, ConversationsListRequest, ConversationsRepliesRequest}
import com.slack.api.methods.request.pins.{PinsAddRequest, PinsListRequest, PinsRemoveRequest}
import com.slack.api.methods.request.users.UsersListRequest
import com.slack.api.methods.request.views.{ViewsOpenRequest, ViewsPublishRequest, ViewsUpdateRequest}
import com.slack.api.methods.response.chat.{ChatPostMessageResponse, ChatUpdateResponse}
import com.slack.api.methods.response.conversations.ConversationsRepliesResponse
import com.slack.api.methods.response.pins.PinsListResponse.MessageItem
import com.slack.api.methods.response.pins.{PinsAddResponse, PinsRemoveResponse}
import com.slack.api.methods.response.views.{ViewsOpenResponse, ViewsPublishResponse, ViewsUpdateResponse}
import com.slack.api.model.ConversationType
import com.typesafe.config.Config

import scala.jdk.CollectionConverters.{CollectionHasAsScala, SeqHasAsJava}

object SlackClient {

  val HistoryThreadText = "Task History"
  val ChannelThreadText = "Task Channels"

  def initialize(config: Config): SlackClient = {
    val botOAuthToken = config.getString("secrets.botOAuthToken")
    val botUserName = config.getString("secrets.botUserName")
    val botChannelName = config.getString("secrets.botChannelName")

    //    val botUserId = SlackUserId(config.getString("secrets.botUserId"))
    //    val botChannelId = config.getString("secrets.botChannelId")

    val client = Slack.getInstance.methods

    val findBotUserQuery = client.usersList((r: UsersListRequest.UsersListRequestBuilder) => r.token(botOAuthToken))
    val botUser = findBotUserQuery.getMembers.asScala.find(o => o.isBot && o.getName == botUserName.toLowerCase).get
    val botUserId = SlackUserId(botUser.getId)
    val conversationsResult = client.conversationsList((r: ConversationsListRequest.ConversationsListRequestBuilder) => r.token(botOAuthToken).types(Seq(ConversationType.PUBLIC_CHANNEL).asJava))
    val channels = conversationsResult.getChannels.asScala
    channels.find(_.getName == botChannelName).map {
      botChannel => new SlackClientImpl(botOAuthToken, botUserId, SlackChannel(botChannel.getId), client)
    }.getOrElse {
      throw new Exception(s"Could not find channel $botChannelName")
    }
  }
}

trait SlackClient {
  def client: MethodsClient

  def botOAuthToken: String

  def botUserId: SlackUserId

  def botChannel: SlackChannel

  def chatUpdate(text: String, ts: SlackTs): ChatUpdateResponse

  def chatUpdateBlocks(blocks: SlackBlocksAsString, ts: SlackTs): ChatUpdateResponse

  def pinsAdd(ts: SlackTs): PinsAddResponse

  def pinsRemove(ts: SlackTs): PinsRemoveResponse

  def pinsList(): Iterable[MessageItem]

//  def pinnedTasks(slackTaskFactories: SlackFactories): Iterable[(SlackTask, Fields)]

  def chatPostMessageInThread(text: String, thread: SlackTs): ChatPostMessageResponse

  def chatPostMessage(text: String): ChatPostMessageResponse

  def viewsUpdate(viewId: String, slackView: SlackView): ViewsUpdateResponse

  def viewsPublish(userId: SlackUserId, slackView: SlackView): ViewsPublishResponse

  def viewsOpen(slackTriggerId: SlackTriggerId, slackView: SlackView): ViewsOpenResponse

  def threadReplies(messageItem: MessageItem): ConversationsRepliesResponse
}

class SlackClientImpl(val botOAuthToken: String, val botUserId: SlackUserId, val botChannel: SlackChannel, val client: MethodsClient) extends SlackClient {

  override def chatUpdate(text: String, ts: SlackTs): ChatUpdateResponse = {
    client.chatUpdate((r: ChatUpdateRequest.ChatUpdateRequestBuilder) => r.token(botOAuthToken).channel(botChannel.value).ts(ts.value).text(text))
  }

  override def chatUpdateBlocks(blocks: SlackBlocksAsString, ts: SlackTs): ChatUpdateResponse = {
    client.chatUpdate((r: ChatUpdateRequest.ChatUpdateRequestBuilder) => r.token(botOAuthToken).channel(botChannel.value).ts(ts.value).blocksAsString(blocks.value))
  }

  override def pinsAdd(ts: SlackTs): PinsAddResponse = {
    client.pinsAdd((r: PinsAddRequest.PinsAddRequestBuilder) => r.token(botOAuthToken).channel(botChannel.value).timestamp(ts.value))
  }

  override def pinsRemove(ts: SlackTs): PinsRemoveResponse = {
    client.pinsRemove((r: PinsRemoveRequest.PinsRemoveRequestBuilder) => r.token(botOAuthToken).channel(botChannel.value).timestamp(ts.value))
  }

  override def pinsList(): Iterable[MessageItem] = {
    client.pinsList((r: PinsListRequest.PinsListRequestBuilder) => r.token(botOAuthToken).channel(botChannel.value)).getItems.asScala
  }

//  override def pinnedTasks(slackTaskFactories: SlackFactories): Iterable[(SlackTask, Fields)] = {
//    pinsList().flatMap(SlackTaskThread.parse(_, slackTaskFactories))
//  }

  override def chatPostMessageInThread(text: String, thread: SlackTs): ChatPostMessageResponse = {
    client.chatPostMessage((r: ChatPostMessageRequest.ChatPostMessageRequestBuilder) => r.token(botOAuthToken).channel(botChannel.value).text(text).threadTs(thread.value))
  }

  override def chatPostMessage(text: String): ChatPostMessageResponse = {
    client.chatPostMessage((r: ChatPostMessageRequest.ChatPostMessageRequestBuilder) => r.token(botOAuthToken).channel(botChannel.value).text(text))
  }

  override def viewsUpdate(viewId: String, slackView: SlackView): ViewsUpdateResponse = {
    client.viewsUpdate((r: ViewsUpdateRequest.ViewsUpdateRequestBuilder) => r.token(botOAuthToken).viewId(viewId).viewAsString(slackView.toString))
  }

  override def viewsPublish(userId: SlackUserId, slackView: SlackView): ViewsPublishResponse = {
    client.viewsPublish((r: ViewsPublishRequest.ViewsPublishRequestBuilder) => r.token(botOAuthToken).userId(userId.value).viewAsString(slackView.toString))
  }

  override def viewsOpen(slackTriggerId: SlackTriggerId, slackView: SlackView): ViewsOpenResponse = {
    client.viewsOpen((r: ViewsOpenRequest.ViewsOpenRequestBuilder) => r.token(botOAuthToken).triggerId(slackTriggerId.value).viewAsString(slackView.toString))
  }

  override def threadReplies(messageItem: MessageItem): ConversationsRepliesResponse = {
    client.conversationsReplies((r: ConversationsRepliesRequest.ConversationsRepliesRequestBuilder) => r.token(botOAuthToken).ts(messageItem.getMessage.getTs))
  }

}

