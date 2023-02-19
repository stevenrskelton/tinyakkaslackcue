package ca.stevenskelton.tinyakkaslackqueue.api

import com.typesafe.config.Config

import java.time.{DayOfWeek, Duration, LocalDateTime, LocalTime}
import scala.jdk.CollectionConverters.ListHasAsScala

object ScheduleConfiguration {

  def stringify(instances: Seq[ScheduleConfiguration]): String = {
    if (instances.forall(_.day.isRight)) {
      instances.groupBy(_.day.map(_.toString).getOrElse("")).map {
        case (dayOfWeek, seq) =>
          s"$dayOfWeek:${seq.map(_.time.toString).mkString(",")}"
      }.mkString(";")
    } else {
      instances.map(o => s"day:${o.day},time:${o.time}").mkString(",")
    }
  }

  def apply(config: Config): Seq[ScheduleConfiguration] = {
    config.getConfigList("schedule").asScala.flatMap {
      instanceConfig =>
        val days = if (instanceConfig.hasPath("day")) {
          val dayString = instanceConfig.getString("day")
          Seq {
            dayString.toIntOption.map(Left(_)).getOrElse {
              Right {
                dayString.toLowerCase match {
                  case "mon" | "monday" => DayOfWeek.MONDAY
                  case "tue" | "tuesday" => DayOfWeek.TUESDAY
                  case "wed" | "wednesday" => DayOfWeek.WEDNESDAY
                  case "thu" | "thursday" => DayOfWeek.THURSDAY
                  case "fri" | "friday" => DayOfWeek.FRIDAY
                  case "sat" | "saturday" => DayOfWeek.SATURDAY
                  case "sun" | "sunday" => DayOfWeek.SUNDAY
                }
              }
            }
          }
        } else {
          DayOfWeek.values().toSeq.map(Right(_))
        }
        val timeString = instanceConfig.getString("time")
        val hourMinute = timeString.split(':')
        val time = LocalTime.of(hourMinute(0).toInt, hourMinute(1).toInt)
        days.map(ScheduleConfiguration(_, time))
    }.toSeq
  }

  def next(now: LocalDateTime, instances: Seq[ScheduleConfiguration]): LocalDateTime = {
    require(instances.nonEmpty)

    instances.map(_.nextTime(now)).minBy(Duration.between(now, _).toMinutes)
  }
}

case class ScheduleConfiguration(day: Either[Int, DayOfWeek], time: LocalTime) {

  def nextTime(now: LocalDateTime): LocalDateTime = {
    var next = now
    if (next.toLocalTime.isAfter(time)) {
      next = next.toLocalDate.plusDays(1).atTime(time)
    } else {
      next = next.toLocalDate.atTime(time)
    }

    day match {
      case Left(int) =>
        if (int < next.getDayOfMonth) {
          next = next.minusDays(next.getDayOfMonth - 1).plusMonths(1)
        }
        val currentMonth = next.getMonth
        while (next.getDayOfMonth != int) {
          val potential = next.plusDays(1)
          if (potential.getMonth != currentMonth) return next
          else {
            next = potential
          }
        }
      case Right(dayOfWeek) =>
        while (next.getDayOfWeek != dayOfWeek) {
          next = next.plusDays(1)
        }
    }
    next
  }

}
