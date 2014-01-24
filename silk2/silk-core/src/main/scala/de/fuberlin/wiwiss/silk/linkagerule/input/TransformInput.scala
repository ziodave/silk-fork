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

package de.fuberlin.wiwiss.silk.linkagerule.input

import de.fuberlin.wiwiss.silk.entity.Entity
import de.fuberlin.wiwiss.silk.config.Prefixes
import de.fuberlin.wiwiss.silk.linkagerule.Operator
import xml.Node
import de.fuberlin.wiwiss.silk.util.{ValidationException, Identifier, DPair}
import de.fuberlin.wiwiss.silk.util.plugin.ResourceLoader

/**
 * A TransformInput applies a transformation to input values.
 */
case class TransformInput(id: Identifier = Operator.generateId, transformer: Transformer, inputs: Seq[Input]) extends Input {
  require(inputs.size > 0, "Number of inputs must be > 0.")

  def apply(entities: DPair[Entity]): Set[String] = {
    val values = for (input <- inputs) yield input(entities)

    transformer(values)
  }

  override def toString = transformer match {
    case Transformer(name, params) => "Transformer(type=" + name + ", params=" + params + ", inputs=" + inputs + ")"
  }

  override def toXML(implicit prefixes: Prefixes) = transformer match {
    case Transformer(func, params) => {
      <TransformInput id={id} function={func}>
        { inputs.map { input => input.toXML } }
        { params.map { case (name, value) => <Param name={name} value={value}/>  } }
      </TransformInput>
    }
  }
}

object TransformInput {

  def fromXML(node: Node, resourceLoader: ResourceLoader)(implicit prefixes: Prefixes) = {
    val id = Operator.readId(node)
    val inputs = Input.fromXML(node.child, resourceLoader)
    if(inputs.isEmpty) throw new ValidationException("No input defined", id, "Transformation")

    try {
      val transformer = Transformer(node \ "@function" text, Operator.readParams(node), resourceLoader)
      TransformInput(id, transformer, inputs)
    } catch {
      case ex: Exception => throw new ValidationException(ex.getMessage, id, "Tranformation")
    }
  }
}
