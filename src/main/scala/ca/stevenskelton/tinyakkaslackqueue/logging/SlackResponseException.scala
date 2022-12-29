package ca.stevenskelton.tinyakkaslackqueue.logging

import com.slack.api.methods.SlackApiTextResponse
import org.slf4j.Logger

import scala.util.{Failure, Success, Try}

case class SlackResponseException(response: SlackApiTextResponse) extends Exception(response.getError)

object SlackResponseException {
  def logError[A](value: Try[A], logger: Logger): Try[A] = value match {
    case Success(_) => value
    case Failure(ex) =>
      logger.error("SlackResponseException", ex)
      value
  }
}

