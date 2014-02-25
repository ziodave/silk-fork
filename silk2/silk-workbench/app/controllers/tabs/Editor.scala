package controllers.tabs

import play.api.mvc.Controller
import play.api.mvc.Action
import de.fuberlin.wiwiss.silk.workspace.User
import de.fuberlin.wiwiss.silk.util.DPair
import de.fuberlin.wiwiss.silk.evaluation.LinkageRuleEvaluator

object Editor extends Controller {

  def editor(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.linkingModule.task(taskName)

    Ok(views.html.editor.editor(project, task))
  }

  def paths(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.linkingModule.task(taskName)
    val pathsCache = task.cache.pathCache
    val prefixes = project.config.prefixes

    if(pathsCache.status.isRunning) {
      val loadingMsg = f"Cache loading (${pathsCache.status.progress * 100}%.1f%%)"
      ServiceUnavailable(views.html.editor.paths(DPair.fill(Seq.empty), onlySource = false, loadingMsg = loadingMsg))
    } else if(pathsCache.status.failed) {
      Ok(views.html.editor.paths(DPair.fill(Seq.empty), onlySource = false, warning = pathsCache.status.message + " Try reloading the paths."))
    } else {
      val paths = pathsCache.value.map(_.paths.map(_.serialize(prefixes)))
      Ok(views.html.editor.paths(paths, onlySource = false))
    }
  }

  def score(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.linkingModule.task(taskName)
    val entitiesCache = task.cache.referenceEntitiesCache

    // If the entity cache is still loading
    if(entitiesCache.status.isRunning) {
      ServiceUnavailable(f"Cache loading (${entitiesCache.status.progress * 100}%.1f%%)")
    // If the cache loading failed
    } else if(entitiesCache.status.failed) {
      Ok(views.html.editor.score(
        info = "No score available",
        error = "No score available as loading the entities that are referenced by the reference links failed. " +
                "Reason: " + entitiesCache.status.message))
    // If there are no reference links
    } else if (entitiesCache.value.positive.isEmpty || entitiesCache.value.negative.isEmpty) {
      Ok(views.html.editor.score(
        info = "No score available",
        error = "No score available as this project does not define any reference links."))
    // If everything needed for computing a score is available
    } else {
      val result = LinkageRuleEvaluator(task.linkSpec.rule, entitiesCache.value)
      val score = f"Precision: ${result.precision}%.2f | Recall: ${result.recall}%.2f | F-measure: ${result.fMeasure}%.2f"
      Ok(views.html.editor.score(score))
    }
  }

}
