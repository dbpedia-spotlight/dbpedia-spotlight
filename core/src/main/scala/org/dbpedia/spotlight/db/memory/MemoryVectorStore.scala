package org.dbpedia.spotlight.db.memory

import breeze.linalg.{DenseVector, Transpose, DenseMatrix}
import com.esotericsoftware.kryo.io.{Output, Input}
import com.esotericsoftware.kryo.{Kryo, KryoSerializable}
import org.dbpedia.spotlight.log.SpotlightLog
import org.dbpedia.spotlight.model.{TokenType, DBpediaResource}

/**
 * Created by dowling on 09/07/15.
 */
@SerialVersionUID(1008001)
class MemoryVectorStore extends MemoryStore with KryoSerializable{

  @transient
  var vectors: DenseMatrix[Float] = null

  @transient
  var resourceIdToVectorIndex: Map[Int, Int] = null

  @transient
  var tokenTypeIdToVectorIndex: Map[Int, Int] = null

  override def size: Int = vectors.rows

  def lookupItem(id: Int) = {
    // look up vector, if it isn't there, simply ignore the word
    if(id != -1){
      vectors(id, ::)
    }else{
      DenseVector.zeros[Float](vectors.cols).t
    }
  }

  def onNilIndex(string: String) = {
    SpotlightLog.warn(this.getClass, "Warning: token " + string + " not in dictionary! Lookup returning null vector.")
    -1
  }

  def lookup(resource: DBpediaResource): Transpose[DenseVector[Float]]={
    lookupItem(resourceIdToVectorIndex.getOrElse(resource.id, onNilIndex(resource.getFullUri)))

  }

  def lookup(token: TokenType): Transpose[DenseVector[Float]]={
    lookupItem(tokenTypeIdToVectorIndex.getOrElse(token.id, onNilIndex(token.tokenType)))

  }

  override def write(kryo: Kryo, output: Output): Unit = {
    output.writeString("# VECTORS")
    output.writeInt(vectors.rows)
    output.writeInt(vectors.cols)
    (0 to vectors.rows-1).foreach { rowIdx =>
      (0 to vectors.cols -1).foreach { colIdx =>
        output.writeFloat(vectors(rowIdx,colIdx))
      }
    }
    output.writeString("# RESOURCEDICT")
    output.writeInt(resourceIdToVectorIndex.size)
    resourceIdToVectorIndex.foreach { case(key, value) =>
      output.writeInt(key)
      output.writeInt(value)
    }
    output.writeString("# TOKENDICT")
    output.writeInt(tokenTypeIdToVectorIndex.size)
    tokenTypeIdToVectorIndex.foreach { case(key, value) =>
      output.writeInt(key)
      output.writeInt(value)
    }
    output.writeChar('#')
    output.close()

  }

  override def read(kryo: Kryo, input: Input): Unit = {
    assert(input.readString() == "# VECTORS")
    val rows = input.readInt()
    val cols = input.readInt()

    vectors = new DenseMatrix[Float](rows, cols)

    (0 to rows-1).foreach { rowIdx =>
      (0 to cols -1).foreach { colIdx =>
        vectors(rowIdx,colIdx) = input.readFloat()
      }
    }
    
    assert(input.readString() == "# RESOURCEDICT")
    val resourceNum = input.readInt()
    resourceIdToVectorIndex = (0 to resourceNum-1).map { i =>
      (input.readInt(), input.readInt())
    }.toMap

    assert(input.readString() == "# TOKENDICT")
    val tokenNum = input.readInt()
    tokenTypeIdToVectorIndex = (0 to tokenNum-1).map { i =>
      (input.readInt(), input.readInt())
    }.toMap

    assert(input.readChar() == '#')
    input.close()
  }
}
