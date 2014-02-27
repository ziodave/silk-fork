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

package de.fuberlin.wiwiss.silk.config

import xml.Node
import de.fuberlin.wiwiss.silk.output.Output
import de.fuberlin.wiwiss.silk.util._
import java.util.logging.Logger
import de.fuberlin.wiwiss.silk.linkagerule.{LinkFilter, LinkageRule}
import de.fuberlin.wiwiss.silk.linkagerule.similarity.{Comparison, Aggregation, SimilarityOperator}
import de.fuberlin.wiwiss.silk.linkagerule.input.{TransformInput, PathInput, Input}
import de.fuberlin.wiwiss.silk.entity.{EntityDescription, Path}
import de.fuberlin.wiwiss.silk.runtime.resource.ResourceLoader

/**
 * Represents a Silk Link Specification.
 */
case class LinkSpecification(id: Identifier = Identifier.random,
                             datasets: DPair[Dataset] = DPair.fill(Dataset.empty),
                             rule: LinkageRule = LinkageRule(),
                             outputs: Traversable[Output] = Traversable.empty) {
  /**
   * Serializes this Link Specification as XML.
   */
  def toXML(implicit prefixes: Prefixes = Prefixes.empty): Node = {
    <Interlink id={id}>
      <LinkType>{rule.linkType.toTurtle}</LinkType>
      {datasets.source.toXML(true)}
      {datasets.target.toXML(false)}
      {rule.toXML}
      {rule.filter.toXML}
      <Outputs>
      {outputs.map(_.toXML)}
      </Outputs>
    </Interlink>
  }

  def entityDescriptions: DPair[EntityDescription] = {
    val sourceVar = datasets.source.variable
    val targetVar = datasets.target.variable

    val sourceRestriction = datasets.source.restriction
    val targetRestriction = datasets.target.restriction

    val sourcePaths = rule.operator match {
      case Some(operator) => collectPaths(sourceVar)(operator)
      case None => Set[Path]()
    }

    val targetPaths = rule.operator match {
      case Some(operator) => collectPaths(targetVar)(operator)
      case None => Set[Path]()
    }

    val sourceEntityDesc = new EntityDescription(sourceVar, sourceRestriction, sourcePaths.toIndexedSeq)
    val targetEntityDesc = new EntityDescription(targetVar, targetRestriction, targetPaths.toIndexedSeq)

    DPair(sourceEntityDesc, targetEntityDesc)
  }

  private def collectPaths(variable: String)(operator: SimilarityOperator): Set[Path] = operator match {
    case aggregation: Aggregation => aggregation.operators.flatMap(collectPaths(variable)).toSet
    case comparison: Comparison => {
      val sourcePaths = collectPathsFromInput(variable)(comparison.inputs.source)
      val targetPaths = collectPathsFromInput(variable)(comparison.inputs.target)
      (sourcePaths ++ targetPaths).toSet
    }
  }

  private def collectPathsFromInput(variable: String)(param: Input): Set[Path] = param match {
    case p: PathInput if p.path.variable == variable && !p.path.operators.isEmpty => Set(p.path)
    case p: TransformInput => p.inputs.flatMap(collectPathsFromInput(variable)).toSet
    case _ => Set()
  }
}

object LinkSpecification {
  private val schemaLocation = "de/fuberlin/wiwiss/silk/LinkSpecificationLanguage.xsd"

  private val logger = Logger.getLogger(LinkSpecification.getClass.getName)

  def load(resourceLoader: ResourceLoader)(implicit prefixes: Prefixes) = {
    new ValidatingXMLReader(node => fromXML(node, resourceLoader), schemaLocation)
  }

  /**
   * Reads a Link Specification from XML.
   */
  def fromXML(node: Node, resourceLoader: ResourceLoader)(implicit prefixes: Prefixes): LinkSpecification = {
    //Read id
    val id = (node \ "@id").text

    //Read linkage rule node
    val linkConditionNode = (node \ "LinkCondition").headOption
    val linkageRuleNode = (node \ "LinkageRule").headOption

    if(linkageRuleNode.isEmpty && linkConditionNode.isEmpty) throw new ValidationException("No <LinkageRule> found in link specification with id '" + id + "'")
    if(linkConditionNode.isDefined) throw new ValidationException("<LinkCondition> has been renamed to <LinkageRule>. Please update the link specification.")

    //Read filter
    val filter = LinkFilter.fromXML((node \ "Filter").head)
    implicit val globalThreshold = filter.threshold

    new LinkSpecification(
      id = id,
      datasets = new DPair(Dataset.fromXML((node \ "SourceDataset").head),
                           Dataset.fromXML((node \ "TargetDataset").head)),
      rule =
        LinkageRule.fromXML(
          node = linkageRuleNode.getOrElse(linkConditionNode.get),
          filter = filter,
          linkType = resolveQualifiedName("LinkType", (node \ "LinkType").text.trim, prefixes),
          resourceLoader = resourceLoader),
      outputs = (node \ "Outputs" \ "Output").map(Output.fromXML(_, resourceLoader))
    )
  }

  private def resolveQualifiedName(element: String, value: String, prefixes: Map[String, String]) = {
    if (value.startsWith("<") && value.endsWith(">")) {
      value.substring(1, value.length - 1)
    }
    else {
      value.split(":", 2) match {
        case Array(prefix, suffix) => prefixes.get(prefix) match {
          case Some(resolvedPrefix) => resolvedPrefix + suffix
          case None => throw new ValidationException("Unknown prefix: '" + prefix + "'")
        }
        case _ => throw new ValidationException("No prefix found in '" + value + "'", element)
      }
    }
  }
}
