package ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory

import ca.stevenskelton.tinyakkaslackqueue.{ScheduledSlackTask, SlackTaskMeta}

import scala.collection.SortedSet

case class TaskHistory(
                        slackTaskMeta: SlackTaskMeta,
                        running: Option[(ScheduledSlackTask, Option[TaskHistoryItem[CancelHistoryItem]])],
                        executed: SortedSet[TaskHistoryItem[TaskHistoryOutcomeItem]],
                        pending: SortedSet[ScheduledSlackTask]
                      )
