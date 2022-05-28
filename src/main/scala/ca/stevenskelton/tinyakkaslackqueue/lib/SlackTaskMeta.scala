package ca.stevenskelton.tinyakkaslackqueue.lib

import ca.stevenskelton.tinyakkaslackqueue.{SlackChannel, SlackTs}

case class SlackTaskMeta(channel: SlackChannel, historyThread: SlackTs, factory: SlackTaskFactory)
