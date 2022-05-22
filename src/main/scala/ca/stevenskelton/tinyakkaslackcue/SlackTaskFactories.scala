package ca.stevenskelton.tinyakkaslackcue

import akka.Done
import akka.actor.ActorSystem
import ca.stevenskelton.tinyakkaslackcue.blocks.{PrivateMetadata, TaskHistory}
import com.typesafe.config.Config
import org.slf4j.Logger

import scala.collection.SortedSet
import scala.util.{Failure, Success, Try}

abstract class SlackTaskFactories(
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

  val tinySlackCue = new TinySlackCue(slackClient, logger, onComplete)(actorSystem, config)

//  private implicit val materializer = SystemMaterializer.get(actorSystem)
  def factories: Seq[SlackTaskFactory]

  implicit val ordering = new Ordering[InteractiveJavaUtilTimer[SlackTask]#ScheduledTask] {
    override def compare(x: InteractiveJavaUtilTimer[SlackTask]#ScheduledTask, y: InteractiveJavaUtilTimer[SlackTask]#ScheduledTask): Int = x.executionStart.compareTo(y.executionStart)
  }

  def history: Seq[TaskHistory] = {
    val allQueuedTasks = tinySlackCue.listScheduledTasks
    factories.map {
      slackTaskFactory =>
        //TODO: read Slack thread

        var runningTask: Option[InteractiveJavaUtilTimer[SlackTask]#ScheduledTask] = None
        val cueTasks = allQueuedTasks.withFilter(_.task.name == slackTaskFactory.name).flatMap {
          scheduleTask =>
            if(scheduleTask.isRunning){
              runningTask = Some(scheduleTask)
              None
            }else {
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
