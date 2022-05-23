package ca.stevenskelton.tinyakkaslackqueue.blocks

import ca.stevenskelton.tinyakkaslackqueue._

import scala.collection.SortedSet

case class TaskHistory(
                        slackTaskIdentifier: SlackTaskIdentifier,
                        running: Option[ScheduledSlackTask],
                        executed: SortedSet[TaskHistoryItem],
                        pending: SortedSet[ScheduledSlackTask]
                      )
