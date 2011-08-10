package de.fuberlin.wiwiss.silk.learning.individual

import de.fuberlin.wiwiss.silk.linkspec.input.{TransformInput, Transformer}

case class TransformNode(isSource: Boolean, inputs: List[InputNode], transformer: StrategyNode[Transformer]) extends InputNode {
  override val children = transformer :: inputs

  override def updateChildren(children: List[Node]) = {
    val newInputs = children.collect {
      case c: InputNode => c
    }
    val newTransformer = children.collect {
      case c: StrategyNode[Transformer] => c
    }.head

    TransformNode(isSource, newInputs, newTransformer)
  }

  def build = {
    TransformInput(
      inputs = inputs.map(_.build),
      transformer = transformer.build()
    )
  }
}

object TransformNode {
  def load(input: TransformInput, isSource: Boolean) = {
    val inputNodes = input.inputs.map(i => InputNode.load(i, isSource)).toList
    val transformerNode = StrategyNode.load(input.transformer, Transformer)

    TransformNode(isSource, inputNodes, transformerNode)
  }
}