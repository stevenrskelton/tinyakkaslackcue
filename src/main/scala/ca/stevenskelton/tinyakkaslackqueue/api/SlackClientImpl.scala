package ca.stevenskelton.tinyakkaslackqueue.api

import ca.stevenskelton.tinyakkaslackqueue._
import ca.stevenskelton.tinyakkaslackqueue.api.SlackClient.SlackConfig
import ca.stevenskelton.tinyakkaslackqueue.views.SlackView
import com.slack.api.methods.request.chat.{ChatPostMessageRequest, ChatUpdateRequest}
import com.slack.api.methods.request.conversations.{ConversationsListRequest, ConversationsRepliesRequest}
import com.slack.api.methods.request.pins.{PinsAddRequest, PinsListRequest, PinsRemoveRequest}
import com.slack.api.methods.request.users.UsersInfoRequest
import com.slack.api.methods.request.views.{ViewsOpenRequest, ViewsPublishRequest, ViewsUpdateRequest}
import com.slack.api.methods.response.chat.{ChatPostMessageResponse, ChatUpdateResponse}
import com.slack.api.methods.response.conversations.ConversationsRepliesResponse
import com.slack.api.methods.response.pins.PinsListResponse.MessageItem
import com.slack.api.methods.response.pins.{PinsAddResponse, PinsRemoveResponse}
import com.slack.api.methods.response.views.{ViewsOpenResponse, ViewsPublishResponse, ViewsUpdateResponse}
import com.slack.api.methods.{MethodsClient, SlackApiTextResponse}
import com.slack.api.model.{Conversation, ConversationType}
import org.slf4j.Logger

import java.time.ZoneId
import scala.jdk.CollectionConverters.{CollectionHasAsScala, SeqHasAsJava}

