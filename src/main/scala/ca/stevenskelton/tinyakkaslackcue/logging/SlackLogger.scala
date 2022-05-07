package ca.stevenskelton.tinyakkaslackcue.logging

import akka.stream.scaladsl.SourceQueueWithComplete
import org.slf4j.event.{Level, LoggingEvent}
import org.slf4j.helpers.MessageFormatter
import org.slf4j.{Logger, Marker}

class SlackLogger(override val getName: String, source: SourceQueueWithComplete[LoggingEvent], base: Logger) extends Logger {

  override def isTraceEnabled: Boolean = base.isTraceEnabled

  override def trace(msg: String): Unit = {
    recordEvent_0Args(Level.TRACE, null, msg, null)
    base.trace(msg)
  }

  override def trace(format: String, arg: Any): Unit = {
    recordEvent_1Args(Level.TRACE, null, format, arg)
    base.trace(format, arg)
  }

  override def trace(format: String, arg1: Any, arg2: Any): Unit = {
    recordEvent2Args(Level.TRACE, null, format, arg1, arg2)
    base.trace(format, arg1, arg2)
  }

  override def trace(format: String, arguments: Object*): Unit = {
    recordEventArgArray(Level.TRACE, null, format, arguments)
    base.trace(format, arguments: _*)
  }

  override def trace(msg: String, t: Throwable): Unit = {
    recordEvent_0Args(Level.TRACE, null, msg, t)
    base.trace(msg, t)
  }

  override def isTraceEnabled(marker: Marker): Boolean = base.isTraceEnabled(marker)

  override def trace(marker: Marker, msg: String): Unit = {
    recordEvent_0Args(Level.TRACE, marker, msg, null)
    base.trace(marker, msg)
  }

  override def trace(marker: Marker, format: String, arg: Any): Unit = {
    recordEvent_1Args(Level.TRACE, marker, format, arg)
    base.trace(marker, format, arg)
  }

  override def trace(marker: Marker, format: String, arg1: Any, arg2: Any): Unit = {
    recordEvent2Args(Level.TRACE, marker, format, arg1, arg2)
    base.trace(marker, format, arg1, arg2)
  }

  override def trace(marker: Marker, format: String, argArray: Object*): Unit = {
    recordEventArgArray(Level.TRACE, marker, format, argArray)
    base.trace(marker, format, argArray: _*)
  }

  override def trace(marker: Marker, msg: String, t: Throwable): Unit = {
    recordEvent_0Args(Level.TRACE, marker, msg, t)
    base.trace(marker, msg, t)
  }

  override def isDebugEnabled: Boolean = base.isDebugEnabled

  override def debug(msg: String): Unit = {
    recordEvent_0Args(Level.DEBUG, null, msg, null)
    base.debug(msg)
  }

  override def debug(format: String, arg: Any): Unit = {
    recordEvent_1Args(Level.DEBUG, null, format, arg)
    base.debug(format, arg)
  }

  override def debug(format: String, arg1: Any, arg2: Any): Unit = {
    recordEvent2Args(Level.DEBUG, null, format, arg1, arg2)
    base.debug(format, arg1, arg2)
  }

  override def debug(format: String, arguments: Object*): Unit = {
    recordEventArgArray(Level.DEBUG, null, format, arguments)
    base.debug(format, arguments)
  }

  override def debug(msg: String, t: Throwable): Unit = {
    recordEvent_0Args(Level.DEBUG, null, msg, t)
    base.debug(msg, t)
  }

  override def isDebugEnabled(marker: Marker): Boolean = base.isDebugEnabled(marker)

  override def debug(marker: Marker, msg: String): Unit = {
    recordEvent_0Args(Level.DEBUG, marker, msg, null)
    base.debug(marker, msg)
  }

  override def debug(marker: Marker, format: String, arg: Any): Unit = {
    recordEvent_1Args(Level.DEBUG, marker, format, arg)
    base.debug(marker, format, arg)
  }

  override def debug(marker: Marker, format: String, arg1: Any, arg2: Any): Unit = {
    recordEvent2Args(Level.DEBUG, marker, format, arg1, arg2)
    base.debug(marker, format, arg1, arg2)
  }

