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

package de.fuberlin.wiwiss.silk.learning.reproduction

import de.fuberlin.wiwiss.silk.util.task.Task
import de.fuberlin.wiwiss.silk.learning.individual.Population
import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule
import de.fuberlin.wiwiss.silk.learning.generation.LinkageRuleGenerator
import de.fuberlin.wiwiss.silk.learning.LearningConfiguration

/**
 * Randomizes the population by mutating its individuals.
 */
class RandomizeTask(population: Population,
                    fitnessFunction: (LinkageRule => Double),
                    generator: LinkageRuleGenerator,
                    config: LearningConfiguration) extends Task[Population] {

  private val mutation = new MutationFunction(new CrossoverFunction(fitnessFunction, config.components), generator)

  override def execute(): Population = {
    Population(population.individuals.par.map(mutation).seq)
  }
}