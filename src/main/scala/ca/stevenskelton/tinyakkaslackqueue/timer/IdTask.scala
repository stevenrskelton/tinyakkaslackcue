package ca.stevenskelton.tinyakkaslackqueue.timer

import akka.actor.Cancellable

trait IdTask[T] extends Runnable with Cancellable {
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

  override def run(): Unit
}
