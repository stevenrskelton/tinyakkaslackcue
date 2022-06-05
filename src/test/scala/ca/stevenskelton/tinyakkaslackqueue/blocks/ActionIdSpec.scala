package ca.stevenskelton.tinyakkaslackqueue.blocks

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ActionIdSpec extends AnyWordSpec
  with Matchers
  with ScalaFutures {

  "getIndex" should {
    "parse some" in {
      val compoundActionId = ActionId("channels-select-0")
      val (actionId, index) = compoundActionId.getIndex.get
      actionId shouldBe ActionId("channel-select")
      index shouldBe 0
    }
    "parse none" in {
      val compoundActionId = ActionId("channels-select")
      compoundActionId.getIndex shouldBe None
    }
  }
}

