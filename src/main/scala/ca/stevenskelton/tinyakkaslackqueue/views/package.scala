package ca.stevenskelton.tinyakkaslackqueue

package object views {
  sealed trait SlackView

  trait SlackHomeTab extends SlackView

  trait SlackModal extends SlackView

  object SlackOkResponse extends SlackView
}
