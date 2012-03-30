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

package de.fuberlin.wiwiss.silk.entity

import xml.Node
import de.fuberlin.wiwiss.silk.config.Prefixes

case class EntityDescription(variable: String, restrictions: SparqlRestriction, paths: IndexedSeq[Path]) {
  require(paths.forall(!_.operators.isEmpty), "Entity description must not contain an empty path")

  /**
   * Retrieves the index of a given path.
   */
  def pathIndex(path: Path) = {
    var index = 0
    while(path != paths(index)) {
      index += 1
      if(index >= paths.size)
        throw new NoSuchElementException("Path " + path + " not found on entity.")
    }
    index
  }

  def merge(other: EntityDescription) = {
    require(variable == other.variable)
    require(restrictions == other.restrictions)

    copy(paths = (paths ++ other.paths).distinct)
  }

  def toXML = {
    <EntityDescription>
      <Variable>{variable}</Variable>
      <Restrictions>{restrictions.toSparql}</Restrictions>
      <Paths> {
        for (path <- paths) yield {
          <Path>{path.serialize(Prefixes.empty)}</Path>
        }
      }
      </Paths>
    </EntityDescription>
  }
}

object EntityDescription {
  def fromXML(node: Node) = {
    val variable = (node \ "Variable").text.trim
    new EntityDescription(
      variable = variable,
      restrictions = SparqlRestriction.fromSparql(variable, (node \ "Restrictions").text),
      paths = for (pathNode <- (node \ "Paths" \ "Path").toIndexedSeq[Node]) yield Path.parse(pathNode.text.trim)
    )
  }
}
