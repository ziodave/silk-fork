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

package de.fuberlin.wiwiss.silk.plugins.transformer.combine

import de.fuberlin.wiwiss.silk.linkagerule.input.Transformer
import de.fuberlin.wiwiss.silk.util.plugin.Plugin

@Plugin(
  id = "concat",
  categories = Array("Combine"),
  label = "Concatenate",
  description = "Concatenates strings from two inputs."
)
case class ConcatTransformer(glue: String = "") extends Transformer {
  override def apply(values: Seq[Set[String]]): Set[String] = {
    for (sequence <- cartesianProduct(values)) yield evaluate(sequence)
  }

  private def cartesianProduct(strings: Seq[Set[String]]): Set[List[String]] = {
    if (strings.tail.isEmpty) for (string <- strings.head) yield string :: Nil
    else for (string <- strings.head; seq <- cartesianProduct(strings.tail)) yield string :: seq
  }

  private def evaluate(strings: Seq[String]) = {
    (strings.head /: strings.tail)(_ + glue + _)
  }
}