package ca.stevenskelton.tinyakkaslackcue

import org.slf4j.event.Level

package object blocks {

  val EmojiINFO = ":eyes:"
  val EmojiWARN = ":rotating_light:"
  val EmojiERROR = ":boom:"
  val EmojiTRACE = ":spider_web:"
  val EmojiDEBUG = ":warning:"

  val logLevelEmoji: Level => String = {
    case Level.INFO => EmojiINFO
    case Level.WARN => EmojiWARN
    case Level.ERROR => EmojiERROR
    case Level.TRACE => EmojiTRACE
    case Level.DEBUG => EmojiDEBUG
  }

  case class ActionId(value: String) extends AnyVal{
    override def toString: String = value
  }

  case class PrivateMetadata(value:String) extends AnyVal

  object PrivateMetadata {
    val Empty = PrivateMetadata("")
  }

}
