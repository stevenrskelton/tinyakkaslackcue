package ca.stevenskelton.tinyakkaslackcue.eventhandlers

import akka.Done
import ca.stevenskelton.tinyakkaslackcue.blocks.HomeTab
import ca.stevenskelton.tinyakkaslackcue.{SlackBlocksAsString, SlackClient, SlackTaskFactories, SlackUserId}
import org.slf4j.Logger
import play.api.libs.json.{JsObject, JsValue}

import scala.concurrent.Future

object AppHomeOpened {
  def apply(slackClient: SlackClient, slackTaskFactories: SlackTaskFactories, jsObject: JsObject)(implicit logger: Logger): Future[Done] = {
    //    val blocks = (jsObject \ "view" \ "blocks").as[Seq[JsValue]]
    //    if (blocks.isEmpty) {
    //      HomeTab.parseView(blocks).fold(Future.failed(_), _ => Future.successful(Done))
    val userId = SlackUserId((jsObject \ "user").as[String])
    val viewBlocks = HomeTab.initialize(slackClient, slackTaskFactories)
    val blocks = SlackBlocksAsString(viewBlocks.map(_.toBlocks.value).mkString(""",{"type": "divider"},"""))
    val response = slackClient.viewsPublish(userId, "home", blocks)
    if (response.isOk) {
      logger.debug(s"Updated home view for ${userId.value}")
      Future.successful(Done)
    } else {
      logger.error(s"Home view update failed: ${response.getError}")
      Future.failed(new Exception(response.getError))
    }
    //    } else {
    //      //TODO: compare to hash
    //      Future.successful(Done)
    //    }
  }
}
