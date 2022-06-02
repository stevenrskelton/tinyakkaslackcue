package ca.stevenskelton.tinyakkaslackqueue.blocks

case class ActionId(value: String) extends AnyVal {
  override def toString: String = value
}

object ActionId {
  /**
   * Create task to run as soon as available
   */
  val TaskQueue = ActionId("task-queue")
  /**
   * Create task to run at specified time
   */
  val TaskSchedule = ActionId("task-schedule")
  /**
   * Signal task to cancel
   */
  val TaskCancel = ActionId("task-cancel")
  /**
   * View scheduled task details in modal
   */
  val ModalQueuedTaskView = ActionId("modal-task-view")
  /**
   * Redirect to thread for running task's logs
   */
  val RedirectToTaskThread = ActionId("redirect-task-thread")
  /**
   * Show list of all previous task executions
   */
  val HomeTabTaskHistory = ActionId("home-history")
  /**
   * Refresh home tab
   */
  val HomeTabRefresh = ActionId("home-refresh")
  /**
   * Show configure page in home tab
   */
  val HomeTabConfiguration = ActionId("home-configure")

  /*
   * Create Task Data Fields
   */
  val DataScheduleDate = ActionId("datepicker")
  val DataScheduleTime = ActionId("timepicker")
  val DataNotifyOnComplete = ActionId("multi-users-notify-on-complete")
  val DataNotifyOnFailure = ActionId("multi-users-notify-on-failure")
  val DataLogLevel = ActionId("static-select")

}
