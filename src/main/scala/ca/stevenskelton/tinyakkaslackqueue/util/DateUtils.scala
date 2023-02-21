package ca.stevenskelton.tinyakkaslackqueue.util

import java.time._
import java.time.format.DateTimeFormatter

object DateUtils {

  val NewYorkZoneId: ZoneId = ZoneId.of("America/New_York")

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

  private val formatter = DateTimeFormatter.ofPattern("EEE, MMM d, h:mma")

  def humanReadable(zonedDateTime: ZonedDateTime): String = zonedDateTime.format(formatter)

  def humanReadable(localDateTime: LocalDateTime, zoneId: ZoneId): String = {
    ZonedDateTime.of(localDateTime, zoneId).format(formatter)
  }

}

