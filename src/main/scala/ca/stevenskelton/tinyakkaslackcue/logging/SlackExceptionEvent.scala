package ca.stevenskelton.tinyakkaslackcue.logging

import org.slf4j.event.{Level, LoggingEvent}
import org.slf4j.{Logger, Marker}

import scala.util.control.NoStackTrace

object SlackExceptionEvent {
  object UserCancelledException extends Exception with NoStackTrace

  def apply(throwable: Throwable)(implicit logger: Logger): SlackExceptionEvent = new SlackExceptionEvent(logger.getName, throwable)
}

class SlackExceptionEvent private(loggerName: String, throwable: Throwable) extends LoggingEvent {
  override def getLevel: Level = Level.ERROR

  override def getMarker: Marker = null

  override def getLoggerName: String = loggerName

  override def getMessage: String = throwable.getMessage

  override def getThreadName: String = Thread.currentThread.getName

  override def getArgumentArray: Array[Object] = null

  override def getTimeStamp: Long = System.currentTimeMillis

  override def getThrowable: Throwable = throwable
}