package ca.stevenskelton.tinyakkaslackcue

abstract class SlackTask extends UUIDTask {
  def name: String

  def description: Mrkdwn

  def ts: SlackTs

  def createdBy: SlackUserId

  def notifyOnError: Seq[SlackUserId]

  def notifyOnComplete: Seq[SlackUserId]
}