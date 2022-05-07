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

  val viewSubmission = FileUtils.readJson(new File("src/test/resources/actions/view_submission.json")).get.as[JsObject]

  "parse" should {
    "read fields" in {
      val (privateMetadata, actionStates) = ScheduleActionModal.parseViewSubmission(viewSubmission)
      actionStates should have size 5
      actionStates(ActionIdScheduleDate).asInstanceOf[DatePickerState].value shouldBe LocalDate.of(2022, 5, 1)
      actionStates(ActionIdScheduleTime).asInstanceOf[TimePickerState].value shouldBe LocalTime.of(11, 38)
      actionStates(ActionIdNotifyOnComplete).asInstanceOf[MultiUsersState].users shouldBe Seq(SlackUserId("U039TCGLX5G"))
      actionStates(ActionIdNotifyOnFailure).asInstanceOf[MultiUsersState].users shouldBe Seq(SlackUserId("U039TCGLX5G"))
      actionStates(ActionIdLogLevel).asInstanceOf[SelectState].value shouldBe "ERROR"
    }
  }

}
