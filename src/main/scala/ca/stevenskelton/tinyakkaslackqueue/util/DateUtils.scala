package ca.stevenskelton.tinyakkaslackqueue.util

import java.time._
import java.time.format.DateTimeFormatter

object DateUtils {

  val NewYorkZoneId: ZoneId = ZoneId.of("America/New_York")
  val Time2minPreOpen: LocalTime = LocalTime.of(4, 0)
  val Time2minRegularOpen: LocalTime = LocalTime.of(9, 30)
  val Time2minPostOpen: LocalTime = LocalTime.of(16, 0)
  val Time2minPostEnd: LocalTime = LocalTime.of(20, 0)

  //  val FirstDataDate: EpochDay = EpochDay(LocalDate.of(2016, Month.APRIL, 20));
  val EpochLocalDateTime: LocalDateTime = DateUtils.parseLocalDateTime(0);

  val yyyyMMddFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  def parseLocalDateTime(epochSeconds: Long): LocalDateTime = {
    val zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), NewYorkZoneId)
    zonedDateTime.toLocalDateTime
  }

  def dateToLocalDateTime(createdAt: java.util.Date): LocalDateTime = {
    parseLocalDateTime(createdAt.getTime / 1000)
  }

  def toTimestamp(localDateTime: LocalDateTime): Long = localDateTime.toEpochSecond(ZoneOffset.UTC)

  def humanReadable(duration: Duration): String = {
    val seconds = duration.toSeconds
    if (seconds >= 2592000) {
      s"${(seconds / 2592000).toInt} months"
    } else if (seconds > 86400) {
      s"${(seconds / 86400).toInt} days"
    } else if (seconds > 3600) {
      s"${(seconds / 3600).toInt} hours"
    } else if (seconds > 60) {
      s"${(seconds / 60).toInt} minutes"
    } else {
      s"$seconds seconds"
    }
  }

  def humanReadable(duration: scala.concurrent.duration.Duration): String = humanReadable(Duration.ofMillis(duration.toMillis))

  def humanReadable(zonedDateTime: ZonedDateTime): String = {
    zonedDateTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)
  }

}

