package ca.stevenskelton.tinyakkaslackqueue.blocks

case class ActionId(value: String) extends AnyVal {
  def getIndex: Option[(ActionId, Int)] = {
    val separator = value.lastIndexOf("-")
    if (separator > -1) {
      value.drop(separator + 1).toIntOption.map {
        i => (ActionId(value.take(separator)), i)
      }
    } else {
      None
    }
  }
}

object ActionId {
  /**
   * Signal task to cancel
   */
  val TaskCancel: ActionId = ActionId("task-cancel")
  /**
   * Show modal to create task to run as soon as available
   */
  val ModalTaskQueue: ActionId = ActionId("task-queue")
  /**
   * Show modal to create task to run at specified time
   */
  val ModalTaskSchedule: ActionId = ActionId("task-schedule")
  /**
   * View scheduled task details in modal
   */
  val ModalQueuedTaskView: ActionId = ActionId("modal-task-view")
  /**
   * Redirect to thread for running task's logs
   */
  val RedirectToTaskThread: ActionId = ActionId("redirect-task-thread")
  /**
   * Show list of all previous task executions
   */
  val HomeTabTaskHistory: ActionId = ActionId("home-history")
  /**
   * Refresh home tab
   */
  val HomeTabRefresh: ActionId = ActionId("home-refresh")
  /**
   * Show configure page in home tab
   */
  val HomeTabConfiguration: ActionId = ActionId("home-configure")

  /*
   * Create Task Data Fields
   */
  val DataScheduleDate: ActionId = ActionId("data-scheduled-datepicker")
  val DataScheduleTime: ActionId = ActionId("data-scheduled-timepicker")
  val DataNotifyOnComplete: ActionId = ActionId("data-multi-users-notify-on-complete")
  val DataNotifyOnFailure: ActionId = ActionId("data-multi-users-notify-on-failure")
  val DataLogLevel: ActionId = ActionId("data-log-level")
  val DataChannel: ActionId = ActionId("data-channel")
  val DataMovedExchangeSymbol: ActionId = ActionId("data-moved-exchange-symbols")

}
