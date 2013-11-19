package org.dbpedia.spotlight.db.memory.util

import it.unimi.dsi.fastutil.objects.{Object2ShortOpenHashMap, Object2IntOpenHashMap}
import java.lang.Integer

/**
 * Factory for a Map for efficient lookup of an ID given a String.
 *
 * @author Joachim Daiber
 */

object StringToIDMapFactory {

  def createFastUtil(expectedSize: Int): java.util.Map[String, Integer] = new Object2IntOpenHashMap[String](expectedSize)
  def createDefault(expectedSize: Int): java.util.Map[String, Integer]  = createFastUtil(expectedSize)
  def createDefaultShort(expectedSize: Int): java.util.Map[String, java.lang.Short] = new Object2ShortOpenHashMap[String](expectedSize)

}