  override def debug(marker: Marker, format: String, arguments: Object*): Unit = {
    recordEventArgArray(Level.DEBUG, marker, format, arguments)
    base.debug(marker, format, arguments)
  }

  override def debug(marker: Marker, msg: String, t: Throwable): Unit = {
    recordEvent_0Args(Level.DEBUG, marker, msg, t)
    base.debug(marker, msg, t)
  }

  override def isInfoEnabled: Boolean = base.isInfoEnabled

  override def info(msg: String): Unit = {
    recordEvent_0Args(Level.INFO, null, msg, null)
    base.info(msg)
  }

  override def info(format: String, arg: Any): Unit = {
    recordEvent_1Args(Level.INFO, null, format, arg)
    base.info(format, arg)
  }

  override def info(format: String, arg1: Any, arg2: Any): Unit = {
    recordEvent2Args(Level.INFO, null, format, arg1, arg2)
    base.info(format, arg1, arg2)
  }

  override def info(format: String, arguments: Object*): Unit = {
    recordEventArgArray(Level.INFO, null, format, arguments)
    base.info(format, arguments)
  }

  override def info(msg: String, t: Throwable): Unit = {
    recordEvent_0Args(Level.INFO, null, msg, t)
    base.info(msg, t)
  }

  override def isInfoEnabled(marker: Marker): Boolean = base.isInfoEnabled(marker)

  override def info(marker: Marker, msg: String): Unit = {
    recordEvent_0Args(Level.INFO, marker, msg, null)
    base.info(marker, msg)
  }

  override def info(marker: Marker, format: String, arg: Any): Unit = {
    recordEvent_1Args(Level.INFO, marker, format, arg)
    base.info(marker, format, arg)
  }

  override def info(marker: Marker, format: String, arg1: Any, arg2: Any): Unit = {
    recordEvent2Args(Level.INFO, marker, format, arg1, arg2)
    base.info(marker, format, arg1, arg2)
  }

  override def info(marker: Marker, format: String, arguments: Object*): Unit = {
    recordEventArgArray(Level.INFO, marker, format, arguments)
    base.info(marker, format, arguments: _*)
  }

  override def info(marker: Marker, msg: String, t: Throwable): Unit = {
    recordEvent_0Args(Level.INFO, marker, msg, t)
    base.info(marker, msg, t)
  }

  override def isWarnEnabled: Boolean = base.isWarnEnabled

  override def warn(msg: String): Unit = {
    recordEvent_0Args(Level.WARN, null, msg, null)
    base.warn(msg)
  }

  override def warn(format: String, arg: Any): Unit = {
    recordEvent_1Args(Level.WARN, null, format, arg)
    base.warn(format, arg)
  }

  override def warn(format: String, arg1: Any, arg2: Any): Unit = {
    recordEvent2Args(Level.WARN, null, format, arg1, arg2)
    base.warn(format, arg1, arg2)
  }

  override def warn(format: String, arguments: Object*): Unit = {
    recordEventArgArray(Level.WARN, null, format, arguments)
    base.warn(format, arguments: _*)
  }

  override def warn(msg: String, t: Throwable): Unit = {
    recordEvent_0Args(Level.WARN, null, msg, t)
    base.warn(msg, t)
  }

  override def isWarnEnabled(marker: Marker): Boolean = base.isWarnEnabled(marker)

  override def warn(marker: Marker, msg: String): Unit = {
    recordEvent_0Args(Level.WARN, marker, msg, null)
    base.warn(marker, msg)
  }

  override def warn(marker: Marker, format: String, arg: Any): Unit = {
    recordEvent_1Args(Level.WARN, marker, format, arg)
    base.warn(marker, format, arg)
  }

  override def warn(marker: Marker, format: String, arg1: Any, arg2: Any): Unit = {
    recordEvent2Args(Level.WARN, marker, format, arg1, arg2)
    base.warn(marker, format, arg1, arg2)
  }

  override def warn(marker: Marker, format: String, arguments: Object*): Unit = {
    recordEventArgArray(Level.WARN, marker, format, arguments)
    base.warn(marker, format, arguments)
  }

