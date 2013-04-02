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

package de.fuberlin.wiwiss.silk.workbench.lift.snippet

import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.workspace.User
import de.fuberlin.wiwiss.silk.workbench.evaluation.CurrentGenerateLinksTask
import de.fuberlin.wiwiss.silk.workbench.lift.util.{JS, SelectField, Dialog}
import de.fuberlin.wiwiss.silk.config.RuntimeConfig
import de.fuberlin.wiwiss.silk.execution.GenerateLinksTask

object GenerateLinksDialog extends Dialog {

  override val title = "Generate Links"

  private val noOutputName = "Display only"

  private val output = SelectField("Output", "The output where the generated links are written", () => noOutputName :: User().project.outputModule.tasks.map(_.name.toString).toList, () => noOutputName)

  override val fields = output :: Nil

  override protected def dialogParams = ("autoOpen" -> "false") :: ("width" -> "400") :: ("modal" -> "true") :: Nil

  private val logger = Logger.getLogger(getClass.getName)

  /** We use a custom runtime config */
  private val runtimeConfig = RuntimeConfig(useFileCache = false, partitionSize = 300, generateLinksWithEntities = true)

  override protected def onSubmit() = {
    val generateLinksTask =
      new GenerateLinksTask(
        sources = User().project.sourceModule.tasks.map(_.source),
        linkSpec = User().linkingTask.linkSpec,
        outputs = if(output.value == noOutputName) Traversable.empty else Traversable(User().project.outputModule.task(output.value).output),
        runtimeConfig = runtimeConfig
      )

    CurrentGenerateLinksTask() = generateLinksTask
    generateLinksTask.runInBackground()

    JS.Empty
  }
}