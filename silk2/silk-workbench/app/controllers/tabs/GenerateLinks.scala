package controllers.tabs

import de.fuberlin.wiwiss.silk.workspace.User
import de.fuberlin.wiwiss.silk.linkagerule.evaluation.DetailedEvaluator
import de.fuberlin.wiwiss.silk.workbench.evaluation.EvalLink
import de.fuberlin.wiwiss.silk.workbench.evaluation.EvalLink.{Unknown, Incorrect, Generated, Correct}
import controllers.util.{Stream, Widgets}
import play.api.mvc.{Controller, Action}
import models.{LinkSorter, CurrentGenerateLinksTask}

object GenerateLinks extends Controller {

  def generateLinks(projectName: String, taskName: String) = Action {
    Ok(views.html.generateLinks.generateLinks(projectName, taskName))
  }

  def generateLinksDialog(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val outputs = project.outputModule.tasks.toSeq.map(_.name.toString())

    Ok(views.html.generateLinks.generateLinksDialog(projectName, taskName, outputs))
  }

  def links(projectName: String, taskName: String, sorting: String, filter: String, page: Int) = Action {
    val project = User().workspace.project(projectName)
    val task = project.linkingModule.task(taskName)
    val referenceLinks = task.referenceLinks
    val linkSorter = LinkSorter.fromId(sorting)

    def links =
      for (link <- models.CurrentGenerateLinksTask().links.view;
           detailedLink <- DetailedEvaluator(task.linkSpec.rule, link.entities.get)) yield {
        if (referenceLinks.positive.contains(link))
          new EvalLink(detailedLink, Correct, Generated)
        else if (referenceLinks.negative.contains(link))
          new EvalLink(detailedLink, Incorrect, Generated)
        else
          new EvalLink(detailedLink, Unknown, Generated)
      }

    Ok(views.html.widgets.linksTable(project, task, links, linkSorter, filter, page, showStatus = false, showDetails = true, showEntities = false, rateButtons = true))
  }

  def linksStream(projectName: String, taskName: String) = Action {
    val stream = Stream.currentTaskValue(CurrentGenerateLinksTask)
    Ok.stream(Widgets.autoReload("updateLinks", stream))
  }

  def statusStream(project: String, task: String) = Action {
    val stream = Stream.currentTaskStatus(CurrentGenerateLinksTask)
    Ok.stream(Widgets.taskStatus(stream))
  }

}
