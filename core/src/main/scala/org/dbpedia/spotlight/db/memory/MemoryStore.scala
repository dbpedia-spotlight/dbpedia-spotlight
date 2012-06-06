package org.dbpedia.spotlight.db.memory

import gnu.trove.TObjectIntHashMap
import com.esotericsoftware.kryo.Kryo
import java.io._
import com.esotericsoftware.kryo.io.{Output, Input}
import com.esotericsoftware.kryo.serializers.JavaSerializer

/**
 * @author Joachim Daiber
 *
 *
 *
 */

class MemoryStore extends Serializable

object MemoryStore {

  val kryo = new Kryo()
  kryo.register(classOf[TObjectIntHashMap], new JavaSerializer())

  def load[B](in: InputStream): B = {
    println("Loading store...")
    val input = new Input(in)

    val s: B = kryo.readClassAndObject(input).asInstanceOf[B]

    input.close()
    println("Done.")
    s
  }


  def dump[A](store: A, out: File) {
    println("Writing store...")
    val output = new Output(new FileOutputStream(out))
    kryo.writeClassAndObject(output, store)
    output.close()
    println("Done.")
  }


}
