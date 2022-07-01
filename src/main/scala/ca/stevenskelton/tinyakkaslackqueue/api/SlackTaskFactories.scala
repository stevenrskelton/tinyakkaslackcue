package ca.stevenskelton.tinyakkaslackqueue.api

object SlackTaskFactories {
  def apply(slackTaskFactories: SlackTaskFactory[_, _]*): SlackTaskFactories = new SlackTaskFactories {
    override val factories: List[SlackTaskFactory[_, _]] = slackTaskFactories.toList
  }
}

trait SlackTaskFactories {
  val factories: List[SlackTaskFactory[_, _]]
}
