package ca.stevenskelton.tinyakkaslackcue

trait SlackTaskIdentifier {
  def name: String

  def description: Mrkdwn
}

trait SlackTaskFactory extends SlackTaskIdentifier {
  def create(ts: SlackTs, createdBy: SlackUserId, notifyOnError: Seq[SlackUserId], notifyOnComplete: Seq[SlackUserId]): SlackTask
}