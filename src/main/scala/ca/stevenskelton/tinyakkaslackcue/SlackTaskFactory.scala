package ca.stevenskelton.tinyakkaslackcue

trait SlackTaskFactory {
  def name: String
  def description: Mrkdwn
  def create(ts: SlackTs, createdBy: SlackUserId, notifyOnError: Seq[SlackUserId], notifyOnComplete: Seq[SlackUserId]): SlackTask
}