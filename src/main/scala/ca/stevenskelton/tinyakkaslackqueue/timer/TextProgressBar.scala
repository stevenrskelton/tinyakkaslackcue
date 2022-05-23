package ca.stevenskelton.tinyakkaslackqueue.timer

object TextProgressBar {
  val Unicode = new TextProgressBar(Array(' ', '▏', '▎', '▍', '▌', '▋', '▊', '▉', '█'))
  val SlackEmoji = new TextProgressBar(Array(":white_large_square:", ":black_small_square:", ":black_medium_square:", ":black_large_square:"))
}

class TextProgressBar(progressCharacters: Array[_]) {

  def bar(progress: Float, width: Int): String = {
    require(0 <= progress && progress <= 1 && width > 0)
    val completeWidth = math.floor(progress * width).toInt
    val partialWidth = (progress * width) % 1
    val progressIndex = math.floor(partialWidth * (progressCharacters.size - 1)).toInt
    val progressChar = if (width == completeWidth) "" else progressCharacters(progressIndex).toString
    val completeBar = progressCharacters.last.toString * completeWidth
    val remainingBar = progressCharacters.head.toString * (width - completeWidth - 1)
    s"$completeBar$progressChar$remainingBar"
  }

  def create(progress: Float, totalCharacters: Int): String = {
    val line = s"[${bar(progress, totalCharacters - 7)}] ${("  " + math.round(progress * 100)).takeRight(3)}%"
    println(line)
    line
  }
}
