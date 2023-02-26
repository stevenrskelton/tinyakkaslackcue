package ca.stevenskelton.tinyakkaslackqueue.api

import ca.stevenskelton.tinyakkaslackqueue._
import ca.stevenskelton.tinyakkaslackqueue.util.DateUtils
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
import scala.util.control.{NoStackTrace, NonFatal}
import scala.util.{Failure, Success, Try}

case class SlackClientImpl(slackConfig: SlackConfig)(implicit logger: Logger) extends SlackClient {

  def logError[T <: SlackApiTextResponse](call: String, body: String, f: (String, MethodsClient) => T): Try[T] = {
    slackConfig.clientOption.map {
      methodsClient =>
        try {
          val result = f(body, methodsClient)
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
          Success(result)
        } catch {
          case NonFatal(ex) =>
            logger.error(call, ex)
            Failure(ex)
        }
    }.getOrElse {
      Failure(new Exception("Slack not initialized") with NoStackTrace)
    }
  }

  def logError[T <: SlackApiTextResponse](call: String, f: MethodsClient => T): Try[T] = {
    slackConfig.clientOption.map {
      methodsClient =>
        try {
          val result = f(methodsClient)
          if (!result.isOk) {
            if (result.getError == "missing_scope") {
              logger.error(s"Missing permission scope for $call: ${result.getNeeded}")
            } else {
              logger.warn(s"$call:${result.getClass.getName} ${result.getError} ${result.getWarning}")
              val st = Thread.currentThread.getStackTrace.map(_.toString).mkString("\n")
              logger.debug(st)
            }
          }
          Success(result)
        } catch {
          case NonFatal(ex) =>
            logger.error(call, ex)
            Failure(ex)
        }
    }.getOrElse {
      Failure(new Exception("Slack not initialized") with NoStackTrace)
    }
  }

  override def chatUpdate(text: String, slackMessage: SlackMessage): Try[ChatUpdateResponse] = logError("chatUpdate", text,
    (body, client) => client.chatUpdate((r: ChatUpdateRequest.ChatUpdateRequestBuilder) => r.token(slackConfig.botOAuthToken).channel(slackMessage.channel.id).ts(slackMessage.ts.value).text(body))
  )

  override def chatUpdateBlocks(blocks: SlackBlocksAsString, slackMessage: SlackMessage): Try[ChatUpdateResponse] = logError("chatUpdateBlocks", blocks.value,
    (body, client) => client.chatUpdate((r: ChatUpdateRequest.ChatUpdateRequestBuilder) => r.token(slackConfig.botOAuthToken).channel(slackMessage.channel.id).ts(slackMessage.ts.value).blocksAsString(body))
  )

  override def pinsAdd(slackMessage: SlackMessage): Try[PinsAddResponse] = logError("pinsAdd",
    _.pinsAdd((r: PinsAddRequest.PinsAddRequestBuilder) => r.token(slackConfig.botOAuthToken).channel(slackMessage.channel.id).timestamp(slackMessage.ts.value))
  )

  override def pinsRemove(slackMessage: SlackMessage): Try[PinsRemoveResponse] = logError("pinsRemove",
    _.pinsRemove((r: PinsRemoveRequest.PinsRemoveRequestBuilder) => r.token(slackConfig.botOAuthToken).channel(slackMessage.channel.id).timestamp(slackMessage.ts.value))
  )

  override def pinsList(channel: SlackChannel): Try[Iterable[MessageItem]] = logError("pinsList",
    _.pinsList((r: PinsListRequest.PinsListRequestBuilder) => r.token(slackConfig.botOAuthToken).channel(channel.id))
  ).map(_.getItems.asScala)

  override def chatPostMessageInThread(text: String, thread: SlackThread): Try[ChatPostMessageResponse] = logError("chatPostMessageInThread", text,
    (body, client) => client.chatPostMessage((r: ChatPostMessageRequest.ChatPostMessageRequestBuilder) => r.token(slackConfig.botOAuthToken).channel(thread.channel.id).text(body).threadTs(thread.ts.value))
  )

  override def chatPostMessage(text: String, channel: SlackChannel): Try[ChatPostMessageResponse] = logError("chatPostMessage", text,
    (body, client) => client.chatPostMessage((r: ChatPostMessageRequest.ChatPostMessageRequestBuilder) => r.token(slackConfig.botOAuthToken).channel(channel.id).text(body))
  )

  override def viewsUpdate(viewId: String, slackView: SlackView): Try[ViewsUpdateResponse] = logError("viewUpdate", slackView.toString,
    (body, client) => client.viewsUpdate((r: ViewsUpdateRequest.ViewsUpdateRequestBuilder) => r.token(slackConfig.botOAuthToken).viewId(viewId).viewAsString(body))
  )

  override def viewsPublish(userId: SlackUserId, slackView: SlackView): Try[ViewsPublishResponse] = logError("viewsPublish", slackView.toString,
    (body, client) => client.viewsPublish((r: ViewsPublishRequest.ViewsPublishRequestBuilder) => r.token(slackConfig.botOAuthToken).userId(userId.value).viewAsString(body))
  )

  override def viewsOpen(slackTriggerId: SlackTriggerId, slackView: SlackView): Try[ViewsOpenResponse] = logError("viewsOpen", slackView.toString,
    (body, client) => client.viewsOpen((r: ViewsOpenRequest.ViewsOpenRequestBuilder) => r.token(slackConfig.botOAuthToken).triggerId(slackTriggerId.value).viewAsString(body))
  )

  override def threadReplies(messageItem: MessageItem): Try[ConversationsRepliesResponse] = logError("threadRepliesMessage",
    _.conversationsReplies((r: ConversationsRepliesRequest.ConversationsRepliesRequestBuilder) => r.token(slackConfig.botOAuthToken).channel(messageItem.getChannel).ts(messageItem.getMessage.getTs))
  )

  override def threadReplies(slackThread: SlackThread): Try[ConversationsRepliesResponse] = logError("threadReplies",
    _.conversationsReplies((r: ConversationsRepliesRequest.ConversationsRepliesRequestBuilder) => r.token(slackConfig.botOAuthToken).channel(slackThread.channel.id).ts(slackThread.ts.value))
  )

  override def allChannels: Try[Seq[Conversation]] = logError("threadReplies", {
    _.conversationsList((r: ConversationsListRequest.ConversationsListRequestBuilder) => r.token(slackConfig.botOAuthToken).types(Seq(ConversationType.PUBLIC_CHANNEL).asJava))
  }).map {
    conversationsResult =>
      slackConfig.allChannels = conversationsResult.getChannels.asScala.toSeq
      slackConfig.allChannels
  }

  override def userZonedId(slackUserId: SlackUserId): ZoneId = {
    if (slackUserId == SlackUser.System.id) {
      DateUtils.NewYorkZoneId
    } else {
      logError("usersInfo", {
        _.usersInfo((r: UsersInfoRequest.UsersInfoRequestBuilder) => r.token(slackConfig.botOAuthToken).user(slackUserId.value))
      }).map(o => ZoneId.of(o.getUser.getTz)).getOrElse(DateUtils.NewYorkZoneId)
    }
  }

}
