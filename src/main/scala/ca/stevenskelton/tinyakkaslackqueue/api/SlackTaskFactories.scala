package ca.stevenskelton.tinyakkaslackqueue.api

object SlackTaskFactories {
  def apply(slackTaskFactories: SlackTaskFactory[?,?]*): SlackTaskFactories = new SlackTaskFactories {
    override val factories: List[SlackTaskFactory[?,?]] = slackTaskFactories.toList
  }
}

trait SlackTaskFactories {
  val factories: List[SlackTaskFactory[?,?]]
}
