package de.fuberlin.wiwiss.silk.util.sparql

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import de.fuberlin.wiwiss.silk.entity.Path
import de.fuberlin.wiwiss.silk.testutil.equalIgnoringWhitespace

@RunWith(classOf[JUnitRunner])
class SparqlPathBuilderTest extends FlatSpec with ShouldMatchers {

  // Example properties
  val p1 = "<1>"
  val p2 = "<2>"

  "SparqlPathBuilder" should "build SPARQL patterns for simple paths" in {
    build(s"?a/$p1") should be(equalIgnoringWhitespace(s"OPTIONAL { ?s $p1 ?v0 . }"))
    build(s"?a\\$p1") should be(equalIgnoringWhitespace(s"OPTIONAL { ?v0 $p1 ?s . }"))
  }

  "SparqlPathBuilder" should "include Filter statements" in {
    build(s"?a/<1>[<2> = <3>]") should be(equalIgnoringWhitespace("OPTIONAL { ?s <1> ?v0 . ?v0 <2> ?f1 . FILTER(?f1 = <3>). }"))
  }

  def build(path: String) = SparqlPathBuilder(Seq(Path.parse(path)))

}
