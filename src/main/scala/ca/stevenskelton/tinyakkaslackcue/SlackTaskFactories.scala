package ca.stevenskelton.tinyakkaslackcue

import ca.stevenskelton.tinyakkaslackcue.blocks.TaskHistory

trait SlackTaskFactories {
  def factories: Seq[SlackTaskFactory]
  def history: Seq[TaskHistory]
}
