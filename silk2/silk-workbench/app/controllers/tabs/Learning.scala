package controllers.tabs

import play.api.mvc.Controller
import play.api.mvc.Action
import de.fuberlin.wiwiss.silk.workspace.User
import de.fuberlin.wiwiss.silk.util.Identifier._
import models.EvalLink
import models.EvalLink.{Unknown, Incorrect, Generated, Correct}
import controllers.util.{Stream, Widgets}
import models._
import de.fuberlin.wiwiss.silk.util.task.{TaskFinished, TaskStatus}

object Learning extends Controller {

  def start(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.linkingModule.task(taskName)
    val referenceLinks = task.referenceLinks

    Ok(views.html.learning.start(projectName, taskName, referenceLinks))
  }

  def learn(project: String, task: String) = Action {
    Ok(views.html.learning.learn(project, task))
  }

  def activeLearn(project: String, task: String) = Action {
    Ok(views.html.learning.activeLearn(project, task))
  }

  def rule(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.linkingModule.task(taskName)
    val referenceLinks = task.referenceLinks
    val population = CurrentPopulation()

    Ok(views.html.learning.rule(population, referenceLinks))
  }

  def ruleStream(projectName: String, taskName: String) = Action {
    val stream = Stream.taskData(CurrentPopulation)
    Ok.stream(Widgets.autoReload("reload", stream))
  }

  def links(projectName: String, taskName: String, sorting: String, filter: String, page: Int) = Action {
    val project = User().workspace.project(projectName)
    val task = project.linkingModule.task(taskName)
    def refLinks = task.referenceLinks
    val linkSorter = LinkSorter.fromId(sorting)

    val valLinks = {
      for (link <- CurrentValidationLinks().view) yield {
        if (refLinks.positive.contains(link))
          new EvalLink(link, Correct, Generated)
        else if (refLinks.negative.contains(link))
          new EvalLink(link, Incorrect, Generated)
        else
          new EvalLink(link, Unknown, Generated)
      }
    }.sortBy(_.confidence.get.abs)

    Ok(views.html.widgets.linksTable(project, task, valLinks, linkSorter, filter, page, showStatus = true, showDetails = false, showEntities = true, rateButtons = true))
  }

  def linksStream(projectName: String, taskName: String) = Action {
    val stream = Stream.taskData(CurrentValidationLinks)
    Ok.stream(Widgets.autoReload("updateLinks", stream))
  }

  def statusStream(project: String, task: String) = Action {
    val stream1 = Stream.currentTaskStatus(CurrentLearningTask)
    val stream2 = Stream.currentTaskStatus(CurrentActiveLearningTask)

    Ok.stream(Widgets.taskStatus(stream1 interleave stream2))
  }

  def population(projectName: String, taskName: String) = Action {
    Ok(views.html.learning.population(projectName, taskName))
  }

  def populationView(projectName: String, taskName: String, page: Int) = Action {
    val project = User().workspace.project(projectName)
    val task = project.linkingModule.task(taskName)

    val pageSize = 20
    val individuals = CurrentPopulation().individuals.toSeq
    val sortedIndividuals = individuals.sortBy(-_.fitness)
    val pageIndividuals = sortedIndividuals.view(page * pageSize, (page + 1) * pageSize)

    Ok(views.html.learning.populationTable(projectName, taskName, pageIndividuals, task.cache.entities))
  }

  /**
   * Listens to changes of the current active learning task.
   */
  private val activeLearningTaskListener = new CurrentTaskStatusListener(CurrentActiveLearningTask) {
    override def onUpdate(status: TaskStatus) {
      status match {
        case _: TaskFinished => {
          CurrentPool() = task.pool
          CurrentPopulation() = task.population
          CurrentValidationLinks() = task.links
        }
        case _ =>
      }
    }
  }
}