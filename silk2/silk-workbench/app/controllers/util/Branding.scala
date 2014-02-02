package controllers.util

import play.api.mvc.{Action, Controller}
import models.WorkbenchConfig
import play.api.templates.Html

object Branding extends Controller {

  def logo = Action {
    val imgStream = WorkbenchConfig.get.logo.load
    val bytes = scala.Stream.continually(imgStream.read).takeWhile(_ != -1).map(_.toByte).toArray
    Ok(bytes).as("image/png")
  }

  def aboutDialog = Action {
    val aboutHtml = Html(WorkbenchConfig.get.about.loadAsString)
    Ok(views.html.aboutDialog(aboutHtml))
  }

}
