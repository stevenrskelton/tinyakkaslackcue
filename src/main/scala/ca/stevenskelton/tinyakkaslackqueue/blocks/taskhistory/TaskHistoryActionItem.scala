package ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory

import java.time.ZonedDateTime

trait TaskHistoryOutcomeItem extends TaskHistoryActionItem {
  def sectionBlocks: Seq[String]
  def start: ZonedDateTime
}

trait TaskHistoryActionItem {
  def action: String
}
