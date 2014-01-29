package models

import java.io.File
import play.api.Play
import play.api.Play.current
import de.fuberlin.wiwiss.silk.runtime.resource._
import models.WorkbenchConfig.Tabs

/**
 * Workbench configuration.
 *
 * @param title The application title.
 * @param logo The application logo. Must point to a file in the conf directory.
 * @param welcome Welcome message. Must point to a file in the conf directory.
 * @param tabs The shown tabs.
 */
case class WorkbenchConfig(title: String = "Silk Workbench",
                           logo: Resource,
                           welcome: Resource,
                           about: Resource,
                           tabs: Tabs = Tabs()) {
}

object WorkbenchConfig {
  /**
   * Retrieves the Workbench configuration.
   */
  lazy val get = {
    val config = Play.configuration
    val resourceLoader = getResourceLoader

    WorkbenchConfig(
      title = config.getString("workbench.title").getOrElse("Silk Workbench"),
      logo = resourceLoader.get(config.getString("workbench.logo").getOrElse("logo.png")),
      welcome = resourceLoader.get(config.getString("workbench.welcome").getOrElse("welcome.html")),
      about = resourceLoader.get(config.getString("workbench.about").getOrElse("about.html")),
      tabs = Tabs(
               config.getBoolean("workbench.tabs.editor").getOrElse(true),
               config.getBoolean("workbench.tabs.generateLinks").getOrElse(true),
               config.getBoolean("workbench.tabs.learn").getOrElse(true),
               config.getBoolean("workbench.tabs.referenceLinks").getOrElse(true),
               config.getBoolean("workbench.tabs.status").getOrElse(true)
             )
    )
  }

  def getResourceLoader: ResourceLoader = {
    //Depending on the distribution method, the configuration resources may be located at different locations.
    //We identify the configuration location by searching for the application configuration.
    if(new File("conf/application.conf").exists())
      new FileResourceManager(new File("conf/"))
    else if(new File("../conf/application.conf").exists())
      new FileResourceManager(new File("../conf/"))
    else if(getClass.getClassLoader.getResourceAsStream("application.conf") != null)
      new ClasspathResourceLoader("")
    else
      throw new ResourceNotFoundException("Could not locate configuration.")
  }

  /**
   * Controls which tabs are shown.
   */
  case class Tabs(editor: Boolean = true,
                  generateLinks: Boolean = true,
                  learn: Boolean = true,
                  referenceLinks:Boolean = true,
                  status: Boolean = true)
}
