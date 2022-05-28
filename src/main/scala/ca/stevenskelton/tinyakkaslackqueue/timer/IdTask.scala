package ca.stevenskelton.tinyakkaslackqueue.timer

import akka.actor.Cancellable
import org.slf4j.Logger

trait IdTask[T] extends Cancellable {
  def id: T

  private var shouldCancel: Boolean = false

  override def isCancelled: Boolean = shouldCancel

  override def cancel(): Boolean = {
    if (shouldCancel) false
    else {
      shouldCancel = true
      true
    }
  }

  def run(logger: Logger): Unit
}
