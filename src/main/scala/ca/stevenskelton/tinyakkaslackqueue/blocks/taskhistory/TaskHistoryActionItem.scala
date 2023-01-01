package ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory

import play.api.libs.json.JsObject

import java.time.ZonedDateTime

trait TaskHistoryOutcomeItem extends TaskHistoryActionItem {
  def sectionBlocks: Seq[JsObject]

  def icon: String

  def start: ZonedDateTime
}

trait TaskHistoryActionItem {
  def action: String
}
