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

package de.fuberlin.wiwiss.silk.execution.methods

import de.fuberlin.wiwiss.silk.plugins.metric.LevenshteinDistance
import io.Source
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.matchers.{MatchResult, BeMatcher, ShouldMatchers}

@RunWith(classOf[JUnitRunner])
class StringMapTest extends FlatSpec with ShouldMatchers {

  /** Load the source files used by the original authors from http://flamingo.ics.uci.edu/releases/4.1/ */
  val source1 = loadSource("names/source1.txt")
  val source2 = loadSource("names/source2.txt")

  /** We are using the levensthein distance metric. */
  val distanceMetric = LevenshteinDistance()

  /** Initialize the string map implementation. */
  val sm = new StringMap.Mapper(
    stringVector = source1 ++ source2,
    distanceMetric = distanceMetric,
    dimensionality = 20
  )

  "de.fuberlin.wiwiss.silk.execution.methods.StringMap" should "generate the same coordinates as the original implementation" in {
    sm.coordinates(0) should be(approximatelyEqualTo(origCoord0))
    sm.coordinates(1) should be(approximatelyEqualTo(origCoord1))
    sm.coordinates(2) should be(approximatelyEqualTo(origCoord2))
    sm.coordinates(3) should be(approximatelyEqualTo(origCoord3))
    sm.coordinates(4) should be(approximatelyEqualTo(origCoord4))
    sm.coordinates(5) should be(approximatelyEqualTo(origCoord5))
    sm.coordinates(6) should be(approximatelyEqualTo(origCoord6))
    sm.coordinates(7) should be(approximatelyEqualTo(origCoord7))
    sm.coordinates(8) should be(approximatelyEqualTo(origCoord8))
    sm.coordinates(9) should be(approximatelyEqualTo(origCoord9))
    sm.coordinates(2000) should be(approximatelyEqualTo(origCoord2000))
  }

  "de.fuberlin.wiwiss.silk.execution.methods.StringMap" should "achieve the same recall as the original implementation" in {
    computeRecall should be >= (0.9798)
  }

