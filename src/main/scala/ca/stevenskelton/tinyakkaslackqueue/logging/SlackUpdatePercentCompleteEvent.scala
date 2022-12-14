package ca.stevenskelton.tinyakkaslackqueue.logging

import org.slf4j.event.{KeyValuePair, Level, LoggingEvent}
import org.slf4j.{Logger, Marker}

import java.util

object SlackUpdatePercentCompleteEvent {
  def apply(percent: Float)(implicit logger: Logger): SlackUpdatePercentCompleteEvent =
    new SlackUpdatePercentCompleteEvent(logger.getName, percent)
}

class SlackUpdatePercentCompleteEvent private(loggerName: String, val percent: Float) extends LoggingEvent {
  override def getLevel: Level = Level.TRACE

  override def getMarkers: util.List[Marker] = null

  override def getLoggerName: String = loggerName

  override def getMessage: String = ""

  override def getThreadName: String = Thread.currentThread.getName

  override def getArgumentArray: Array[Object] = null

  override def getTimeStamp: Long = System.currentTimeMillis

  override def getThrowable: Throwable = null

  override def getArguments: util.List[AnyRef] = null

  override def getKeyValuePairs: util.List[KeyValuePair] = null
}
