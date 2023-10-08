package ca.stevenskelton.tinyakkaslackqueue.timer

import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.slf4j.{Logger, LoggerFactory}

import java.time.*
import java.util.UUID

class InteractiveJavaUtilTimerSpec extends AnyWordSpec
  with Matchers
  with ScalaFutures {

  var i = 0

  abstract class NamedTask(val name: String) extends IdTask[UUID] {
    override val id = UUID.randomUUID()
  }

  implicit val logger: Logger = LoggerFactory.getLogger("Specs")
  val timer = new InteractiveJavaUtilTimer[UUID, NamedTask]
  val timeout2sec = timeout(Span(5, Seconds))

  def createTask1 = new NamedTask("name1") {
    override def run(): Unit = {
      Thread.sleep(200)
      if (isCancelled) i = -10
      else i += 1
    }
  }

  def createTask2 = new NamedTask("name2") {
    override def run(): Unit = {
      Thread.sleep(100)
      i += 3
    }
  }

  def createTask3 = new NamedTask("name3") {
    override def run(): Unit = {
      Thread.sleep(100)
      i += 5
    }
  }

  "immediate execution" should {

    "list tasks" in {
      timer.cancel()
      i = 0
      val task1 = createTask1
      val task2 = createTask2

      timer.schedule(task1, _ => ())
      timer.schedule(task2, _ => ())
      val prelist = timer.list
      prelist should have size 2
      val name1 = prelist(0)
      name1.task.name shouldBe task1.name
      name1.isRunning shouldBe true
      name1.executionStart should be < LocalDateTime.now

      val name2 = prelist(1)
      name2.task.name shouldBe task2.name
      name2.isRunning shouldBe false
      name2.executionStart should be >= name1.executionStart
      name2.executionStart should be < LocalDateTime.now

      i shouldBe 0
      eventually(timeout2sec) {
        i shouldBe 1
        timer.list should have size 1
      }

      eventually(timeout2sec) {
        i shouldBe 4
        timer.list should have size 0
      }
    }

    "cancel tasks" in {
      timer.cancel()
      i = 0
      val task1 = createTask1
      val task2 = createTask2
      val task3 = createTask3

      timer.schedule(task1, _ => ())
      val t2 = task2
      timer.schedule(t2, _ => ())
      Thread.sleep(1)
      timer.schedule(task3, _ => ())

      val prelist = timer.list
      prelist should have size 3
      val name1 = prelist(0)
      name1.task.name shouldBe task1.name
      name1.isRunning shouldBe true
      name1.executionStart should be < LocalDateTime.now

      val name2 = prelist(1)
      name2.task.name shouldBe task2.name
      name2.isRunning shouldBe false
      name2.executionStart should be >= name1.executionStart
      name2.executionStart should be < LocalDateTime.now

      val name3 = prelist(2)
      name3.task.name shouldBe task3.name
      name3.isRunning shouldBe false
      name3.executionStart should be >= name1.executionStart
      name3.executionStart should be < LocalDateTime.now

      i shouldBe 0
      timer.cancel(task2.id)
      eventually(timeout2sec) {
        i shouldBe 1
      }
      val postlist = timer.list
      postlist should have size 1
      val postname1 = postlist(0)
      postname1.task.name shouldBe task3.name
      postname1.isRunning shouldBe true
      postname1.executionStart should be >= name3.executionStart
      postname1.executionStart should be < LocalDateTime.now

      eventually(timeout2sec) {
        i shouldBe 6
      }
      timer.list should have size 0
    }

    "not cancel running tasks" in {
      timer.cancel()
      i = 0
      val task1 = createTask1
      val task2 = createTask2

      timer.schedule(task1, _ => ())
      timer.schedule(task2, _ => ())
      val prelist = timer.list
      prelist should have size 2
      val name1 = prelist(0)
      name1.task.name shouldBe task1.name
      name1.isRunning shouldBe true
      name1.executionStart should be < LocalDateTime.now

      val name2 = prelist(1)
      name2.task.name shouldBe task2.name
      name2.isRunning shouldBe false
      name2.executionStart should be >= name1.executionStart
      name2.executionStart should be < LocalDateTime.now

      i shouldBe 0
      timer.cancel(task1.id)

      val postlist = timer.list
      postlist should have size 2
      val postName1 = postlist(0)
      postName1.task.name shouldBe task1.name
      postName1.isRunning shouldBe true
      postName1.executionStart should be < LocalDateTime.now

      val postName2 = postlist(1)
      postName2.task.name shouldBe task2.name
      postName2.isRunning shouldBe false
      postName2.executionStart should be >= name1.executionStart
      postName2.executionStart should be < LocalDateTime.now

      eventually(timeout2sec) {
        i shouldBe -10
      }
      timer.list should have size 1

      eventually(timeout2sec) {
        i shouldBe -7
      }
      timer.list should have size 0
    }

    "cancel all shouldn't cancel running" in {
      timer.cancel()
      i = 0
      val task1 = createTask1
      val task2 = createTask2

      timer.schedule(task1, _ => ())
      timer.schedule(task2, _ => ())
      val prelist = timer.list
      prelist should have size 2
      val name1 = prelist(0)
      name1.task.name shouldBe task1.name
      name1.isRunning shouldBe true
      name1.executionStart should be < LocalDateTime.now

      val name2 = prelist(1)
      name2.task.name shouldBe task2.name
      name2.isRunning shouldBe false
      name2.executionStart should be >= name1.executionStart
      name2.executionStart should be < LocalDateTime.now

      i shouldBe 0
      timer.cancel()

      val postlist = timer.list
      postlist should have size 1
      val postName1 = postlist(0)
      postName1.task.name shouldBe task1.name
      postName1.isRunning shouldBe true
      postName1.executionStart should be < LocalDateTime.now

      eventually(timeout2sec) {
        i shouldBe -10
      }
      timer.list should have size 0
    }
  }

  "scheduling using time" should {
    "set future time, schedule in order" in {
      timer.cancel()
      i = 0
      val task1 = createTask1
      val task1time = ZonedDateTime.of(LocalDateTime.now.plusDays(1), ZoneId.systemDefault)
      val task2 = createTask2
      val task2time = ZonedDateTime.of(LocalDateTime.now.plusMinutes(5), ZoneId.systemDefault)

      timer.schedule(task1, task1time, _ => ())
      timer.schedule(task2, task2time, _ => ())
      val prelist = timer.list
      prelist should have size 2
      val name1 = prelist(0)
      name1.task.name shouldBe task2.name
      name1.isRunning shouldBe false
      Duration.between(name1.executionStart, task2time).toSeconds shouldBe 0

      val name2 = prelist(1)
      name2.task.name shouldBe task1.name
      name2.isRunning shouldBe false
      Duration.between(name2.executionStart, task1time).toSeconds shouldBe 0

      i shouldBe 0
      timer.cancel()
      timer.list should have size 0
    }
    //    "adjust zones" in {
    //      timer.cancel()
    //      i = 0
    //      val task1 = createTask1
    //      val task1time = ZonedDateTime.now(ZoneId.ofOffset("", ZoneOffset.ofHours(1))).plusHours(3)
    //      val task2 = createTask2
    //      val task2time = ZonedDateTime.now(ZoneId.ofOffset("", ZoneOffset.ofHours(9))).plusHours(5)
    //
    //      timer.schedule(task1, task1time)
    //      timer.schedule(task2, task2time)
    //      val preZoneId = ZoneId.systemDefault()
    //      val prelist = timer.list(preZoneId)
    //      prelist should have size 2
    //      val name1 = prelist(0)
    //      name1.task.name shouldBe task1.name
    //      name1.isRunning shouldBe false
    //      Duration.between(name1.executionStart, task1time).toSeconds shouldBe 0
    //      name1.executionStart.getZone shouldBe preZoneId
    //
    //      val name2 = prelist(1)
    //      name2.task.name shouldBe task2.name
    //      name2.isRunning shouldBe false
    //      Duration.between(name2.executionStart, task2time).toSeconds shouldBe 0
    //      name2.executionStart.getZone shouldBe preZoneId
    //
    //      val postZoneId = ZoneOffset.ofHours(9)
    //      val postlist = timer.list(ZoneId.ofOffset("", postZoneId))
    //      postlist should have size 2
    //      val postName1 = postlist(0)
    //      postName1.task.name shouldBe task1.name
    //      postName1.isRunning shouldBe false
    //      Duration.between(postName1.executionStart, task1time).toSeconds shouldBe 0
    //      postName1.executionStart.getZone shouldBe postZoneId
    //
    //      val postName2 = postlist(1)
    //      postName2.task.name shouldBe task2.name
    //      postName2.isRunning shouldBe false
    //      Duration.between(postName2.executionStart, task2time).toSeconds shouldBe 0
    //      postName2.executionStart.getZone shouldBe postZoneId
    //    }
  }

  //  "break" should {
  //    "exit task but allow more tasks to be scheduled" in {
  //      timer.cancel()
  //      i = 0
  //      val task1 = new NamedTask("name1") {
  //        override def run(logger: Logger, break: => Nothing): Unit = {
  //          (0 to 10).foreach { _ =>
  //            if (isCancelled) break
  //            else Thread.sleep(20)
  //          }
  //          i += 1
  //        }
  //      }
  //      val task2 = new NamedTask("name2") {
  //        override def run(logger: Logger, break: => Nothing): Unit = {
  //          (0 to 10).foreach { _ =>
  //            if (isCancelled) break
  //            else Thread.sleep(20)
  //          }
  //          i += 3
  //        }
  //      }
  //
  //      timer.schedule(task1)
  //      timer.schedule(task2)
  //      val prelist = timer.list
  //      prelist should have size 2
  //
  //      i shouldBe 0
  //      timer.cancel(task1.uuid)
  //      Thread.sleep(100)
  //      i shouldBe 0
  //
  //      val postlist = timer.list
  //      postlist should have size 1
  //      val name1 = postlist(0)
  //      name1.task.name shouldBe task2.name
  //      name1.isRunning shouldBe true
  //      name1.executionStart should be < LocalDateTime.now
  //
  //      eventually(timeout2sec) {
  //        i shouldBe 3
  //      }
  //      timer.list should have size 0
  //    }
  //  }

}
