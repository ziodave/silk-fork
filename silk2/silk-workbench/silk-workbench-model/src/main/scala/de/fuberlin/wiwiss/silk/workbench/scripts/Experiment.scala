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

package de.fuberlin.wiwiss.silk.workbench.scripts

import de.fuberlin.wiwiss.silk.learning.LearningConfiguration
import de.fuberlin.wiwiss.silk.learning.LearningConfiguration.{Parameters, Components}
import de.fuberlin.wiwiss.silk.workbench.scripts.PerformanceMetric.FixedIterationsFMeasure._
import de.fuberlin.wiwiss.silk.workbench.scripts.PerformanceMetric.FixedIterationsFMeasure
import de.fuberlin.wiwiss.silk.learning.individual.fitness.{FMeasureFitness, MCCFitnessFunction}

case class Experiment(name: String, configurations: Seq[LearningConfiguration], metrics: Seq[PerformanceMetric])

object Experiment {

  private val defaultConfig = LearningConfiguration("Our Approach") :: Nil

  val default = Experiment("Default", defaultConfig, Nil)

  val seeding =
    Experiment("Seeding",
      configurations =
        LearningConfiguration("No Seeding",   components = Components(seed = false), params = Parameters(maxIterations = 10)) ::
        LearningConfiguration("Our Approach", components = Components(seed = true),  params = Parameters(maxIterations = 10)) :: Nil,
      metrics =
        FixedIterationsFMeasure(0) :: FixedIterationsFMeasure(10) :: Nil
    )

  val transformations =
    Experiment("Transformations",
      configurations =
        LearningConfiguration("No Transf.", components = Components(transformations = false), params = Parameters(maxIterations = 10)) ::
        LearningConfiguration("With Transf.", components = Components(transformations = true), params = Parameters(maxIterations = 10)) :: Nil,
      metrics =
        FixedIterationsFMeasure(0) :: FixedIterationsFMeasure(10) :: Nil
    )

  val crossover =
    Experiment("Crossover Operators",
      configurations =
        LearningConfiguration("Subtree Crossover", components = Components(useSpecializedCrossover = false), params = Parameters(maxIterations = 10)) ::
        LearningConfiguration("Our Approach", components = Components(useSpecializedCrossover = true), params = Parameters(maxIterations = 10)) :: Nil,
      metrics =
        FixedIterationsFMeasure(10) :: Nil
    )

  val fitness =
    Experiment("Fitness Functions",
      configurations =
        LearningConfiguration("F-measure", fitnessFunction = MCCFitnessFunction(0.0), params = Parameters(maxIterations = 10)) ::
        LearningConfiguration("MCC", fitnessFunction = FMeasureFitness(), params = Parameters(maxIterations = 10)) :: Nil,
      metrics =
        FixedIterationsFMeasure(10) :: Nil
    )

  //TODO
//  val bloating =
//    Experiment(
//      name = "Bloating",
//      configurations = LearningConfiguration("None",     params = Parameters(cleanFrequency = Int.MaxValue), fitnessFunction = MCCFitnessFunction(0.0))   ::
//                       LearningConfiguration("Penalty",  params = Parameters(cleanFrequency = Int.MaxValue), fitnessFunction = MCCFitnessFunction(0.005)) ::
//                       LearningConfiguration("Cleaning", params = Parameters(cleanFrequency = 5),            fitnessFunction = MCCFitnessFunction(0.0))   ::
//                       LearningConfiguration("Combined", params = Parameters(cleanFrequency = 5),            fitnessFunction = MCCFitnessFunction(0.005)) :: Nil
//    )
}