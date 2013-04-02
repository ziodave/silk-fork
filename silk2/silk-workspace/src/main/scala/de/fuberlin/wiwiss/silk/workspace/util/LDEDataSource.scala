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

package de.fuberlin.wiwiss.silk.workspace.util

import de.fuberlin.wiwiss.silk.plugins.datasource.SparqlDataSource
import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.entity.{SparqlRestriction, Path}

@Plugin(id = "LDEsparqlEndpoint", label = "LDE SPARQL Endpoint", description = "DataSource in the LDE context")
class LDEDataSource(endpointURI : String) extends SparqlDataSource(endpointURI){

  override def retrievePaths(restrictions : SparqlRestriction, depth : Int, limit : Option[Int]) : Traversable[(Path, Double)] =
  {
    LDEPathsCollector(createEndpoint(), restrictions, limit)
  }
}