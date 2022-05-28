package ca.stevenskelton.tinyakkaslackqueue.blocks

import ca.stevenskelton.tinyakkaslackqueue._
import ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory.TaskHistoryItem
import ca.stevenskelton.tinyakkaslackqueue.lib.SlackTaskMeta

import scala.collection.SortedSet

case class TaskHistory(
                        slackTaskMeta: SlackTaskMeta,
                        running: Option[ScheduledSlackTask],
                        executed: SortedSet[TaskHistoryItem],
                        pending: SortedSet[ScheduledSlackTask]
                      )
