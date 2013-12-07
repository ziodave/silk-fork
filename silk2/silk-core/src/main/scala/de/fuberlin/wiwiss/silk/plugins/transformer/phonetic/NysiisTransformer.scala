package de.fuberlin.wiwiss.silk.plugins.transformer.phonetic

import de.fuberlin.wiwiss.silk.util.plugin.Plugin
import com.rockymadden.stringmetric.phonetic.{RefinedNysiisAlgorithm, NysiisAlgorithm}
import de.fuberlin.wiwiss.silk.linkagerule.input.SimpleTransformer

@Plugin(
  id = "NYSIIS",
  categories = Array("Phonetic"),
  label = "NYSIIS",
  description = "NYSIIS phonetic encoding. Provided by the StringMetric library: http://rockymadden.com/stringmetric/"
)
case class NysiisTransformer(refined: Boolean = true) extends SimpleTransformer {

  override def evaluate(value: String) = {
    if(refined)
      RefinedNysiisAlgorithm.compute(value).getOrElse("")
    else
      NysiisAlgorithm.compute(value).getOrElse("")
  }
}
