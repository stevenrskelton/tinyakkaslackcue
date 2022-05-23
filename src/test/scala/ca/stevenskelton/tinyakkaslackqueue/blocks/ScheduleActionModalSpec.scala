package ca.stevenskelton.tinyakkaslackqueue.blocks

import ca.stevenskelton.tinyakkaslackqueue.views.CreateTaskModal
import ca.stevenskelton.tinyakkaslackqueue.util.FileUtils
import ca.stevenskelton.tinyakkaslackqueue.{SlackUserId, TestData}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsDefined, JsObject, JsString, Json}

import java.io.File
import java.time.{LocalDate, LocalTime}

class ScheduleActionModalSpec extends AnyWordSpec
  with Matchers
  with ScalaFutures {

  "viewModal" should {
    "be json" in {
      val slackTask = TestData.slackTaskFactories.factories.head.create(
        ts = TestData.slackTs,
        createdBy = TestData.CreatedBy,
        notifyOnError = Nil,
        notifyOnComplete = Nil
      )
      val scheduledTask = TestData.toScheduledTask(slackTask)
      val modal = CreateTaskModal.viewModal(scheduledTask)
      val json = Json.parse(modal.value)
      json \ "type" shouldBe JsDefined(JsString("modal"))
    }
  }

  "queue" should {

    val viewSubmission = FileUtils.readJson(new File("../tinyakkaslackqueue/src/test/resources/actions/view_submission_queue.json")).get.as[JsObject]

    "read fields" in {
      val (privateMetadata, actionStates, callbackId) = CreateTaskModal.parseViewSubmission(viewSubmission)
      privateMetadata.value shouldBe "Exchange Listings"

      actionStates should have size 3
      actionStates(ActionId.NotifyOnComplete).asInstanceOf[MultiUsersState].users shouldBe Seq(SlackUserId("U039T9DUHGT"))
      actionStates(ActionId.NotifyOnFailure).asInstanceOf[MultiUsersState].users shouldBe Seq(SlackUserId("U039T9DUHGT"))
      actionStates(ActionId.LogLevel).asInstanceOf[SelectState].value shouldBe "ERROR"
    }
  }

  "schedule" should {

    val viewSubmission = FileUtils.readJson(new File("../tinyakkaslackqueue/src/test/resources/actions/view_submission_schedule.json")).get.as[JsObject]

    "read fields" in {
      val (privateMetadata, actionStates, callbackId) = CreateTaskModal.parseViewSubmission(viewSubmission)
      privateMetadata.value shouldBe "Yahoo 2min Prices"

      actionStates should have size 5
      actionStates(ActionId.ScheduleDate).asInstanceOf[DatePickerState].value shouldBe LocalDate.of(2022, 5, 18)
      actionStates(ActionId.ScheduleTime).asInstanceOf[TimePickerState].value shouldBe LocalTime.of(11, 31)
      actionStates(ActionId.NotifyOnComplete).asInstanceOf[MultiUsersState].users shouldBe Seq(SlackUserId("U039T9DUHGT"))
      actionStates(ActionId.NotifyOnFailure).asInstanceOf[MultiUsersState].users shouldBe Seq(SlackUserId("U039T9DUHGT"), SlackUserId("U039TCGLX5G"))
      actionStates(ActionId.LogLevel).asInstanceOf[SelectState].value shouldBe "WARN"
    }
  }

}
