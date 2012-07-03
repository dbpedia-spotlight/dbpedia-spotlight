package org.dbpedia.spotlight.db.memory

import org.dbpedia.spotlight.model.{Token, DBpediaResource}
import java.util.{Map, HashMap}
import scala.collection.JavaConversions._
import org.dbpedia.spotlight.db.model.{TokenStore, ContextStore}
import com.esotericsoftware.kryo.io.{Input, Output}
import org.apache.commons.lang.{SerializationException, NotImplementedException}
import com.esotericsoftware.kryo.{KryoException, Kryo, KryoSerializable}


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
  var tokenStore: TokenStore = null

  var tokens: Array[Array[Int]] = null
  var counts: Array[Array[Int]] = null

  def size = tokens.length

  def getContextCount(resource: DBpediaResource, token: Token): Int = {
    throw new NotImplementedException()
  }

  def getContextCounts(resource: DBpediaResource): Map[Token, Int] = {

    val contextCounts = new HashMap[Token, Int]()
    val i = resource.id +1

    if (tokens(i) != null) {
      val t = tokens(i)
      val c = counts(i)

      (0 to t.length-1) foreach { j =>
        contextCounts.put(tokenStore.getTokenByID(t(j)), c(j))
      }
    }

    contextCounts
  }

  def write(kryo: Kryo, output: Output) {
    output.writeInt(tokens.length)

    (0 to tokens.length-1).foreach { i =>
      output.writeInt(tokens(i).length)

      tokens(i).foreach{ token =>
        output.writeInt(token)
      }
      counts(i).foreach{ count =>
        output.writeInt(count)
      }
      if (i == tokens.length-1)
        output.writeChar('#')
    }
  }

  def read(kryo: Kryo, input: Input) {
    val size = input.readInt()

    (0 to size-1).foreach { i =>
      tokens = new Array[Array[Int]](size)
      counts = new Array[Array[Int]](size)

      val subsize = input.readInt()
      tokens(i) = new Array[Int](subsize)
      counts(i) = new Array[Int](subsize)

      (0 to subsize-1).foreach { j =>
        tokens(i)(j) = input.readInt()
      }
      (0 to subsize-1).foreach { j =>
        counts(i)(j) = input.readInt()
      }

      if(input.readChar() != '#')
        throw new KryoException("Error in deserializing context store...")
    }

  }

}
