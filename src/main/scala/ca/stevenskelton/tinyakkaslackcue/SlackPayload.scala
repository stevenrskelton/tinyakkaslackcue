package ca.stevenskelton.tinyakkaslackcue

import ca.stevenskelton.tinyakkaslackcue.SlackPayload.SlackPayloadType
import ca.stevenskelton.tinyakkaslackcue.blocks.{ActionId, CallbackId, PrivateMetadata, State}
import play.api.libs.json.JsObject

object SlackPayload {

  final case class SlackPayloadType(value: String) extends AnyVal
  val BlockActions = SlackPayloadType("block_actions")
  val ViewSubmission = SlackPayloadType("view_submission")

  def apply(jsObject: JsObject): SlackPayload = {
    val payloadType = (jsObject \ "type").as[String] match {
      case BlockActions.value => BlockActions
      case ViewSubmission.value => ViewSubmission
      case x => SlackPayloadType(x)
    }
    val user = (jsObject \ "user").as[SlackUser]
    val slackActions: Seq[SlackAction] = (jsObject \ "actions").asOpt[Seq[SlackAction]].getOrElse(Nil)
    val view = jsObject \ "view"
    val id = (view \ "id").as[String]
    val triggerId = SlackTriggerId((jsObject \ "trigger_id").as[String])
    val privateMetadata = (view \ "private_metadata").asOpt[String].map(PrivateMetadata(_))
    val callbackId = (view \ "callback_id").asOpt[String].map(CallbackId(_))
    val actionStates = State.parseActionStates(view \ "state" \ "values")
    SlackPayload(payloadType, id, user, slackActions, triggerId, privateMetadata, callbackId, actionStates)
  }
}
case class SlackPayload(payloadType: SlackPayloadType, viewId: String, user: SlackUser, actions: Seq[SlackAction], triggerId: SlackTriggerId,
                        privateMetadata: Option[PrivateMetadata], callbackId: Option[CallbackId], actionStates: Map[ActionId, State])
