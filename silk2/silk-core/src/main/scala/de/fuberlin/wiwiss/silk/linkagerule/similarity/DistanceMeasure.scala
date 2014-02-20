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

package de.fuberlin.wiwiss.silk.linkagerule.similarity

import de.fuberlin.wiwiss.silk.runtime.plugin.{AnyPlugin, PluginFactory}
import de.fuberlin.wiwiss.silk.entity.Index

trait DistanceMeasure extends AnyPlugin {

  /**
   * Computes the distance between two sets of strings.
   *
   * @param values1 The first set of strings.
   * @param values2 The second set of strings.
   * @param limit If the expected distance between both sets exceeds this limit, this method may
   *              return Double.PositiveInfinity instead of the actual distance in order to save computation time.
   * @return A positive number that denotes the computed distance between both strings.
   */
  //TODO accept set instead of traversable?
  def apply(values1: Traversable[String], values2: Traversable[String], limit: Double = Double.PositiveInfinity): Double

  def index(values: Set[String], limit: Double): Index = Index.default
}

object DistanceMeasure extends PluginFactory[DistanceMeasure]
