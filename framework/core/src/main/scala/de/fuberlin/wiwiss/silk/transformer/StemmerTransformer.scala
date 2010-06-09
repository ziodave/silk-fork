package de.fuberlin.wiwiss.silk.transformer

import de.fuberlin.wiwiss.silk.linkspec.Transformer

class StemmerTransformer(val params: Map[String, String] = Map()) extends Transformer
{
    override def evaluate(strings : Seq[String]) =
    {
        val stemmer = new PorterStemmer
        stemmer.stem(strings.toList.head)
    }
}