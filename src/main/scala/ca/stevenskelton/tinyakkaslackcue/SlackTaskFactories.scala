package ca.stevenskelton.tinyakkaslackcue

import ca.stevenskelton.tinyakkaslackcue.blocks.{PrivateMetadata, TaskHistory}

trait SlackTaskFactories {

  def tinySlackCue: TinySlackCue

  def factories: Seq[SlackTaskFactory]

  def history: Seq[TaskHistory]

  def findByName(name: String): Option[SlackTaskFactory] = {
    factories.find(_.name == name)
  }

  def findByPrivateMetadata(privateMetadata: PrivateMetadata): Option[SlackTaskFactory] = {
    factories.find(_.name.take(25) == privateMetadata.value)
  }
}
