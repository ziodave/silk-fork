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

package de.fuberlin.wiwiss.silk.plugins.transformer.normalize

import de.fuberlin.wiwiss.silk.runtime.plugin.Plugin
import de.fuberlin.wiwiss.silk.linkagerule.input.SimpleTransformer

@Plugin(
  id = "capitalize",
  categories = Array("Normalize"),
  label = "Capitalize",
  description = "Capitalizes the string i.e. converts the first character to upper case. " +
    "If 'allWords' is set to true, all words are capitalized and not only the first character."
)
case class CapitalizeTransformer(allWords: Boolean = false) extends SimpleTransformer {
  override def evaluate(value: String) = {
    if(allWords)
      value.capitalize
    else
      value.split("\\s+").map(_.capitalize).mkString
  }
}