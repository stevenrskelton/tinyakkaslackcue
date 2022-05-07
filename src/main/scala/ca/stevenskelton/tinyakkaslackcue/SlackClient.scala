package ca.stevenskelton.tinyakkaslackcue

import akka.stream.Materializer
import ca.stevenskelton.tinyakkaslackcue.blocks.SlackTaskThread
import ca.stevenskelton.tinyakkaslackcue.blocks.SlackTaskThread.Fields
import com.slack.api.Slack
import com.slack.api.methods.request.chat.{ChatPostMessageRequest, ChatUpdateRequest}
import com.slack.api.methods.request.pins.{PinsAddRequest, PinsListRequest, PinsRemoveRequest}
import com.slack.api.methods.request.views.{ViewsOpenRequest, ViewsPublishRequest}
import com.slack.api.methods.response.chat.{ChatPostMessageResponse, ChatUpdateResponse}
import com.slack.api.methods.response.pins.PinsListResponse.MessageItem
import com.slack.api.methods.response.pins.{PinsAddResponse, PinsRemoveResponse}
import com.slack.api.methods.response.views.{ViewsOpenResponse, ViewsPublishResponse}
import com.typesafe.config.Config

import scala.jdk.CollectionConverters.CollectionHasAsScala

object SlackClient {
  def apply(config: Config): SlackClient = {
    val botOAuthToken = config.getString("secrets.botOAuthToken")
    val botUserId = SlackUserId(config.getString("secrets.botUserId"))
    val botChannelId = config.getString("secrets.botChannelId")
    new SlackClient(botOAuthToken, botUserId, botChannelId)
  }
}

class SlackClient(val botOAuthToken: String, botUserId: SlackUserId, botChannelId: String) {

  val client = Slack.getInstance.methods

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

