package ca.stevenskelton.tinyakkaslackqueue.util

import org.slf4j.Logger
import play.api.libs.json.{JsValue, Json}

import java.io._
import java.security.MessageDigest
import scala.util.{Success, Try, Using}

object FileUtils {

  def readJson(file: File): Try[JsValue] = Using(new FileInputStream(file)) {
    Json.parse(_)
  }

  def humanFileSize(file: File): String = {
    if (!file.exists) ""
    else {
      val size = file.length
      if (size > 1000000) {
        s"${(size / 1000000).toInt}mb"
      } else if (size > 1024) {
        s"${(size / 1024).toInt}kb"
      } else {
        s"${size}bytes"
      }
    }
  }
}

