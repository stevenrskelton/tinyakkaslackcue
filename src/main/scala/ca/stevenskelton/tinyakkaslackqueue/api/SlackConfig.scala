package ca.stevenskelton.tinyakkaslackqueue.api

import ca.stevenskelton.tinyakkaslackqueue._
import com.slack.api.Slack
import com.slack.api.methods.MethodsClient
import com.slack.api.methods.request.chat.{ChatPostMessageRequest, ChatUpdateRequest}
import com.slack.api.methods.request.conversations.ConversationsListRequest
import com.slack.api.methods.request.pins.{PinsAddRequest, PinsListRequest}
import com.slack.api.methods.request.users.UsersListRequest
import com.slack.api.model.{Conversation, ConversationType}
import com.typesafe.config.Config
import org.slf4j.Logger
import play.api.libs.json.Json

import scala.jdk.CollectionConverters.{CollectionHasAsScala, SeqHasAsJava}
import scala.util.control.NonFatal

object SlackConfig {
  def apply(config: Config, logger: Logger): SlackConfig = {
    val returnValue = SlackConfig(
      botOAuthToken = config.getString("secrets.botOAuthToken"),
      botUserName = config.getString("secrets.botUserName"),
      botChannelName = config.getString("secrets.botChannelName"),
      logger,
      methodsClient = None,
      botUserId = SlackUserId(""),
      botChannel = BotChannel(""),
      allChannels = Nil
    )
    //non-lazy initialize
    returnValue.clientOption

    returnValue
  }
}

case class SlackConfig private(
                                botOAuthToken: String,
                                botUserName: String,
                                botChannelName: String,
                                logger: Logger,
                                private var methodsClient: Option[MethodsClient],
                                var botUserId: SlackUserId,
                                var botChannel: BotChannel,
                                var allChannels: Seq[Conversation]
                              ) {

  def clientOption: Option[MethodsClient] = {
    if (methodsClient.isDefined) methodsClient
    else {
      try {
        val client = Slack.getInstance.methods

        if (botUserId.value.isEmpty) {
          try {
            val findBotUserQuery = client.usersList((r: UsersListRequest.UsersListRequestBuilder) => r.token(botOAuthToken))
            val botUser = findBotUserQuery.getMembers.asScala.find(o => o.isBot && o.getName == botUserName.toLowerCase).get
            botUserId = SlackUserId(botUser.getId)
          } catch {
            case NonFatal(ex) =>
              logger.error("SlackClient failed botUserId", ex)
              return None
          }
        }

        if (botChannel.id.isEmpty) {
          try {
            val conversationsResult = client.conversationsList((r: ConversationsListRequest.ConversationsListRequestBuilder) => r.token(botOAuthToken).types(Seq(ConversationType.PUBLIC_CHANNEL).asJava))
            Option(conversationsResult.getChannels.asScala).foreach {
              channels => allChannels = channels.toSeq
            }
          } catch {
            case NonFatal(ex) => logger.error("SlackClient failed botChannel", ex)
              return None
          }
          allChannels.find(_.getName == botChannelName).map {
            foundBotChannel =>
              botChannel = BotChannel(foundBotChannel.getId)
          }.getOrElse {
            val ex = new Exception(s"Could not find bot channel $botChannelName")
            logger.error("SlackClient bot config", ex)
            throw ex
          }
        }

        methodsClient = Some(client)
        methodsClient
      } catch {
        case NonFatal(ex) =>
          logger.error("SlackClient failed getInstance", ex)
          None
      }
    }
  }

  def persistConfig(slackFactories: SlackFactories): Boolean = {
    val json = Json.obj(
      "taskchannels" -> slackFactories.slackTasks.collect {
        case SlackTaskInitialized(slackTaskFactory, Some(slackTaskMeta)) => Json.obj(
          "task" -> slackTaskFactory.name.getText,
          "channelId" -> slackTaskMeta.taskLogChannel.id,
          "queueTs" -> slackTaskMeta.queueThread.ts.value
        )
      }
    )
    clientOption.map {
      client =>
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
    }.getOrElse {
      false
    }
  }
}