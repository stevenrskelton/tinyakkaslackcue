package ca.stevenskelton.tinyakkaslackqueue.api

import ca.stevenskelton.tinyakkaslackqueue._
import ca.stevenskelton.tinyakkaslackqueue.api.SlackClient.SlackConfig
import ca.stevenskelton.tinyakkaslackqueue.views.SlackView
import com.slack.api.Slack
import com.slack.api.methods.MethodsClient
import com.slack.api.methods.request.chat.{ChatPostMessageRequest, ChatUpdateRequest}
import com.slack.api.methods.request.conversations.ConversationsListRequest
import com.slack.api.methods.request.pins.{PinsAddRequest, PinsListRequest}
import com.slack.api.methods.request.users.UsersListRequest
import com.slack.api.methods.response.chat.{ChatPostMessageResponse, ChatUpdateResponse}
import com.slack.api.methods.response.conversations.ConversationsRepliesResponse
import com.slack.api.methods.response.pins.PinsListResponse.MessageItem
import com.slack.api.methods.response.pins.{PinsAddResponse, PinsRemoveResponse}
import com.slack.api.methods.response.views.{ViewsOpenResponse, ViewsPublishResponse, ViewsUpdateResponse}
import com.slack.api.model.{Conversation, ConversationType}
import com.typesafe.config.Config
import org.slf4j.Logger
import play.api.libs.json.Json

import java.time.ZoneId
import scala.jdk.CollectionConverters.{CollectionHasAsScala, SeqHasAsJava}

object SlackClient {

  case class SlackConfig(
                          botOAuthToken: String,
                          botUserId: SlackUserId,
                          botChannel: BotChannel,
                          client: MethodsClient
                        ) {

    def persistConfig(slackFactories: SlackFactories)(implicit logger: Logger): Boolean = {
      val json = Json.obj(
        "taskchannels" -> slackFactories.slackTasks.collect {
          case SlackTaskInitialized(slackTaskFactory, Some(slackTaskMeta)) => Json.obj(
            "task" -> slackTaskFactory.name.getText,
            "channelId" -> slackTaskMeta.taskLogChannel.id,
            "queueTs" -> slackTaskMeta.queueThread.ts.value
          )
        }
      )
      val message = s"${SlackFactories.ConfigurationThreadHeader}```${Json.prettyPrint(json)}```"
      val pinnedMessages = Option(client.pinsList((r: PinsListRequest.PinsListRequestBuilder) => r.token(botOAuthToken).channel(botChannel.id)).getItems).map(_.asScala.filter(_.getCreatedBy == botUserId.value)).getOrElse(Nil)
      val slackApiTextResponse = pinnedMessages.find(_.getMessage.getText.startsWith(SlackFactories.ConfigurationThreadHeader)).map {
        pinnedConfig => client.chatUpdate((r: ChatUpdateRequest.ChatUpdateRequestBuilder) => r.token(botOAuthToken).channel(botChannel.id).ts(pinnedConfig.getMessage.getTs).text(message))
      }.getOrElse {
        val postMessage = client.chatPostMessage((r: ChatPostMessageRequest.ChatPostMessageRequestBuilder) => r.token(botOAuthToken).channel(botChannel.id).text(message))
        if (postMessage.isOk) {
          client.pinsAdd((r: PinsAddRequest.PinsAddRequestBuilder) => r.token(botOAuthToken).channel(botChannel.id).timestamp(postMessage.getTs))
        } else {
          postMessage
        }
      }
      if (slackApiTextResponse.isOk) true
      else {
        logger.error(s"SlackConfig.persist: ${slackApiTextResponse.getError}")
        false
      }
    }
  }

  def initialize(config: Config): SlackConfig = {
    val botOAuthToken = config.getString("secrets.botOAuthToken")
    val botUserName = config.getString("secrets.botUserName")
    val botChannelName = config.getString("secrets.botChannelName")

    val client = Slack.getInstance.methods

    val findBotUserQuery = client.usersList((r: UsersListRequest.UsersListRequestBuilder) => r.token(botOAuthToken))
    val botUser = findBotUserQuery.getMembers.asScala.find(o => o.isBot && o.getName == botUserName.toLowerCase).get
    val botUserId = SlackUserId(botUser.getId)
    val conversationsResult = client.conversationsList((r: ConversationsListRequest.ConversationsListRequestBuilder) => r.token(botOAuthToken).types(Seq(ConversationType.PUBLIC_CHANNEL).asJava))
    val channels = Option(conversationsResult.getChannels.asScala).getOrElse(Nil)
    channels.find(_.getName == botChannelName).map {
      botChannel => SlackConfig(botOAuthToken, botUserId, BotChannel(botChannel.getId), client)
    }.getOrElse {
      throw new Exception(s"Could not find bot channel $botChannelName")
    }
  }

}

trait SlackClient {
  def slackConfig: SlackConfig

  def client: MethodsClient

  def chatUpdate(text: String, slackMessage: SlackMessage): ChatUpdateResponse

  def chatUpdateBlocks(blocks: SlackBlocksAsString, slackPost: SlackMessage): ChatUpdateResponse

  def pinsAdd(slackMessage: SlackMessage): PinsAddResponse

  def pinsRemove(slackMessage: SlackMessage): PinsRemoveResponse

  def pinsList(channel: SlackChannel): Iterable[MessageItem]

  def chatPostMessageInThread(text: String, thread: SlackThread): ChatPostMessageResponse

  def chatPostMessage(text: String, channel: SlackChannel): ChatPostMessageResponse

  def viewsUpdate(viewId: String, slackView: SlackView): ViewsUpdateResponse

  def viewsPublish(userId: SlackUserId, slackView: SlackView): ViewsPublishResponse

  def viewsOpen(slackTriggerId: SlackTriggerId, slackView: SlackView): ViewsOpenResponse

  def threadReplies(slackThread: SlackThread): ConversationsRepliesResponse

  def threadReplies(messageItem: MessageItem): ConversationsRepliesResponse

  def userZonedId(slackUserId: SlackUserId): ZoneId

  def allChannels: Seq[Conversation]
}
