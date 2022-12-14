package ca.stevenskelton.tinyakkaslackqueue.logging

import org.slf4j.event.{KeyValuePair, Level, LoggingEvent}
import org.slf4j.{Logger, Marker}

import java.util
import scala.util.control.NoStackTrace

object SlackExceptionEvent {
  object UserCancelledException extends Exception with NoStackTrace

  def apply(throwable: Throwable)(implicit logger: Logger): SlackExceptionEvent = new SlackExceptionEvent(logger.getName, throwable)
}

class SlackExceptionEvent private(loggerName: String, throwable: Throwable) extends LoggingEvent {
  override def getLevel: Level = Level.ERROR

  override def getMarkers: util.List[Marker] = null

  override def getLoggerName: String = loggerName

  override def getMessage: String = throwable.getMessage

  override def getThreadName: String = Thread.currentThread.getName

  override def getArgumentArray: Array[Object] = null

  override def getTimeStamp: Long = System.currentTimeMillis

  override def getThrowable: Throwable = throwable

  override def getArguments: util.List[AnyRef] = null

  override def getKeyValuePairs: util.List[KeyValuePair] = null
}