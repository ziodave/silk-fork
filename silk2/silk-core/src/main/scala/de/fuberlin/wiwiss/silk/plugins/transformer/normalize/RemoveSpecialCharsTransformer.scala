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

import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import de.fuberlin.wiwiss.silk.plugins.transformer.replace.RegexReplaceTransformer

@Plugin(
  id = "removeSpecialChars",
  categories = Array("Normalize"),
  label = "Remove special chars",
  description = "Remove special characters (including punctuation) from a string."
)
class RemoveSpecialCharsTransformer() extends RegexReplaceTransformer("[^\\d\\pL\\w]+", "")