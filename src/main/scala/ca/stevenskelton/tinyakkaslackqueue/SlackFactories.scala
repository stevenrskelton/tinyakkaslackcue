package ca.stevenskelton.tinyakkaslackqueue

import akka.Done
import akka.actor.ActorSystem
import ca.stevenskelton.tinyakkaslackqueue.blocks.{PrivateMetadata, TaskHistory}
import com.typesafe.config.Config
import org.slf4j.Logger

import scala.collection.SortedSet
import scala.util.{Failure, Success, Try}

abstract class SlackFactories(
                                   val slackClient: SlackClient,
                                   val logger: Logger,
                                   val actorSystem: ActorSystem,
                                   val config: Config
                                 ) {

  def onComplete(slackTask: SlackTask, result: Try[Done]): Unit = {
    result match {
      case Failure(ex) =>
      case Success(Done) =>
    }
  }

  val tinySlackQueue = new TinySlackQueue(slackClient, logger, onComplete)(actorSystem, config)

  //  private implicit val materializer = SystemMaterializer.get(actorSystem)
  def factories: Seq[SlackTaskFactory]

  implicit val ordering = new Ordering[InteractiveJavaUtilTimer[SlackTs, SlackTask]#ScheduledTask] {
    override def compare(x: InteractiveJavaUtilTimer[SlackTs,SlackTask]#ScheduledTask, y: InteractiveJavaUtilTimer[SlackTs,SlackTask]#ScheduledTask): Int = x.executionStart.compareTo(y.executionStart)
  }

  def history: Seq[TaskHistory] = {
    val allQueuedTasks = tinySlackQueue.listScheduledTasks
    factories.map {
      slackTaskFactory =>
        //TODO: read Slack thread

        var runningTask: Option[InteractiveJavaUtilTimer[SlackTs,SlackTask]#ScheduledTask] = None
        val cueTasks = allQueuedTasks.withFilter(_.task.name == slackTaskFactory.name).flatMap {
          scheduleTask =>
            if (scheduleTask.isRunning) {
              runningTask = Some(scheduleTask)
              None
            } else {
              Some(scheduleTask)
            }
        }
        TaskHistory(slackTaskFactory, runningTask, executed = SortedSet.empty, pending = SortedSet.from(cueTasks))
    }
  }

  def findByName(name: String): Option[SlackTaskFactory] = {
    factories.find(_.name == name)
  }

  def findByPrivateMetadata(privateMetadata: PrivateMetadata): Option[SlackTaskFactory] = {
    factories.find(o => PrivateMetadata(o.name) == privateMetadata)
  }

}
