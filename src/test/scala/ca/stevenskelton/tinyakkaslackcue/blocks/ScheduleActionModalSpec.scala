package ca.stevenskelton.tinyakkaslackcue.blocks

import ca.stevenskelton.tinyakkaslackcue.SlackUserId
import ca.stevenskelton.tinyakkaslackcue.blocks.ScheduleActionModal._
import ca.stevenskelton.tinyakkaslackcue.util.FileUtils
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.JsObject

import java.io.File
import java.time.{LocalDate, LocalTime}

class ScheduleActionModalSpec extends AnyWordSpec
  with Matchers
  with ScalaFutures {

  "queue" should {

    val viewSubmission = FileUtils.readJson(new File("../tinyakkaslackcue/src/test/resources/actions/view_submission_queue.json")).get.as[JsObject]

    "read fields" in {
      val (privateMetadata, actionStates, callbackId) = ScheduleActionModal.parseViewSubmission(viewSubmission)
      privateMetadata.value shouldBe "Exchange Listings"

      actionStates should have size 3
      actionStates(ActionIdNotifyOnComplete).asInstanceOf[MultiUsersState].users shouldBe Seq(SlackUserId("U039T9DUHGT"))
      actionStates(ActionIdNotifyOnFailure).asInstanceOf[MultiUsersState].users shouldBe Seq(SlackUserId("U039T9DUHGT"))
      actionStates(ActionIdLogLevel).asInstanceOf[SelectState].value shouldBe "ERROR"
    }
  }

  "schedule" should {

    val viewSubmission = FileUtils.readJson(new File("../tinyakkaslackcue/src/test/resources/actions/view_submission_schedule.json")).get.as[JsObject]

    "read fields" in {
      val (privateMetadata, actionStates, callbackId) = ScheduleActionModal.parseViewSubmission(viewSubmission)
      privateMetadata.value shouldBe "Yahoo 2min Prices"

      actionStates should have size 5
      actionStates(ActionIdScheduleDate).asInstanceOf[DatePickerState].value shouldBe LocalDate.of(2022, 5, 18)
      actionStates(ActionIdScheduleTime).asInstanceOf[TimePickerState].value shouldBe LocalTime.of(11, 31)
      actionStates(ActionIdNotifyOnComplete).asInstanceOf[MultiUsersState].users shouldBe Seq(SlackUserId("U039T9DUHGT"))
      actionStates(ActionIdNotifyOnFailure).asInstanceOf[MultiUsersState].users shouldBe Seq(SlackUserId("U039T9DUHGT"), SlackUserId("U039TCGLX5G"))
      actionStates(ActionIdLogLevel).asInstanceOf[SelectState].value shouldBe "WARN"
    }
  }

}