  override def warn(marker: Marker, msg: String, t: Throwable): Unit = {
    recordEvent_0Args(Level.WARN, marker, msg, t)
    base.warn(marker, msg, t)
  }

  override def isErrorEnabled: Boolean = base.isErrorEnabled

  override def error(msg: String): Unit = {
    recordEvent_0Args(Level.ERROR, null, msg, null)
    base.error(msg)
  }

  override def error(format: String, arg: Any): Unit = {
    recordEvent_1Args(Level.ERROR, null, format, arg)
    base.error(format, arg)
  }

  override def error(format: String, arg1: Any, arg2: Any): Unit = {
    recordEvent2Args(Level.ERROR, null, format, arg1, arg2)
    base.error(format, arg1, arg2)
  }

  override def error(format: String, arguments: Object*): Unit = {
    recordEventArgArray(Level.ERROR, null, format, arguments)
    base.error(format, arguments)
  }

  override def error(msg: String, t: Throwable): Unit = {
    recordEvent_0Args(Level.ERROR, null, msg, t)
    base.error(msg, t)
  }

  override def isErrorEnabled(marker: Marker): Boolean = base.isErrorEnabled(marker)

  override def error(marker: Marker, msg: String): Unit = {
    recordEvent_0Args(Level.ERROR, marker, msg, null)
    base.error(marker, msg)
  }

  override def error(marker: Marker, format: String, arg: Any): Unit = {
    recordEvent_1Args(Level.ERROR, marker, format, arg)
    base.error(marker, format, arg)
  }

  override def error(marker: Marker, format: String, arg1: Any, arg2: Any): Unit = {
    recordEvent2Args(Level.ERROR, marker, format, arg1, arg2)
    base.error(marker, format, arg1, arg2)
  }

  override def error(marker: Marker, format: String, arguments: Object*): Unit = {
    recordEventArgArray(Level.ERROR, marker, format, arguments)
    base.error(marker, format, arguments: _*)
  }

  override def error(marker: Marker, msg: String, t: Throwable): Unit = {
    recordEvent_0Args(Level.ERROR, marker, msg, t)
    base.error(marker, msg, t)
  }

  private def recordEvent_0Args(level: Level, marker: Marker, msg: String, t: Throwable): Unit = {
    recordEvent(level, marker, msg, null, t)
  }

  private def recordEvent_1Args(level: Level, marker: Marker, msg: String, arg1: Any): Unit = {
    recordEvent(level, marker, msg, Seq(arg1), null)
  }

  private def recordEvent2Args(level: Level, marker: Marker, msg: String, arg1: Any, arg2: Any): Unit = {
    if (arg2.isInstanceOf[Throwable]) {
      recordEvent(level, marker, msg, Seq(arg1), arg2.asInstanceOf[Throwable])
    }
    else {
      recordEvent(level, marker, msg, Seq(arg1, arg2), null)
    }
  }

  private def recordEventArgArray(level: Level, marker: Marker, msg: String, args: Seq[Object]): Unit = {
    val throwableCandidate: Throwable = MessageFormatter.getThrowableCandidate(args.toArray)
    if (throwableCandidate != null) {
      val trimmedCopy: Array[AnyRef] = MessageFormatter.trimmedCopy(args.toArray)
      recordEvent(level, marker, msg, trimmedCopy, throwableCandidate)
    }
    else {
      recordEvent(level, marker, msg, args, null)
    }
  }

  // WARNING: this method assumes that any throwable is properly extracted
  private def recordEvent(level: Level, marker: Marker, msg: String, args: Seq[Any], throwable: Throwable): Unit = {
    val self = this
    source.offer(new LoggingEvent {
      override def getLevel: Level = level

      override def getMarker: Marker = marker

      override def getLoggerName: String = self.getName

      override def getMessage: String = msg

      override def getThreadName: String = Thread.currentThread.getName

      override def getArgumentArray: Array[Object] = Option(args).fold(Array.empty[Object])(_.map(_.asInstanceOf[AnyRef]).toArray)

      override def getTimeStamp: Long = System.currentTimeMillis

      override def getThrowable: Throwable = throwable
    })
  }

  def recordEvent(loggingEvent: LoggingEvent): Unit = {
    source.offer(loggingEvent)
  }
}
