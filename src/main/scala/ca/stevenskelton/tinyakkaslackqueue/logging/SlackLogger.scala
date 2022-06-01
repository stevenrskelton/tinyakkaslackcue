package ca.stevenskelton.tinyakkaslackqueue.logging

import akka.stream.QueueOfferResult.{Dropped, QueueClosed}
import akka.stream.scaladsl.SourceQueueWithComplete
import org.slf4j.event.{Level, LoggingEvent}
import org.slf4j.helpers.MessageFormatter
import org.slf4j.{Logger, Marker}

import scala.util.{Failure, Success}

trait Guarentee extends Marker

class SlackLogger(
                   override val getName: String,
                   source: SourceQueueWithComplete[LoggingEvent],
                   backup: Option[Logger] = None,
                   override val isTraceEnabled: Boolean = true,
                   override val isDebugEnabled: Boolean = true,
                   override val isInfoEnabled: Boolean = true,
                   override val isWarnEnabled: Boolean = true,
                   override val isErrorEnabled: Boolean = true
                 ) extends Logger {

  override def trace(msg: String): Unit = recordEvent_0Args(Level.TRACE, null, msg, null)

  override def trace(format: String, arg: Any): Unit = recordEvent_1Args(Level.TRACE, null, format, arg)

  override def trace(format: String, arg1: Any, arg2: Any): Unit = recordEvent2Args(Level.TRACE, null, format, arg1, arg2)

  override def trace(format: String, arguments: Object*): Unit = recordEventArgArray(Level.TRACE, null, format, arguments)

  override def trace(msg: String, t: Throwable): Unit = recordEvent_0Args(Level.TRACE, null, msg, t)

  override def isTraceEnabled(marker: Marker): Boolean = backup.exists(_.isTraceEnabled(marker)) || isTraceEnabled

  override def trace(marker: Marker, msg: String): Unit = {
    recordEvent_0Args(Level.TRACE, marker, msg, null)
    if (marker.isInstanceOf[Guarentee]) backup.foreach(_.trace(marker, msg))
  }

  override def trace(marker: Marker, format: String, arg: Any): Unit = {
    recordEvent_1Args(Level.TRACE, marker, format, arg)
    if (marker.isInstanceOf[Guarentee]) backup.foreach(_.trace(marker, format, arg))
  }

  override def trace(marker: Marker, format: String, arg1: Any, arg2: Any): Unit = {
    recordEvent2Args(Level.TRACE, marker, format, arg1, arg2)
    if (marker.isInstanceOf[Guarentee]) backup.foreach(_.trace(marker, format, arg1, arg2))
  }

  override def trace(marker: Marker, format: String, argArray: Object*): Unit = {
    recordEventArgArray(Level.TRACE, marker, format, argArray)
    if (marker.isInstanceOf[Guarentee]) backup.foreach(_.trace(marker, format, argArray: _*))
  }

  override def trace(marker: Marker, msg: String, t: Throwable): Unit = {
    recordEvent_0Args(Level.TRACE, marker, msg, t)
    if (marker.isInstanceOf[Guarentee]) backup.foreach(_.trace(marker, msg, t))
  }

  override def debug(msg: String): Unit = {
    recordEvent_0Args(Level.DEBUG, null, msg, null)
  }

  override def debug(format: String, arg: Any): Unit = {
    recordEvent_1Args(Level.DEBUG, null, format, arg)
  }

  override def debug(format: String, arg1: Any, arg2: Any): Unit = {
    recordEvent2Args(Level.DEBUG, null, format, arg1, arg2)
  }

  override def debug(format: String, arguments: Object*): Unit = {
    recordEventArgArray(Level.DEBUG, null, format, arguments)
  }

  override def debug(msg: String, t: Throwable): Unit = {
    recordEvent_0Args(Level.DEBUG, null, msg, t)
  }

  override def isDebugEnabled(marker: Marker): Boolean = backup.exists(_.isDebugEnabled(marker)) || isDebugEnabled

  override def debug(marker: Marker, msg: String): Unit = {
    recordEvent_0Args(Level.DEBUG, marker, msg, null)
    if (marker.isInstanceOf[Guarentee]) backup.foreach(_.debug(marker, msg))
  }

  override def debug(marker: Marker, format: String, arg: Any): Unit = {
    recordEvent_1Args(Level.DEBUG, marker, format, arg)
    if (marker.isInstanceOf[Guarentee]) backup.foreach(_.debug(marker, format, arg))
  }

  override def debug(marker: Marker, format: String, arg1: Any, arg2: Any): Unit = {
    recordEvent2Args(Level.DEBUG, marker, format, arg1, arg2)
    if (marker.isInstanceOf[Guarentee]) backup.foreach(_.debug(marker, format, arg1, arg2))
  }

  override def debug(marker: Marker, format: String, arguments: Object*): Unit = {
    recordEventArgArray(Level.DEBUG, marker, format, arguments)
    if (marker.isInstanceOf[Guarentee]) backup.foreach(_.debug(marker, format, arguments))
  }

  override def debug(marker: Marker, msg: String, t: Throwable): Unit = {
    recordEvent_0Args(Level.DEBUG, marker, msg, t)
    if (marker.isInstanceOf[Guarentee]) backup.foreach(_.debug(marker, msg, t))
  }

  override def info(msg: String): Unit = {
    recordEvent_0Args(Level.INFO, null, msg, null)
  }

  override def info(format: String, arg: Any): Unit = {
    recordEvent_1Args(Level.INFO, null, format, arg)
  }

  override def info(format: String, arg1: Any, arg2: Any): Unit = {
    recordEvent2Args(Level.INFO, null, format, arg1, arg2)
  }

  override def info(format: String, arguments: Object*): Unit = {
    recordEventArgArray(Level.INFO, null, format, arguments)
  }

  override def info(msg: String, t: Throwable): Unit = {
    recordEvent_0Args(Level.INFO, null, msg, t)
  }

  override def isInfoEnabled(marker: Marker): Boolean = backup.exists(_.isInfoEnabled(marker)) || isInfoEnabled

  override def info(marker: Marker, msg: String): Unit = {
    recordEvent_0Args(Level.INFO, marker, msg, null)
    if (marker.isInstanceOf[Guarentee]) backup.foreach(_.info(marker, msg))
  }

  override def info(marker: Marker, format: String, arg: Any): Unit = {
    recordEvent_1Args(Level.INFO, marker, format, arg)
    if (marker.isInstanceOf[Guarentee]) backup.foreach(_.info(marker, format, arg))
  }

  override def info(marker: Marker, format: String, arg1: Any, arg2: Any): Unit = {
    recordEvent2Args(Level.INFO, marker, format, arg1, arg2)
    if (marker.isInstanceOf[Guarentee]) backup.foreach(_.info(marker, format, arg1, arg2))
  }

  override def info(marker: Marker, format: String, arguments: Object*): Unit = {
    recordEventArgArray(Level.INFO, marker, format, arguments)
    if (marker.isInstanceOf[Guarentee]) backup.foreach(_.info(marker, format, arguments: _*))
  }

  override def info(marker: Marker, msg: String, t: Throwable): Unit = {
    recordEvent_0Args(Level.INFO, marker, msg, t)
    if (marker.isInstanceOf[Guarentee]) backup.foreach(_.info(marker, msg, t))
  }

  override def warn(msg: String): Unit = {
    recordEvent_0Args(Level.WARN, null, msg, null)
  }

  override def warn(format: String, arg: Any): Unit = {
    recordEvent_1Args(Level.WARN, null, format, arg)
  }

  override def warn(format: String, arg1: Any, arg2: Any): Unit = {
    recordEvent2Args(Level.WARN, null, format, arg1, arg2)
  }

  override def warn(format: String, arguments: Object*): Unit = {
    recordEventArgArray(Level.WARN, null, format, arguments)
  }

  override def warn(msg: String, t: Throwable): Unit = {
    recordEvent_0Args(Level.WARN, null, msg, t)
  }

  override def isWarnEnabled(marker: Marker): Boolean = backup.exists(_.isWarnEnabled(marker)) || isWarnEnabled

  override def warn(marker: Marker, msg: String): Unit = {
    recordEvent_0Args(Level.WARN, marker, msg, null)
    if (marker.isInstanceOf[Guarentee]) backup.foreach(_.warn(marker, msg))
  }

  override def warn(marker: Marker, format: String, arg: Any): Unit = {
    recordEvent_1Args(Level.WARN, marker, format, arg)
    if (marker.isInstanceOf[Guarentee]) backup.foreach(_.warn(marker, format, arg))
  }

  override def warn(marker: Marker, format: String, arg1: Any, arg2: Any): Unit = {
    recordEvent2Args(Level.WARN, marker, format, arg1, arg2)
    if (marker.isInstanceOf[Guarentee]) backup.foreach(_.warn(marker, format, arg1, arg2))
  }

  override def warn(marker: Marker, format: String, arguments: Object*): Unit = {
    recordEventArgArray(Level.WARN, marker, format, arguments)
    if (marker.isInstanceOf[Guarentee]) backup.foreach(_.warn(marker, format, arguments))
  }

  override def warn(marker: Marker, msg: String, t: Throwable): Unit = {
    recordEvent_0Args(Level.WARN, marker, msg, t)
    if (marker.isInstanceOf[Guarentee]) backup.foreach(_.warn(marker, msg, t))
  }

  override def error(msg: String): Unit = {
    recordEvent_0Args(Level.ERROR, null, msg, null)
  }

  override def error(format: String, arg: Any): Unit = {
    recordEvent_1Args(Level.ERROR, null, format, arg)
  }

  override def error(format: String, arg1: Any, arg2: Any): Unit = {
    recordEvent2Args(Level.ERROR, null, format, arg1, arg2)
  }

  override def error(format: String, arguments: Object*): Unit = {
    recordEventArgArray(Level.ERROR, null, format, arguments)
  }

  override def error(msg: String, t: Throwable): Unit = {
    recordEvent_0Args(Level.ERROR, null, msg, t)
  }

  override def isErrorEnabled(marker: Marker): Boolean = backup.exists(_.isErrorEnabled(marker)) || isErrorEnabled

  override def error(marker: Marker, msg: String): Unit = {
    recordEvent_0Args(Level.ERROR, marker, msg, null)
    if (marker.isInstanceOf[Guarentee]) backup.foreach(_.error(marker, msg))
  }

  override def error(marker: Marker, format: String, arg: Any): Unit = {
    recordEvent_1Args(Level.ERROR, marker, format, arg)
    if (marker.isInstanceOf[Guarentee]) backup.foreach(_.error(marker, format, arg))
  }

  override def error(marker: Marker, format: String, arg1: Any, arg2: Any): Unit = {
    recordEvent2Args(Level.ERROR, marker, format, arg1, arg2)
    if (marker.isInstanceOf[Guarentee]) backup.foreach(_.error(marker, format, arg1, arg2))
  }

  override def error(marker: Marker, format: String, arguments: Object*): Unit = {
    recordEventArgArray(Level.ERROR, marker, format, arguments)
    if (marker.isInstanceOf[Guarentee]) backup.foreach(_.error(marker, format, arguments: _*))
  }

  override def error(marker: Marker, msg: String, t: Throwable): Unit = {
    recordEvent_0Args(Level.ERROR, marker, msg, t)
    if (marker.isInstanceOf[Guarentee]) backup.foreach(_.error(marker, msg, t))
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
    backup match {
      case None => source.offer(loggingEvent)
      case Some(backupLogger) =>
        source.offer(loggingEvent).onComplete {
          case Success(Dropped) => backupLogger.warn(s"Dropped $getName: $loggingEvent")
          case Success(QueueClosed) => backupLogger.warn(s"Queue $getName Closed: $loggingEvent")
          case Failure(ex) => backupLogger.error(s"Error $getName $ex: $loggingEvent")
          case _ =>
        }(scala.concurrent.ExecutionContext.global)
    }
  }
}
