//package ca.stevenskelton.tinyakkaslackqueue.blocks
//
//import ca.stevenskelton.tinyakkaslackqueue.TestData
//import ca.stevenskelton.tinyakkaslackqueue.views.HomeTab
//import org.scalatest.concurrent.ScalaFutures
//import org.scalatest.matchers.should.Matchers
//import org.scalatest.wordspec.AnyWordSpec
//import play.api.libs.json.{JsDefined, JsObject, JsString, Json}
//
//import java.time.ZoneId
//
//class SlackViewSpec extends AnyWordSpec
//  with Matchers
//  with ScalaFutures {
//
//  "queue" should {
//    "read fields" in {
//      implicit val slackFactories = TestData.slackTaskFactories
//      val view = new HomeTab(ZoneId.of("Canada/Eastern"))
//      val json = Json.parse(view.toString).as[JsObject]
//      json \ "type" shouldBe JsDefined(JsString("home"))
//    }
//  }
//}
