package ca.stevenskelton.tinyakkaslackqueue

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

  val logLevelEmoji: Level => String = {
    case Level.INFO => EmojiINFO
    case Level.WARN => EmojiWARN
    case Level.ERROR => EmojiERROR
    case Level.TRACE => EmojiTRACE
    case Level.DEBUG => EmojiDEBUG
  }

}
