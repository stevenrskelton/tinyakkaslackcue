package ca.stevenskelton.tinyakkaslackcue

import ca.stevenskelton.tinyakkaslackcue.blocks.{SlackTaskThread, TaskHistory}
import ca.stevenskelton.tinyakkaslackcue.blocks.SlackTaskThread.Fields
import com.slack.api.Slack
import com.slack.api.methods.MethodsClient
import com.slack.api.methods.request.apps.connections.AppsConnectionsOpenRequest
import com.slack.api.methods.request.bots.BotsInfoRequest
import com.slack.api.methods.request.chat.{ChatPostMessageRequest, ChatUpdateRequest}
import com.slack.api.methods.request.conversations.{ConversationsListRequest, ConversationsOpenRequest}
import com.slack.api.methods.request.pins.{PinsAddRequest, PinsListRequest, PinsRemoveRequest}
import com.slack.api.methods.request.users.UsersListRequest
import com.slack.api.methods.request.views.{ViewsOpenRequest, ViewsPublishRequest}
import com.slack.api.methods.response.chat.{ChatPostMessageResponse, ChatUpdateResponse}
import com.slack.api.methods.response.pins.PinsListResponse.MessageItem
import com.slack.api.methods.response.pins.{PinsAddResponse, PinsRemoveResponse}
import com.slack.api.methods.response.views.{ViewsOpenResponse, ViewsPublishResponse}
import com.slack.api.model.ConversationType
import com.typesafe.config.Config

import scala.concurrent.Future
import scala.jdk.CollectionConverters.{CollectionHasAsScala, SeqHasAsJava}

object SlackClient {

  val HistoryThreadText = "Task History"

  def initialize(config: Config): SlackClient = {
    val botOAuthToken = config.getString("secrets.botOAuthToken")
    val botUserName = config.getString("secrets.botUserName")
    val botChannelName = config.getString("secrets.botChannelName")

//    val botUserId = SlackUserId(config.getString("secrets.botUserId"))
//    val botChannelId = config.getString("secrets.botChannelId")

    val client = Slack.getInstance.methods

    val findBotUserQuery = client.usersList((r: UsersListRequest.UsersListRequestBuilder) => r.token(botOAuthToken))
    val botUser = findBotUserQuery.getMembers.asScala.find(o => o.isBot && o.getName == botUserName.toLowerCase).get
//    val bot = client.botsInfo((r: BotsInfoRequest.BotsInfoRequestBuilder) => r.token(botOAuthToken).bot("tradeaudittaskmanager")).getBot
    val botUserId = SlackUserId(botUser.getId)
    val conversationsResult = client.conversationsList((r: ConversationsListRequest.ConversationsListRequestBuilder) => r.token(botOAuthToken).types(Seq(ConversationType.PUBLIC_CHANNEL).asJava))
    val channels = conversationsResult.getChannels.asScala
    val botChannelId = channels.find(_.getName == botChannelName).get.getId

    val pinnedMessages = client.pinsList((r: PinsListRequest.PinsListRequestBuilder) => r.token(botOAuthToken).channel(botChannelId)).getItems.asScala
    val pinnedResult = pinnedMessages.filter {
      o => o.getCreatedBy == botUserId.value && o.getMessage.getText == HistoryThreadText
    }
    val pinnedTs = pinnedResult.headOption.map { o =>
      //TODO: parse history
      SlackTs(o.getMessage.getTs)
    }.getOrElse {
      val pinnedMessageResult = client.chatPostMessage((r: ChatPostMessageRequest.ChatPostMessageRequestBuilder) => r.token(botOAuthToken).channel(botChannelId).text(HistoryThreadText))
      client.pinsAdd((r: PinsAddRequest.PinsAddRequestBuilder) => r.token(botOAuthToken).channel(botChannelId).timestamp(pinnedMessageResult.getTs))
      SlackTs(pinnedMessageResult.getTs)
    }

    new SlackClient(botOAuthToken, botUserId, botChannelId, pinnedTs, client)
  }
}

class SlackClient(val botOAuthToken: String, botUserId: SlackUserId, botChannelId: String, historyThread: SlackTs, client: MethodsClient) {

  def getHistory() : Seq[TaskHistory] = {
    Nil
  }

  def chatUpdate(text: String, ts: SlackTs): ChatUpdateResponse = {
    client.chatUpdate((r: ChatUpdateRequest.ChatUpdateRequestBuilder) => r.token(botOAuthToken).channel(botChannelId).ts(ts.value).text(text))
  }

  def chatUpdateBlocks(blocks: SlackBlocksAsString, ts: SlackTs): ChatUpdateResponse = {
    client.chatUpdate((r: ChatUpdateRequest.ChatUpdateRequestBuilder) => r.token(botOAuthToken).channel(botChannelId).ts(ts.value).blocksAsString(blocks.value))
  }

  def pinsAdd(ts: SlackTs): PinsAddResponse = {
    client.pinsAdd((r: PinsAddRequest.PinsAddRequestBuilder) => r.token(botOAuthToken).channel(botChannelId).timestamp(ts.value))
  }

  def pinsRemove(ts: SlackTs): PinsRemoveResponse = {
    client.pinsRemove((r: PinsRemoveRequest.PinsRemoveRequestBuilder) => r.token(botOAuthToken).channel(botChannelId).timestamp(ts.value))
  }

  def pinsList(): Iterable[MessageItem] = {
    client.pinsList((r: PinsListRequest.PinsListRequestBuilder) => r.token(botOAuthToken).channel(botChannelId)).getItems.asScala
  }

  def pinnedTasks(slackTaskFactories: SlackTaskFactories): Iterable[(SlackTask, Fields)] = {
    pinsList().flatMap(SlackTaskThread.parse(_, this, slackTaskFactories))
  }

  def chatPostMessageInThread(text: String, thread: SlackTs): ChatPostMessageResponse = {
    client.chatPostMessage((r: ChatPostMessageRequest.ChatPostMessageRequestBuilder) => r.token(botOAuthToken).channel(botChannelId).text(text).threadTs(thread.value))
  }

  def chatPostMessage(text: String): ChatPostMessageResponse = {
    client.chatPostMessage((r: ChatPostMessageRequest.ChatPostMessageRequestBuilder) => r.token(botOAuthToken).channel(botChannelId).text(text))
  }

  def viewsPublish(userId: SlackUserId, viewName: String, blocks: SlackBlocksAsString): ViewsPublishResponse = {
    val viewAsString = s"{\"type\":\"$viewName\",\"blocks\":[${blocks.value}]}"
    client.viewsPublish((r: ViewsPublishRequest.ViewsPublishRequestBuilder) => r.token(botOAuthToken).userId(userId.value).viewAsString(viewAsString))
  }

  def viewsOpen(slackTriggerId: SlackTriggerId, view: SlackBlocksAsString): ViewsOpenResponse = {
    client.viewsOpen((r: ViewsOpenRequest.ViewsOpenRequestBuilder) => r.token(botOAuthToken).triggerId(slackTriggerId.value).viewAsString(view.value))
  }

}

