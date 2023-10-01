package ca.stevenskelton.tinyakkaslackqueue.api

import ca.stevenskelton.tinyakkaslackqueue.*
import ca.stevenskelton.tinyakkaslackqueue.views.SlackView
import com.slack.api.methods.response.chat.{ChatPostMessageResponse, ChatUpdateResponse}
import com.slack.api.methods.response.conversations.ConversationsRepliesResponse
import com.slack.api.methods.response.pins.PinsListResponse.MessageItem
import com.slack.api.methods.response.pins.{PinsAddResponse, PinsRemoveResponse}
import com.slack.api.methods.response.views.{ViewsOpenResponse, ViewsPublishResponse, ViewsUpdateResponse}
import com.slack.api.model.Conversation

import java.time.ZoneId
import scala.util.Try

trait SlackClient {
  def slackConfig: SlackConfig

  def chatUpdate(text: String, slackMessage: SlackMessage): Try[ChatUpdateResponse]

  def chatUpdateBlocks(blocks: SlackBlocksAsString, slackPost: SlackMessage): Try[ChatUpdateResponse]

  def pinsAdd(slackMessage: SlackMessage): Try[PinsAddResponse]

  def pinsRemove(slackMessage: SlackMessage): Try[PinsRemoveResponse]

  def pinsList(channel: SlackChannel): Try[Iterable[MessageItem]]

  def chatPostMessageInThread(text: String, thread: SlackThread): Try[ChatPostMessageResponse]

  def chatPostMessage(text: String, channel: SlackChannel): Try[ChatPostMessageResponse]

  def viewsUpdate(viewId: String, slackView: SlackView): Try[ViewsUpdateResponse]

  def viewsPublish(userId: SlackUserId, slackView: SlackView): Try[ViewsPublishResponse]

  def viewsOpen(slackTriggerId: SlackTriggerId, slackView: SlackView): Try[ViewsOpenResponse]

  def threadReplies(slackThread: SlackThread): Try[ConversationsRepliesResponse]

  def threadReplies(messageItem: MessageItem): Try[ConversationsRepliesResponse]

  def userZonedId(slackUserId: SlackUserId): ZoneId

  def allChannels: Try[Seq[Conversation]]
}
