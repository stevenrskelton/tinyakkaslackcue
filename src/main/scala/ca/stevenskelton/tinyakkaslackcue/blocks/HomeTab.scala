package ca.stevenskelton.tinyakkaslackcue.blocks

import akka.Done
import ca.stevenskelton.tinyakkaslackcue.util.DateUtils
import ca.stevenskelton.tinyakkaslackcue._
import org.slf4j.Logger
import play.api.libs.json.{JsObject, JsValue, Json}

import java.time.{Duration, ZonedDateTime}
import scala.collection.SortedSet
import scala.concurrent.Future
import scala.util.Try

object HomeTab {

  object State extends Enumeration {
    type State = Value

    val Scheduled, Success, Failure = Value
  }

  private implicit val orderingFields = new Ordering[Fields] {
    override def compare(x: Fields, y: Fields): Int = x.name.compareTo(y.name)
  }

  private implicit val orderingFieldsInstance = new Ordering[FieldsInstance] {
    override def compare(x: FieldsInstance, y: FieldsInstance): Int = x.date.compareTo(y.date)
  }

  case class FieldsInstance(slackTs: SlackTs, date: ZonedDateTime, duration: Duration, createdBy: SlackUserId, state: State.State) {

    def toBlocks: SlackBlocksAsString = {
      val header = state match {
        case State.Success => "Last Success"
        case State.Failure => "Last Failure"
        case State.Scheduled => ???
      }
      SlackBlocksAsString {
        s"""
{
  "type": "section",
  "text": {
    "type": "mrkdwn",
    "text": "*$header*\\nCreated By:${createdBy.value}\\nDuration:${DateUtils.humanReadable(duration)}\\n${DateUtils.humanReadable(date)}"
  },
  "accessory": {
    "type": "button",
    "text": {
      "type": "plain_text",
      "text": "View Details",
      "emoji": true
    },
    "value": "click_me_123",
    "url": "https://tradeaudit.slack.com/archives/C03BC1HBVQD/p1651114822709949",
    "action_id": "button-action"
  }
}"""
      }
    }
  }

  case class Fields(
                     name: String,
                     description: Mrkdwn,
                     executed: SortedSet[FieldsInstance],
                     pending: SortedSet[FieldsInstance]
                   ) {

    val nextTs: Option[SlackTs] = pending.headOption.map(_.slackTs)

    private val HeaderPreamble = "Scheduled Task "
    private val CreatedByPreamble = "*Created by* "
    private val ScheduledForPreamble = "*Scheduled for* "

    def toBlocks: SlackBlocksAsString = {
      SlackBlocksAsString {
        s"""
{
  "type": "header",
  "text": {
    "type": "plain_text",
    "text": "$name",
    "emoji": true
  }
},
{
  "type": "section",
  "text": {
    "type": "mrkdwn",
    "text": "$description"
  }
},
{
  "type": "section",
  "fields": [
    {
      "type": "mrkdwn",
      "text": "*Type:*\nComputer (laptop)"
    },
    {
      "type": "mrkdwn",
      "text": "*When:*\nSubmitted Aut 10"
    },
    ${pending.map(_.toBlocks.value).mkString(""",{"type": "divider"},""")}
    ${executed.toSeq.reverse.map(_.toBlocks.value).mkString(""",{"type": "divider"},""")}
  ]
},
{
  "type": "actions",
  "elements": [
    {
      "type": "button",
      "text": {
        "type": "plain_text",
        "emoji": true,
        "text": "Run"
      },
      "style": "primary",
      "value": "run_click_me_123_$name"
    },
    {
      "type": "button",
      "text": {
        "type": "plain_text",
        "emoji": true,
        "text": "Schedule"
      },
      "value": "schedule_click_me_123_$name"
    },
    {
      "type": "button",
      "text": {
        "type": "plain_text",
        "emoji": true,
        "text": "Cancel"
      },
      "style": "danger",
      "value": "cancel_click_me_123_$name"
    }
  ]
}"""
      }
    }

  }

  def parseView(blocks: Seq[JsValue]): Try[Fields] = Try {
    ???
  }

  def initialize(slackClient: SlackClient, slackTaskFactories: SlackTaskFactories): Seq[Fields] = {
    val allPinned = slackClient.pinsList()
    slackTaskFactories.factories.map {
      slackTask =>
        //        val pinned = allPinned.find(_.)
        Fields(
          name = slackTask.name,
          description = slackTask.description,
          executed = SortedSet.empty,
          pending = SortedSet.empty
        )
    }
  }

  def handleSubmission(slackTriggerId: SlackTriggerId, slackUser: SlackUser, jsObject: JsObject)(implicit slackClient: SlackClient, slackTaskFactories: SlackTaskFactories, logger: Logger): Future[Done] = {
    logger.info(Json.stringify(jsObject))
    val state = ScheduleActionModal.parseViewSubmission(jsObject)
    Future.successful(Done)
  }

  def handleAction(slackTriggerId: SlackTriggerId, slackUser: SlackUser, jsObject: JsObject)(implicit slackClient: SlackClient, slackTaskFactories: SlackTaskFactories, logger: Logger): Future[Done] = {
    logger.info(Json.stringify(jsObject))

    val slackActions: Seq[SlackAction] = (jsObject \ "actions").as[Seq[SlackAction]]

    //    slackActions.headOption.map {
    //      slackAction =>
    //        Slack.getInstance.methods.viewsOpen((r: ViewsOpenRequest.ViewsOpenRequestBuilder) => r.token())
    //    }
    val view = ScheduleActionModal.modal("somenamehere", ZonedDateTime.now(), PrivateMetadata(""))
    val result = slackClient.viewsOpen(slackTriggerId, view)
    if(!result.isOk){
      logger.error(result.getError)
    }
    Future.successful(Done)
  }
  //  def toAddInstanceToBlocks(existing: Seq[Fields], slackTask: SlackTask, instance: FieldsInstance): Seq[Fields] = {
  //    val updatedFields = if (existing.exists(_.name == slackTask)) {
  //      existing.map {
  //        fields =>
  //          if (fields.name == slackTask.name) {
  //            val (lastSuccess, lastFailure) = instance.state match {
  //              case State.Scheduled => (fields.lastSuccess, fields.lastFailure)
  //              case State.Success => (Some(instance), fields.lastFailure)
  //              case State.Failure => (fields.lastSuccess, Some(instance))
  //            }
  //            val nextTs = if (instance.state == State.Scheduled) {
  //              Some(fields.nextTs.map { s =>
  //                SlackTs(Seq(s.value, slackTask.ts.value).min)
  //              }.getOrElse {
  //                slackTask.ts
  //              })
  //            } else {
  //              //TODO: read from queue
  //              fields.nextTs.filterNot(_ == slackTask.ts)
  //            }
  //            Fields(
  //              name = slackTask.name,
  //              description = slackTask.description,
  //              nextTs = nextTs,
  //              lastSuccess = lastSuccess,
  //              lastFailure = lastFailure
  //            )
  //          } else {
  //            fields
  //          }
  //      }
  //    } else {
  //      val (lastSuccess, lastFailure) = instance.state match {
  //        case State.Scheduled => (None, None)
  //        case State.Success => (Some(instance), None)
  //        case State.Failure => (None, Some(instance))
  //      }
  //      existing :+ Fields(
  //        name = slackTask.name,
  //        description = slackTask.description,
  //        nextTs = None,
  //        lastSuccess = lastSuccess,
  //        lastFailure = lastFailure
  //      )
  //    }
  //    updatedFields.sorted
  //  }
}