case class SlackClientImpl(slackConfig: SlackConfig, client: MethodsClient)(implicit logger: Logger) extends SlackClient {

  def logError[T <: SlackApiTextResponse](call: String, body: String, f: String => T): T = {
    val result = f(body)
    if (!result.isOk) {
      if (result.getError == "not_in_channel") {
        logger.error(s"Add User to channel  permission scope for $call: ${result.getNeeded}")
      } else if (result.getError == "missing_scope") {
        logger.error(s"Missing permission scope for $call: ${result.getNeeded}")
      } else {
        logger.warn(s"$call:${result.getClass.getName} ${result.getError} ${result.getWarning}, body: $body")
        val st = Thread.currentThread.getStackTrace.map(_.toString).mkString("\n")
        logger.debug(st)
      }
    }
    result
  }

  def logError[T <: SlackApiTextResponse](call: String, result: T): T = {
    if (!result.isOk) {
      if (result.getError == "missing_scope") {
        logger.error(s"Missing permission scope for $call: ${result.getNeeded}")
      } else {
        logger.warn(s"$call:${result.getClass.getName} ${result.getError} ${result.getWarning}")
        val st = Thread.currentThread.getStackTrace.map(_.toString).mkString("\n")
        logger.debug(st)
      }
    }
    result
  }

  override def chatUpdate(text: String, slackMessage: SlackMessage): ChatUpdateResponse = logError("chatUpdate", text,
    body => client.chatUpdate((r: ChatUpdateRequest.ChatUpdateRequestBuilder) => r.token(slackConfig.botOAuthToken).channel(slackMessage.channel.id).ts(slackMessage.ts.value).text(body))
  )

  override def chatUpdateBlocks(blocks: SlackBlocksAsString, slackMessage: SlackMessage): ChatUpdateResponse = logError("chatUpdateBlocks", blocks.value,
    body => client.chatUpdate((r: ChatUpdateRequest.ChatUpdateRequestBuilder) => r.token(slackConfig.botOAuthToken).channel(slackMessage.channel.id).ts(slackMessage.ts.value).blocksAsString(body))
  )

  override def pinsAdd(slackMessage: SlackMessage): PinsAddResponse = logError("pinsAdd",
    client.pinsAdd((r: PinsAddRequest.PinsAddRequestBuilder) => r.token(slackConfig.botOAuthToken).channel(slackMessage.channel.id).timestamp(slackMessage.ts.value))
  )

  override def pinsRemove(slackMessage: SlackMessage): PinsRemoveResponse = logError("pinsRemove",
    client.pinsRemove((r: PinsRemoveRequest.PinsRemoveRequestBuilder) => r.token(slackConfig.botOAuthToken).channel(slackMessage.channel.id).timestamp(slackMessage.ts.value))
  )

  override def pinsList(channel: SlackChannel): Iterable[MessageItem] = logError("pinsList",
    client.pinsList((r: PinsListRequest.PinsListRequestBuilder) => r.token(slackConfig.botOAuthToken).channel(channel.id))
  ).getItems.asScala

  override def chatPostMessageInThread(text: String, thread: SlackThread): ChatPostMessageResponse = logError("chatPostMessageInThread", text,
    body => client.chatPostMessage((r: ChatPostMessageRequest.ChatPostMessageRequestBuilder) => r.token(slackConfig.botOAuthToken).channel(thread.channel.id).text(body).threadTs(thread.ts.value))
  )

  override def chatPostMessage(text: String, channel: SlackChannel): ChatPostMessageResponse = logError("chatPostMessage", text,
    body =>  client.chatPostMessage((r: ChatPostMessageRequest.ChatPostMessageRequestBuilder) => r.token(slackConfig.botOAuthToken).channel(channel.id).text(body))
  )

  override def viewsUpdate(viewId: String, slackView: SlackView): ViewsUpdateResponse = logError("viewUpdate", slackView.toString,
    body => client.viewsUpdate((r: ViewsUpdateRequest.ViewsUpdateRequestBuilder) => r.token(slackConfig.botOAuthToken).viewId(viewId).viewAsString(body))
  )

  override def viewsPublish(userId: SlackUserId, slackView: SlackView): ViewsPublishResponse = logError("viewsPublish", slackView.toString,
    body => client.viewsPublish((r: ViewsPublishRequest.ViewsPublishRequestBuilder) => r.token(slackConfig.botOAuthToken).userId(userId.value).viewAsString(body))
  )

  override def viewsOpen(slackTriggerId: SlackTriggerId, slackView: SlackView): ViewsOpenResponse = logError("viewsOpen", slackView.toString,
    body => client.viewsOpen((r: ViewsOpenRequest.ViewsOpenRequestBuilder) => r.token(slackConfig.botOAuthToken).triggerId(slackTriggerId.value).viewAsString(body))
  )

  override def threadReplies(messageItem: MessageItem): ConversationsRepliesResponse = logError("threadRepliesMessage",
    client.conversationsReplies((r: ConversationsRepliesRequest.ConversationsRepliesRequestBuilder) => r.token(slackConfig.botOAuthToken).channel(messageItem.getChannel).ts(messageItem.getMessage.getTs))
  )

  override def threadReplies(slackThread: SlackThread): ConversationsRepliesResponse = logError("threadReplies",
    client.conversationsReplies((r: ConversationsRepliesRequest.ConversationsRepliesRequestBuilder) => r.token(slackConfig.botOAuthToken).channel(slackThread.channel.id).ts(slackThread.ts.value))
  )

  override def allChannels: Seq[Conversation] = {
    val conversationsResult = client.conversationsList((r: ConversationsListRequest.ConversationsListRequestBuilder) => r.token(slackConfig.botOAuthToken).types(Seq(ConversationType.PUBLIC_CHANNEL).asJava))
    conversationsResult.getChannels.asScala.toSeq
  }

  override def userZonedId(slackUserId: SlackUserId): ZoneId = ZoneId.of {
    logError("usersInfo", {
      client.usersInfo((r: UsersInfoRequest.UsersInfoRequestBuilder) => r.token(slackConfig.botOAuthToken).user(slackUserId.value))
    }).getUser.getTz
  }

}
