package org.dbpedia.spotlight.db.memory

import java.util.{Map, HashMap}
import scala.collection.JavaConversions._
import org.dbpedia.spotlight.db.model.{TokenTypeStore, ContextStore}
import com.esotericsoftware.kryo.io.{Input, Output}
import org.apache.commons.lang.{SerializationException, NotImplementedException}
import com.esotericsoftware.kryo.{KryoException, Kryo, KryoSerializable}
import org.dbpedia.spotlight.model.{TokenType, Token, DBpediaResource}


/**
 * @author Joachim Daiber
 *
 *
 *
 */

@SerialVersionUID(1007001)
class MemoryContextStore
  extends MemoryStore
  with ContextStore
  with KryoSerializable {

  @transient
  var tokenStore: TokenTypeStore = null

  @transient
  var totalTokenCounts: Array[Int] = null

  var tokens: Array[Array[Int]] = null
  var counts: Array[Array[Int]] = null

  def size = tokens.length

  def getContextCount(resource: DBpediaResource, token: TokenType): Int = {
    throw new NotImplementedException()
  }

  def getTotalTokenCount(resource: DBpediaResource): Int = totalTokenCounts(resource.id)

  def getContextCounts(resource: DBpediaResource): Map[TokenType, Int] = {

    val contextCounts = new HashMap[TokenType, Int]()
    val i = resource.id

    if (tokens(i) != null) {
      val t = tokens(i)
      val c = counts(i)

      (0 to t.length-1) foreach { j =>
        contextCounts.put(tokenStore.getTokenTypeByID(t(j)), c(j))
      }
    }

    contextCounts
  }

  def write(kryo: Kryo, output: Output) {
    output.writeInt(tokens.length)

    (0 to tokens.length-1).foreach { i =>
      if (tokens(i) == null) {
        output.writeInt(0)
      } else {
        output.writeInt(tokens(i).length)

        (0 to tokens(i).length-1).foreach{ j =>
          output.writeInt(tokens(i)(j))
        }
        (0 to tokens(i).length-1).foreach{ j =>
          output.writeInt(counts(i)(j))
        }
      }
    }
    output.writeChar('#')
  }

  def read(kryo: Kryo, input: Input) {
    val size = input.readInt()

    tokens = new Array[Array[Int]](size)
    counts = new Array[Array[Int]](size)
    totalTokenCounts = new Array[Int](size)

    var i = 0
    var j = 0

    while(i < size) {
      val subsize = input.readInt()

      if (subsize > 0) {
        tokens(i) = new Array[Int](subsize)
        counts(i) = new Array[Int](subsize)

        j = 0
        while(j < subsize) {
          tokens(i)(j) = input.readInt()
          j += 1
        }

        j = 0
        while(j < subsize) {
          counts(i)(j) = input.readInt()
          totalTokenCounts(i) += counts(i)(j)
          j += 1
        }
     }

     i += 1
   }

   if(input.readChar() != '#')
     throw new KryoException("Error in deserializing context store...")

  }

}
