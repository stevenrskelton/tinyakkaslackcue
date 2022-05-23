package ca.stevenskelton.tinyakkaslackqueue

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class TextProgressBarSpec extends AnyWordSpec
  with Matchers
  with ScalaFutures {

  "integers" when {

    val textProgressBar = new TextProgressBar(Array(0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100))

    "width 1" should {
      "match 0%" in {
        textProgressBar.bar(0, 1) shouldBe "0"
      }
      "match 0.1%" in {
        textProgressBar.bar(0.001f, 1) shouldBe "0"
      }
      "match 5%" in {
        textProgressBar.bar(0.05f, 1) shouldBe "0"
      }
      "match 9.9%" in {
        textProgressBar.bar(0.099f, 1) shouldBe "0"
      }
      "match 10%" in {
        textProgressBar.bar(0.10f, 1) shouldBe "10"
      }
      "match 10.1%" in {
        textProgressBar.bar(0.101f, 1) shouldBe "10"
      }
      "match 49%" in {
        textProgressBar.bar(0.49f, 1) shouldBe "40"
      }
      "match 50%" in {
        textProgressBar.bar(0.51f, 1) shouldBe "50"
      }
      "handle 89%" in {
        textProgressBar.bar(0.89f, 1) shouldBe "80"
      }
      "match 90%" in {
        textProgressBar.bar(0.90f, 1) shouldBe "90"
      }
      "match 99%" in {
        textProgressBar.bar(0.99f, 1) shouldBe "90"
      }
      "match 100%" in {
        textProgressBar.bar(1, 1) shouldBe "100"
      }
    }
  }

  "multiple characters" when {

    val textProgressBar = new TextProgressBar(Array(":sloth:", ":working:", ":working:", ":working:", ":working:", ":working-on-it:", ":working-on-it:", ":working-on-it:", ":working-on-it:", ":firecracker:", ":done-slant:"))

    "width 1" should {
      "match 0%" in {
        textProgressBar.bar(0, 1) shouldBe ":sloth:"
      }
      "match 5%" in {
        textProgressBar.bar(0.05f, 1) shouldBe ":sloth:"
      }
      "match 11%" in {
        textProgressBar.bar(0.11f, 1) shouldBe ":working:"
      }
      "match 49%" in {
        textProgressBar.bar(0.49f, 1) shouldBe ":working:"
      }
      "match 50%" in {
        textProgressBar.bar(0.51f, 1) shouldBe ":working-on-it:"
      }
      "handle 89%" in {
        textProgressBar.bar(0.89f, 1) shouldBe ":working-on-it:"
      }
      "match 90%" in {
        textProgressBar.bar(0.90f, 1) shouldBe ":firecracker:"
      }
      "match 99%" in {
        textProgressBar.bar(0.99f, 1) shouldBe ":firecracker:"
      }
      "match 100%" in {
        textProgressBar.bar(1, 1) shouldBe ":done-slant:"
      }
    }
  }

  "Unicode characters" when {
    val textProgressBar = TextProgressBar.Unicode

    "50 characters in length" should {
      "handle zero" in {
        textProgressBar.bar(0, 50).length shouldBe 50
        textProgressBar.create(0, 50).length shouldBe 50
      }
      "handle 50%" in {
        textProgressBar.bar(0.5f, 50).length shouldBe 50
        textProgressBar.create(0.5f, 50).length shouldBe 50
      }
      "handle 100%" in {
        textProgressBar.bar(1, 50).length shouldBe 50
        textProgressBar.create(1, 50).length shouldBe 50
      }
    }
    "53 characters in length" should {
      "handle zero" in {
        textProgressBar.bar(0, 53).length shouldBe 53
        textProgressBar.create(0, 53).length shouldBe 53
      }
      "handle 50%" in {
        textProgressBar.bar(0.5f, 53).length shouldBe 53
        textProgressBar.create(0.5f, 53).length shouldBe 53
      }
      "handle 100%" in {
        textProgressBar.bar(1, 53).length shouldBe 53
        textProgressBar.create(1, 53).length shouldBe 53
      }
    }
    "width 1" should {
      "match 0%" in {
        textProgressBar.bar(0, 1) shouldBe " "
      }
      "match 1%" in {
        textProgressBar.bar(0.01f, 1) shouldBe " "
      }
      "match 12.5%" in {
        textProgressBar.bar(0.13f, 1) shouldBe "▏"
      }
      "match 25%" in {
        textProgressBar.bar(0.26f, 1) shouldBe "▎"
      }
      "match 37.5%" in {
        textProgressBar.bar(0.38f, 1) shouldBe "▍"
      }
      "match 50%" in {
        textProgressBar.bar(0.51f, 1) shouldBe "▌"
      }
      "handle 62.5%" in {
        textProgressBar.bar(0.63f, 1) shouldBe "▋"
      }
      "match 75%" in {
        textProgressBar.bar(0.76f, 1) shouldBe "▊"
      }
      "match 87.5%" in {
        textProgressBar.bar(0.88f, 1) shouldBe "▉"
      }
      "match 99%" in {
        textProgressBar.bar(0.99f, 1) shouldBe "▉"
      }
      "match 100%" in {
        textProgressBar.bar(1, 1) shouldBe "█"
      }
    }
  }

}
