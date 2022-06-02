package ca.stevenskelton.tinyakkaslackqueue

import ca.stevenskelton.tinyakkaslackqueue.blocks.taskhistory.{CancelHistoryItem, ErrorHistoryItem, SuccessHistoryItem, TaskHistoryOutcomeItem}
import org.slf4j.event.Level

package object blocks {

  val EmojiINFO = ":eyes:"
  val EmojiWARN = ":rotating_light:"
  val EmojiERROR = ":boom:"
  val EmojiTRACE = ":spider_web:"
  val EmojiDEBUG = ":warning:"

  val TaskFailure = ":headstone:"
  val TaskSuccess = ":white_check_mark:"
  val TaskCancelled = ":no_entry_sign:"
  val TaskScheduled = ":hourglass_flowing_sand:"

  def taskOutcomeicon(outcome: TaskHistoryOutcomeItem): String = {
    outcome match {
      case _:CancelHistoryItem => TaskCancelled
      case _:ErrorHistoryItem => TaskFailure
      case _:SuccessHistoryItem => TaskSuccess
    }
  }

  val logLevelEmoji: Level => String = {
    case Level.INFO => EmojiINFO
    case Level.WARN => EmojiWARN
    case Level.ERROR => EmojiERROR
    case Level.TRACE => EmojiTRACE
    case Level.DEBUG => EmojiDEBUG
  }

}
