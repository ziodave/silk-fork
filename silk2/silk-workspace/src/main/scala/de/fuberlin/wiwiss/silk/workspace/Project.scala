/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.workspace

import modules.linking.LinkingModule
import modules.output.OutputModule
import modules.source.SourceModule
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.runtime.resource.{ResourceManager, ResourceLoader}
import de.fuberlin.wiwiss.silk.workspace.modules.transform.TransformModule

trait Project {
  /**
   * The name of this project
   */
  val name : Identifier

  /**
   * Retrieves the project configuration.
   */
  def config : ProjectConfig

  /**
   * Updates the project configuration.
   */
  def config_=(config : ProjectConfig)

  /**
   * The source module, which encapsulates all data sources.
   */
  def sourceModule : SourceModule

  /**
   * The linking module, which encapsulates all linking tasks.
   */
  def linkingModule : LinkingModule

  /**
   * The transform module, which encapsulates all transform tasks.
   */
  def transformModule: TransformModule

  /**
   * The output module, which encapsulates all outputs.
   */
  def outputModule : OutputModule

  /**
   * For loading and writing resources.
   */
  def resourceManager: ResourceManager
}