  /** Some coordinates generated by the original implementation. */
  val origCoord0 = Array(14.4285714285714, 11.2543514180982, 16.2389657483746, 11.5396606679504, 11.5626897899057, 8.47278097920413, 10.9002257235015, 11.0873522692474, 9.41281583264436, 9.66330680772292, 6.20841432867345, 11.5002396723136, 7.4669032421746, 6.97127584248199, 11.8382271143214, 10.6469401846028, 11.055744136774, 8.46242991485724, 7.15372917193599, 8.51333205419316)
  val origCoord1 = Array(10.5, 13.8857195206851, 14.5771529508591, 11.5210288481215, 11.0636419102576, 9.85367440287574, 12.6506365036887, 11.3822665941838, 9.46308852093747, 14.0184145795504, 9.87094241561982, 9.88587760073732, 8.85350361252383, 9.25665991991014, 10.2782931163833, 8.71264223887723, 11.911622058384, 8.84109840107858, 9.98019742762547, 6.80810355783389)
  val origCoord2 = Array(13.3571428571429, 11.3965530318695, 15.311497311715, 12.3105644280118, 10.5234467124489, 10.3028729733333, 11.2508217851795, 10.1486383094663, 8.25882240978047, 9.59925394562292, 9.39873228439803, 9.49806277355082, 8.33075484137456, 7.99909429487851, 12.8746266473845, 9.83574734463982, 9.51995209828045, 8.69957714548916, 9.27502630710471, 8.9735263164955)
  val origCoord3 = Array(14.4285714285714, 13.731263548815, 16.7052084522491, 12.2463078742272, 10.5248127214681, 11.1569509614619, 9.60917021529498, 9.52374086514791, 8.78499836071099, 11.2875654282249, 8.21050995278532, 10.6598286504538, 8.93471383977082, 8.51412076316893, 12.0735496455872, 8.72480333357776, 10.309594180548, 9.33187449296207, 8.8430403634334, 9.28925888119871)
  val origCoord4 = Array(11.9285714285714, 13.1540322225369, 15.7023551211887, 11.5000980832484, 10.5899536197229, 10.860935639615, 11.6246066109191, 9.90047066383127, 8.45563418287884, 11.088459140352, 7.96411776909371, 7.88277572638482, 9.29549067152769, 9.77253424228151, 10.0027476823219, 10.1077943103391, 6.32050826797281, 8.52878157496005, 7.48100000034585, 8.4203963046853)
  val origCoord5 = Array(12.7142857142857, 10.6962675263494, 12.0040701254216, 10.1259328433954, 9.06100575344467, 12.3309953108171, 10.5346282908936, 9.9340718465419, 7.78782877120853, 10.9606893516778, 8.67252125888597, 8.8301379231417, 9.01211855906163, 5.51832688640122, 12.3539558023723, 7.11932216764398, 10.4172508065024, 10.6637864366818, 9.2313291858286, 9.28439253338826)
  val origCoord6 = Array(14.4285714285714, 9.85326980880387, 17.5753544494926, 10.7682458871699, 12.6817971294506, 9.92223192969547, 12.236936056842, 9.49202392803204, 7.90312865055449, 12.0488827316763, 6.07047714559649, 11.1922583213138, 8.21931947232945, 8.14879403383447, 10.7870088970993, 10.6602870595656, 11.6019960283576, 8.02704266157797, 6.86121266073339, 7.47478745275714)
  val origCoord7 = Array(12.8571428571429, 14.4185287988552, 16.7494831764723, 11.2665525415578, 10.5966012710483, 11.5627587809474, 9.41195008396271, 10.8702711491571, 9.69117882393605, 11.0705489824905, 12.6837724980965, 8.97208129753507, 8.83808568527284, 8.7001419551492, 11.2933642996501, 7.82254504451718, 11.7175509841722, 10.7053842146927, 6.55122080852345, 3.86602209166051)
  val origCoord8 = Array(12.8571428571429, 9.91505219755191, 14.1565752724186, 7.51016514333448, 9.60384356192838, 10.9549404995255, 11.9451160265552, 10.6426061467047, 10.8365224777758, 11.0688072342293, 8.5745281232819, 12.970235353843, 9.40175999185482, 7.79972218895765, 12.0886573321119, 6.32350838404403, 9.6429086806224, 8.16582764546065, 10.390912727978, 9.26714527978364)
  val origCoord9 = Array(12.0238095238095, 15.0267364192164, 15.0617743792403, 12.1206542605972, 5.55278235082782, 11.3944150151342, 7.59032166994711, 11.5184958326959, 10.4827691625226, 12.7049469064549, 8.41319319224661, 12.2129864935699, 8.55850928485589, 6.20967458969635, 12.9160806169689, 8.71104376072714, 10.7750921455517, 9.80102696821875, 9.61773294484923, 9.20959719763996)
  val origCoord2000 = Array(12.1190476190476, 15.148088604278, 15.3950093693189, 12.1017712352083, 9.5167343690371, 11.9142946743942, 9.42742509299494, 9.9857424024219, 9.23970696423749, 10.5193250846191, 8.10789391863431, 11.7174491562294, 9.04153293822916, 8.45624908509521, 7.40694231364865, 8.95346105057554, 10.622666395547, 10.2660042714931, 10.9891360680569, 7.39756415396112)

  private def loadSource(name: String): Array[Set[String]] = {
    val source = Source.fromURL(getClass.getClassLoader.getResource(name))
    val data = source.getLines.map(Set(_)).toArray
    source.close()
    data
  }

  def computeRecall: Double = {
    val distThreshold = 2
    val mappedThreshold = sm.computeThreshold(source1, 0.5, source2, 0.5, distThreshold)

    println("Threshold: " + mappedThreshold)

    var foundDuplicates = 0
    var actualDuplicates = 0

    for(i <- 0 until source1.size;
        j <- 0 until source2.size) {
      val dist = sm.mappedDistance(sm.coordinates(i), sm.coordinates(source1.size + j))
      val actualDist = distanceMetric(source1(i), source2(j))
      if(actualDist <= distThreshold) {
        actualDuplicates += 1
        if(dist < mappedThreshold) {
          foundDuplicates += 1
          //println("Found: " + stringVector1(i) + " - " + stringVector2(j) + " (" + actualDist + ")")
        } else {
          //println("Missed: " + stringVector1(i) + " - " + stringVector2(j) + " (" + actualDist + ")")
        }
      }
    }

    val recall = (foundDuplicates.toDouble / actualDuplicates)
    println("Recall: " + recall)
    recall
  }

  /**
   * Matcher to test if 2 coordinates are approximately equal.
   */
  case class approximatelyEqualTo(r: Array[Double]) extends BeMatcher[Array[Double]] {
    val epsilon = 0.00001

    def apply(l: Array[Double]) =
      MatchResult(
        compare(l, r),
        l.mkString("[", ",", "]") + " is not approximately equal to " + r.mkString("[", ",", "]"),
        l.mkString("[", ",", "]") + " is approximately equal to " + r.mkString("[", ",", "]")
      )

    private def compare(l: Array[Double], r: Array[Double]): Boolean = {
      (l zip r).forall{ case (c1, c2) => math.abs(c1 - c2) < epsilon }
    }
  }
}
