package ca.stevenskelton.tinyakkaslackqueue.util

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.{ZoneId, ZonedDateTime}

class DateUtilsSpec extends AnyWordSpec
  with Matchers
  with ScalaFutures {

  //  val usersInfoJson = FileUtils.readJson(new File("../tinyakkaslackqueue/src/test/resources/slack/users-info.json")).get.as[JsObject]

  "parse user zone" in {
    //    val field = (usersInfoJson \ "user" \ "tz").as[String]
    val zoneId = ZoneId.of("America/Los_Angeles")
    val zoneDefault = ZoneId.of("Canada/Eastern")
    val date = ZonedDateTime.now(zoneDefault)
    val diff = date.withZoneSameInstant(zoneId)
    diff.toLocalDateTime.plusHours(3).toString shouldBe date.toLocalDateTime.toString
  }

}
