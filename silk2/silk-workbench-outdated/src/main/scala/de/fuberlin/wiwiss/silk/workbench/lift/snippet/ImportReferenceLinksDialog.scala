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

import xml.NodeSeq
import java.io.ByteArrayInputStream
import io.Source
import de.fuberlin.wiwiss.silk.workspace.User
import net.liftweb.http.{SHtml, FileParamHolder}
import net.liftweb.util.Helpers._
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.JsCmds.OnLoad
import de.fuberlin.wiwiss.silk.evaluation.{ReferenceLinks, ReferenceLinksReader}
import de.fuberlin.wiwiss.silk.config.Dataset
import de.fuberlin.wiwiss.silk.entity.{Link, Path, EntityDescription}

class ImportReferenceLinksDialog {

  def render(xhtml: NodeSeq): NodeSeq = {
    var fileHolder: FileParamHolder = null

    bind("entry", xhtml,
         "file" -> SHtml.fileUpload(fileHolder = _),
         "submit" -> SHtml.submit("Open", () => loadFromFile(fileHolder), "style" -> "float:right;"))
  }

  private def loadFromFile(fileHolder: FileParamHolder) = fileHolder match {
    case FileParamHolder(_, _, fileName, data) => {
      val referenceLinks = fileName.split('.').last match {
        case "xml" | "rdf" => ReferenceLinksReader.readReferenceLinks(new ByteArrayInputStream(data))
        case "nt" => ReferenceLinksReader.readNTriples(Source.fromBytes(data))
        case ext => throw new IllegalArgumentException("Unsupported file extension: '" + ext + "'.")
      }

      updateReferenceLinks(referenceLinks)
    }
    case _ =>
  }

  private def loadFromSources()  {
    val datasets = User().linkingTask.linkSpec.datasets

    val sourceEntities = getEntities(datasets.source).toList
    val targetEntities = getEntities(datasets.target).toList

    val sourceUris = sourceEntities.map(_.uri).toSet
    val targetUris = targetEntities.map(_.uri).toSet

    val sourceLinks = sourceEntities.flatMap(i => i.evaluate(0).map(new Link(i.uri, _)))
                                     .filter(link => targetUris.contains(link.target))

    val targetLinks = targetEntities.flatMap(i => i.evaluate(0).map(new Link(_, i.uri)))
                                     .filter(link => sourceUris.contains(link.source))

    val links = sourceLinks ++ targetLinks

    updateReferenceLinks(ReferenceLinks(links.toSet))
  }

  private def getEntities(dataset: Dataset, uris: Seq[String] = Seq.empty) = {
    val source = User().project.sourceModule.task(dataset.sourceId).source
    val entityDesc =
      EntityDescription(
        variable = dataset.variable,
        restrictions = dataset.restriction,
        paths = IndexedSeq(Path.parse("?" + dataset.variable + "/<http://www.w3.org/2002/07/owl#sameAs>"))
      )

    source.retrieve(entityDesc, uris)
  }

  private def updateReferenceLinks(referenceLinks: ReferenceLinks) {
    //No negative links loaded -> generate some
    if (referenceLinks.negative.isEmpty) {
      val updatedLinkingTask = User().linkingTask.updateReferenceLinks(referenceLinks.generateNegative, User().project)

      User().project.linkingModule.update(updatedLinkingTask)
      User().task = updatedLinkingTask
    }
    else {
      val updatedLinkingTask = User().linkingTask.updateReferenceLinks(referenceLinks, User().project)
      User().project.linkingModule.update(updatedLinkingTask)
      User().task = updatedLinkingTask
    }
  }
}

object ImportReferenceLinksDialog {
  def initCmd = OnLoad(JsRaw("$('#importReferenceLinksDialog').dialog({ autoOpen: false, width: 500, modal: true })").cmd)

  def openCmd = JsRaw("$('#importReferenceLinksDialog').dialog('open')").cmd
}