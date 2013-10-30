import play.api.{Application, GlobalSettings}
import play.api.mvc.RequestHeader
import play.api.mvc.Results._
import java.util.logging.{SimpleFormatter, FileHandler}
import de.fuberlin.wiwiss.silk.workspace.FileUser
import de.fuberlin.wiwiss.silk.workspace.User
import de.fuberlin.wiwiss.silk.plugins.Plugins
import de.fuberlin.wiwiss.silk.plugins.jena.JenaPlugins
import scala.concurrent.Future

object Global extends GlobalSettings {

  override def beforeStart(app: Application) {
    // Configure logging
    val fileHandler = new FileHandler(app.getFile("/logs/engine.log").getAbsolutePath)
    fileHandler.setFormatter(new SimpleFormatter())
    java.util.logging.Logger.getLogger("de.fuberlin.wiwiss.silk").addHandler(fileHandler)

    //Initialize user manager
    val user = new FileUser
    User.userManager = () => user

    //Load plugins
    Plugins.register()
    JenaPlugins.register()
  }
  
  override def onError(request: RequestHeader, ex: Throwable) = {
    Future.successful(InternalServerError(ex.getMessage))
  }

}