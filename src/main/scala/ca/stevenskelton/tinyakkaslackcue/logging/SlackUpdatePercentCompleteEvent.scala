package ca.stevenskelton.tinyakkaslackcue.logging

import org.slf4j.event.{Level, LoggingEvent}
import org.slf4j.{Logger, Marker}

object SlackUpdatePercentCompleteEvent {
  def apply(percent: Float)(implicit logger: Logger): SlackUpdatePercentCompleteEvent =
    new SlackUpdatePercentCompleteEvent(logger.getName, percent)
}

class SlackUpdatePercentCompleteEvent private(loggerName: String, val percent: Float) extends LoggingEvent {
  override def getLevel: Level = Level.TRACE

  override def getMarker: Marker = null

  override def getLoggerName: String = loggerName

  override def getMessage: String = ""

  override def getThreadName: String = Thread.currentThread.getName

  override def getArgumentArray: Array[Object] = null

  override def getTimeStamp: Long = System.currentTimeMillis

  override def getThrowable: Throwable = null
}
