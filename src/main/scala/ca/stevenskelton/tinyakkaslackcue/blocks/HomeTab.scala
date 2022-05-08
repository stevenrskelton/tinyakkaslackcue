package ca.stevenskelton.tinyakkaslackcue.blocks

import akka.Done
import ca.stevenskelton.tinyakkaslackcue._
import ca.stevenskelton.tinyakkaslackcue.util.DateUtils
import com.slack.api.methods.SlackApiTextResponse
import org.slf4j.Logger
import play.api.libs.json.{JsObject, JsValue, Json}

import java.time.{Duration, ZonedDateTime}
import scala.collection.SortedSet
import scala.concurrent.Future
import scala.util.Try

object HomeTab {

  val ActionIdTaskQueue = ActionId("task-queue-action")
  val ActionIdTaskSchedule = ActionId("schedule-task-action")
  val ActionIdTaskCancel = ActionId("multi_users_select-action1")

  object State extends Enumeration {
    type State = Value

    val Running, Scheduled, Success, Failure = Value
  }

  private implicit val orderingFields = new Ordering[Fields] {
    override def compare(x: Fields, y: Fields): Int = x.slackTaskIdentifier.name.compareTo(y.slackTaskIdentifier.name)
  }

  private implicit val orderingFieldsInstance = new Ordering[FieldsInstance] {
    override def compare(x: FieldsInstance, y: FieldsInstance): Int = x.date.compareTo(y.date)
  }

  case class FieldsInstance(slackTs: SlackTs, date: ZonedDateTime, duration: Duration, createdBy: SlackUserId, state: State.State) {

    def toBlocks: SlackBlocksAsString = {
      val blocksAsString = state match {
        case State.Success => s"""
{
  "type": "section",
  "text": {
    "type": "mrkdwn",
    "text": ":white_check_mark: *Last Success:* ${DateUtils.humanReadable(date)}"
  },
  "accessory": {
    "type": "button",
    "text": {
      "type": "plain_text",
      "text": "View Logs",
      "emoji": true
    },
    "value": "click_me_123",
    "action_id": "button-action"
  }
}"""
        case State.Failure => s"""
{
  "type": "section",
  "text": {
    "type": "mrkdwn",
    "text": ":no_entry_sign: *Last Failure:* ${DateUtils.humanReadable(date)}"
  },
  "accessory": {
    "type": "button",
    "text": {
      "type": "plain_text",
      "text": "View Logs",
      "emoji": true
    },
    "value": "click_me_123",
    "action_id": "button-action"
  }
}"""
        case State.Scheduled => s"""
{
  "type": "section",
  "text": {
    "type": "mrkdwn",
    "text": ":watch: *Scheduled:* 2022-05-01 4:51pm"
  },
  "accessory": {
    "type": "button",
    "text": {
      "type": "plain_text",
      "text": "View Details",
      "emoji": true
    },
    "value": "click_me_123",
    "action_id": "button-action"
  }
}"""
        case State.Running =>
          s"""
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
    {
      "type": "mrkdwn",
      "text": "*Last Update:*\nMar 10, 2015 (3 years, 5 months)"
    },
    {
      "type": "mrkdwn",
      "text": "*Reason:*\nAll vowel keys aren't working."
    },
    {
      "type": "mrkdwn",
      "text": "*Specs:*\n\"Cheetah Pro 15\" - Fast, really fast\""
    }
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
        "text": "View Logs"
      },
      "value": "click_me_123"
    },
    {
      "type": "button",
      "text": {
        "type": "plain_text",
        "emoji": true,
        "text": "Cancel"
      },
      "style": "danger",
      "value": "click_me_123"
    }
  ]
}"""
      }
      SlackBlocksAsString(blocksAsString)
    }
  }

  case class Fields(
                     slackTaskIdentifier: SlackTaskIdentifier,
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
    "text": "${slackTaskIdentifier.name}",
    "emoji": true
  }
},
{
  "type": "section",
  "text": {
    "type": "mrkdwn",
    "text": "${slackTaskIdentifier.description}"
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
        "text": "Queue"
      },
      "style": "primary",
      "action_id": "${ActionIdTaskQueue.value}",
      "value": "${slackTaskIdentifier.getClass.getName}"
    },
    {
      "type": "button",
      "text": {
        "type": "plain_text",
        "emoji": true,
        "text": "Schedule"
      },
      "action_id": "${ActionIdTaskSchedule.value}",
      "value": "${slackTaskIdentifier.getClass.getName}"
    },
    {
      "type": "button",
      "text": {
        "type": "plain_text",
        "emoji": true,
        "text": "Cancel"
      },
      "style": "danger",
      "action_id": "${ActionIdTaskCancel.value}",
      "value": "${slackTaskIdentifier.getClass.getName}"
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
          slackTaskIdentifier = slackTask,
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
    val action = slackActions.head
    val result: SlackApiTextResponse = action.actionId match {
      case ActionIdTaskQueue =>
        val view = ScheduleActionModal.modal(slackUser, action.value, None, PrivateMetadata(action.value))
        slackClient.viewsOpen(slackTriggerId, view)
      case ActionIdTaskSchedule =>
        val view = ScheduleActionModal.modal(slackUser,action.value, Some(ZonedDateTime.now()), PrivateMetadata(action.value))
        slackClient.viewsOpen(slackTriggerId, view)
      case ActionIdTaskCancel =>
        val view = ScheduleActionModal.modal(slackUser,action.value, None, PrivateMetadata(action.value))
        slackClient.viewsOpen(slackTriggerId, view)
    }
    //    slackActions.headOption.map {
    //      slackAction =>
    //        Slack.getInstance.methods.viewsOpen((r: ViewsOpenRequest.ViewsOpenRequestBuilder) => r.token())
    //    }

    if (!result.isOk) {
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
