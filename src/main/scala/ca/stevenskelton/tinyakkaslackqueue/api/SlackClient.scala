package ca.stevenskelton.tinyakkaslackqueue.api

import ca.stevenskelton.tinyakkaslackqueue._
import ca.stevenskelton.tinyakkaslackqueue.views.SlackView
import com.slack.api.methods.response.chat.{ChatPostMessageResponse, ChatUpdateResponse}
import com.slack.api.methods.response.conversations.ConversationsRepliesResponse
import com.slack.api.methods.response.pins.PinsListResponse.MessageItem
import com.slack.api.methods.response.pins.{PinsAddResponse, PinsRemoveResponse}
import com.slack.api.methods.response.views.{ViewsOpenResponse, ViewsPublishResponse, ViewsUpdateResponse}
import com.slack.api.model.Conversation

import java.time.ZoneId

trait SlackClient {
  def slackConfig: SlackConfig

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
