package ca.stevenskelton.tinyakkaslackcue

trait SlackTaskFactories {
  def factories: Seq[SlackTaskFactory]
}
