package de.fuberlin.wiwiss.silk.linkspec.metric

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import de.fuberlin.wiwiss.silk.metric.LevenshteinMetric

class LevenshteinMetricTest extends FlatSpec with ShouldMatchers
{
    val metric = new LevenshteinMetric()

    "LevenshteinMetric" should "return distance 3 (kitten, sitting)" in
    {
        metric.evaluateDistance("kitten", "sitting") should equal (3)
        metric.evaluateDistance("sitting", "kitten") should equal (3)
    }

    "LevenshteinMetric" should "return distance 3 (Saturday, Sunday)" in
    {
        metric.evaluateDistance("Saturday", "Sunday") should equal (3)
        metric.evaluateDistance("Sunday", "Saturday") should equal (3)
    }


/*         * StringUtils.getLevenshteinDistance("","")               = 0
     * StringUtils.getLevenshteinDistance("","a")              = 1
     * StringUtils.getLevenshteinDistance("aaapppp", "")       = 7
     * StringUtils.getLevenshteinDistance("frog", "fog")       = 1
     * StringUtils.getLevenshteinDistance("fly", "ant")        = 3
     * StringUtils.getLevenshteinDistance("elephant", "hippo") = 7
     * StringUtils.getLevenshteinDistance("hippo", "elephant") = 7
     * StringUtils.getLevenshteinDistance("hippo", "zzzzzzzz") = 8
     * StringUtils.getLevenshteinDistance("hello", "hallo")    = 1*/
}