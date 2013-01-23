package de.fuberlin.wiwiss.silk.execution

import de.fuberlin.wiwiss.silk.entity.{Path, Link, Index, Entity}
import de.fuberlin.wiwiss.silk.linkagerule.LinkageRule
import de.fuberlin.wiwiss.silk.cache.Partition
import de.fuberlin.wiwiss.silk.util.DPair
import methods.MultiBlock
import scala.math.{min, max, abs}

/**
 * The execution method determines how a linkage rule is executed.
 */
trait ExecutionMethod {

  /**
   * Generates an index for a single entity.
   */
  def indexEntity(entity: Entity, rule: LinkageRule): Index = Index.default

  /**
   * Generates comparison pairs from two partitions.
   */
  def comparisonPairs(sourcePartition: Partition, targetPartition: Partition, full: Boolean) = new Traversable[DPair[Entity]] {
    /**
     * Iterates through all comparison pairs
     */
    def foreach[U](f: DPair[Entity] => U) {
      //Iterate over all entities in the source partition
      var s = 0
      while(s < sourcePartition.size) {
        //Iterate over all entities in the target partition
        var t = if (full) 0 else s + 1
        while(t < targetPartition.size) {
          //Check if the indices match
          if((sourcePartition.indices(s) matches targetPartition.indices(t))) {
            //Yield entity pair
            f(DPair(sourcePartition.entities(s), targetPartition.entities(t)))
          }
          t += 1
        }
        s += 1
      }
    }
  }
}

object ExecutionMethod {
  /** Returns the default execution method. */
  def apply(): ExecutionMethod = new methods.MultiBlock()
}

//TODO sorted blocks and sortedneighbourhood

//class SortedNeighbourhoodExecutionMethod(blockingKey: Path) extends ExecutionMethod {
//
//  private val minChar = '0'
//  private val maxChar = 'z'
//  private val indexSize = 5 //Maximum number of chars that will be indexed
//
//  override def indexEntity(entity: Entity, rule: LinkageRule): Index = {
//    val values = entity.evaluate(blockingKey)
//    Index.oneDim(
//      indices = values.map(indexValue),
//      size = BigInt(maxChar - minChar + 1).pow(indexSize).toInt
//    )
//  }
//
//  override def comparisonPairs(sourcePartition: Partition, targetPartition: Partition, full: Boolean) = {
//    sourcePartition.entities
//  }
//
//  private def indexValue(value: String): Int = {
//    def combine(index: Int, char: Char) = {
//      val croppedChar = min(max(char, minChar), maxChar)
//      index * (maxChar - minChar + 1) + croppedChar - minChar
//    }
//
//    value.foldLeft(0)(combine)
//  }
//}
