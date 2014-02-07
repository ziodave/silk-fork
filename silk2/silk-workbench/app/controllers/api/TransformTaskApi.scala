package controllers.api

import play.api.mvc.{Action, Controller}
import de.fuberlin.wiwiss.silk.workspace.User
import java.util.logging.{Logger, Level}
import play.api.libs.json.{JsArray, JsString, JsObject}
import de.fuberlin.wiwiss.silk.linkagerule.TransformRule
import de.fuberlin.wiwiss.silk.util.{ValidationException, CollectLogs}
import de.fuberlin.wiwiss.silk.util.ValidationException.ValidationError

object TransformTaskApi extends Controller {

  private val log = Logger.getLogger(getClass.getName)

  def getRule(projectName: String, taskName: String) = Action {
    val project = User().workspace.project(projectName)
    val task = project.transformModule.task(taskName)
    implicit val prefixes = project.config.prefixes
    val ruleXml = task.rule.toXML

    Ok(ruleXml)
  }

  def putRule(projectName: String, taskName: String) = Action { request => {
    val project = User().workspace.project(projectName)
    val task = project.transformModule.task(taskName)
    implicit val prefixes = project.config.prefixes

    request.body.asXml match {
      case Some(xml) =>
        try {
          //Collect warnings while parsing transformation rule
          val warnings = CollectLogs(Level.WARNING, "de.fuberlin.wiwiss.silk.linkagerule") {
            // Currently the editor always uses the Tag <LinkageRule> so we need to replace it
            val ruleXml = <TransformRule>{xml.head.child}</TransformRule>
            //Load transformation rule
            val updatedRule = TransformRule.load(project.resourceManager)(prefixes)(ruleXml)
            //Update linking task
            val updatedTask = task.updateRule(updatedRule, project)
            project.transformModule.update(updatedTask)
          }
          // Return warnings
          Ok(statusJson(warnings = warnings.map(_.getMessage)))
        } catch {
          case ex: ValidationException =>
            log.log(Level.INFO, "Invalid transformation rule")
            BadRequest(statusJson(errors = ex.errors))
          case ex: Exception =>
            log.log(Level.INFO, "Failed to save transformation rule", ex)
            InternalServerError(statusJson(errors = ValidationError("Error in back end: " + ex.getMessage) :: Nil))
        }
      case None =>
        BadRequest("Expecting text/xml request body")
    }
  }}

  private def statusJson(errors: Seq[ValidationError] = Nil, warnings: Seq[String] = Nil, infos: Seq[String] = Nil) = {
    /**Generates a Json expression from an error */
    def errorToJsExp(error: ValidationError) = JsObject(("message", JsString(error.toString)) :: ("id", JsString(error.id.map(_.toString).getOrElse(""))) :: Nil)

    JsObject(
      ("error", JsArray(errors.map(errorToJsExp))) ::
          ("warning", JsArray(warnings.map(JsString(_)))) ::
          ("info", JsArray(infos.map(JsString(_)))) :: Nil
    )
  }

}
