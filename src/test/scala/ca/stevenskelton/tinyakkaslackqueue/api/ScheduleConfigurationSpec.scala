package ca.stevenskelton.tinyakkaslackqueue.api

import ca.stevenskelton.tinyakkaslackqueue.util.DateUtils
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.*

class ScheduleConfigurationSpec extends AnyWordSpec
  with Matchers
  with ScalaFutures {

  "parse instances" when {
    "dayOfWeek" should {
      "parse" in {
        val config = ConfigFactory.parseString(
          """schedule = [
            {
              day = Monday
              time = "20:00"
            },{
              day = tuesday
              time = "9:30"
            }
          ]"""
        )

        val instances = ScheduleConfiguration(config)
        instances.size shouldEqual 2
        instances(0).day shouldEqual Right(DayOfWeek.MONDAY)
        instances(0).time shouldEqual LocalTime.of(20, 0)

        instances(1).day shouldEqual Right(DayOfWeek.TUESDAY)
        instances(1).time shouldEqual LocalTime.of(9, 30)
      }
      "multiple parse" in {
        val config = ConfigFactory.parseString(
          """schedule = [
            {
              days = [
                Monday,
                Tuesday,
                Friday
              ],
              time = "20:00"
            }
          ]"""
        )

        val instances = ScheduleConfiguration(config)
        instances.size shouldEqual 3
        instances(0).day shouldEqual Right(DayOfWeek.MONDAY)
        instances(0).time shouldEqual LocalTime.of(20, 0)

        instances(1).day shouldEqual Right(DayOfWeek.TUESDAY)
        instances(1).time shouldEqual LocalTime.of(20, 0)

        instances(2).day shouldEqual Right(DayOfWeek.FRIDAY)
        instances(2).time shouldEqual LocalTime.of(20, 0)
      }
    }
    "no day" should {
      "parse" in {
        val config = ConfigFactory.parseString(
          """schedule = [
            {
              time = "20:00"
            }
          ]"""
        )

        val instances = ScheduleConfiguration(config)
        instances.size shouldEqual 7
        instances(0).day shouldEqual Right(DayOfWeek.MONDAY)
        instances(0).time shouldEqual LocalTime.of(20, 0)

        instances(1).day shouldEqual Right(DayOfWeek.TUESDAY)
        instances(1).time shouldEqual LocalTime.of(20, 0)
      }
    }
  }

  "nextTime" when {
    val instanceTime = LocalTime.of(17, 0)
    val now = ZonedDateTime.of(LocalDateTime.of(LocalDate.of(2023, 2, 18), LocalTime.of(3, 0)), DateUtils.NewYorkZoneId)

    "dayOfWeek" should {
      "handle before" in {
        val nextTime = ScheduleConfiguration(Right(DayOfWeek.MONDAY), instanceTime).nextTime(now)
        nextTime.toLocalDateTime shouldEqual LocalDateTime.of(LocalDate.of(2023, 2, 20), instanceTime)
      }
      "handle after" in {
        val nextTime = ScheduleConfiguration(Right(DayOfWeek.SUNDAY), instanceTime).nextTime(now)
        nextTime.toLocalDateTime shouldEqual LocalDateTime.of(LocalDate.of(2023, 2, 19), instanceTime)
      }
    }
    "dayOfMonth" should {
      "handle before" in {
        val nextTime = ScheduleConfiguration(Left(5), instanceTime).nextTime(now)
        nextTime.toLocalDateTime shouldEqual LocalDateTime.of(LocalDate.of(2023, 3, 5), instanceTime)
      }
      "handle after" in {
        val nextTime = ScheduleConfiguration(Left(25), instanceTime).nextTime(now)
        nextTime.toLocalDateTime shouldEqual LocalDateTime.of(LocalDate.of(2023, 2, 25), instanceTime)
      }
      "handle values > 31" in {
        val nextTime = ScheduleConfiguration(Left(30), instanceTime).nextTime(now)
        nextTime.toLocalDateTime shouldEqual LocalDateTime.of(LocalDate.of(2023, 2, 28), instanceTime)
      }
    }
  }
}
