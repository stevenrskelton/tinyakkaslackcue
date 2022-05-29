package ca.stevenskelton.tinyakkaslackqueue.blocks

case class ActionId(value: String) extends AnyVal {
  override def toString: String = value
}

object ActionId {
  val TaskQueue = ActionId("task-queue-action")
  val TaskSchedule = ActionId("schedule-task-action")
  val TaskCancel = ActionId("task-cancel")
  val TaskView = ActionId("view-task")
  val TaskThread = ActionId("view-thread")
  val TaskHistory = ActionId("view-history")

  val ScheduleDate = ActionId("datepicker-action")
  val ScheduleTime = ActionId("timepicker-action")
  val NotifyOnComplete = ActionId("multi-users-notify-on-complete")
  val NotifyOnFailure = ActionId("multi-users-notify-on-failure")
  val LogLevel = ActionId("static-select-action")

  val TabRefresh = ActionId("tab-refresh")
}
