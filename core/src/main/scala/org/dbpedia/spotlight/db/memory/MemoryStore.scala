package org.dbpedia.spotlight.db.memory

import com.esotericsoftware.kryo.Kryo

import com.esotericsoftware.kryo.io.{Output, Input}
import org.dbpedia.spotlight.model.{DBpediaType, FreebaseType, SchemaOrgType, OntologyType}
import java.io._
import gnu.trove.TObjectIntHashMap
import scala.Predef._
import com.esotericsoftware.kryo.serializers.{FieldSerializer, DefaultArraySerializers, JavaSerializer}
import org.apache.mahout.math.map.OpenObjectIntHashMap
import java.lang.{System, Short, String}
import org.dbpedia.spotlight.db.model.CandidateMapStore
import collection.mutable.{ListBuffer, HashMap}


/**
 * @author Joachim Daiber
 */

@SerialVersionUID(1001001)
abstract class MemoryStore extends Serializable {


  def loaded() {
    //Implementations may execute code after the store is loaded
  }

  def size: Int

}

object MemoryStore {

  val kryos = HashMap[String, Kryo]()

  kryos.put(classOf[MemoryResourceStore].getSimpleName,
    {
      val kryo = new Kryo()
      kryo.setRegistrationRequired(true)

      kryo.register(classOf[Array[Int]], new DefaultArraySerializers.IntArraySerializer())
      kryo.register(classOf[Array[String]], new DefaultArraySerializers.StringArraySerializer())
      kryo.register(classOf[Array[Array[Short]]], new JavaSerializer())

      kryo.register(classOf[OntologyType])
      kryo.register(classOf[DBpediaType])
      kryo.register(classOf[FreebaseType])
      kryo.register(classOf[SchemaOrgType])

      kryo.register(classOf[MemoryResourceStore])
      kryo.register(classOf[MemoryOntologyTypeStore], new JavaSerializer())

      kryo
    }
  )

  kryos.put(classOf[MemorySurfaceFormStore].getSimpleName,
    {
      val kryo = new Kryo()
      kryo.setRegistrationRequired(true)

      kryo.register(classOf[Array[Int]],    new DefaultArraySerializers.IntArraySerializer())
      kryo.register(classOf[Array[String]], new DefaultArraySerializers.StringArraySerializer())
      //kryo.register(classOf[TObjectIntHashMap], new JavaSerializer())
      kryo.register(classOf[MemorySurfaceFormStore])

      kryo
    }
  )

  kryos.put(classOf[MemoryContextStore].getSimpleName,
    {
      val kryo = new Kryo()
      kryo.setRegistrationRequired(true)

      kryo.register(classOf[MemoryContextStore], new JavaSerializer())

      kryo
    }
  )

  kryos.put(classOf[MemoryTokenStore].getSimpleName,
    {
      val kryo = new Kryo()
      kryo.setRegistrationRequired(true)

      kryo.register(classOf[Array[Int]],    new DefaultArraySerializers.IntArraySerializer())
      kryo.register(classOf[Array[String]], new DefaultArraySerializers.StringArraySerializer())
      kryo.register(classOf[MemoryTokenStore])

      kryo
    }
  )


  def load[T](in: InputStream, os: MemoryStore): T = {

    val kryo: Kryo = kryos.get(os.getClass.getSimpleName).get

    System.err.println("Loading %s...".format(os.getClass.getSimpleName))
    val sStart = System.currentTimeMillis()
    val input = new Input(in)

    val s = kryo.readClassAndObject(input).asInstanceOf[T]
    s.asInstanceOf[MemoryStore].loaded()

    input.close()
    System.err.println("Done (%d ms)".format(System.currentTimeMillis() - sStart))
    s
  }


  def dump(store: MemoryStore, out: File) {
    val kryo = kryos.get(store.getClass.getSimpleName).get

    System.err.println("Writing %s...".format(store.getClass.getSimpleName))
    val output = new Output(new FileOutputStream(out))
    kryo.writeClassAndObject(output, store)

    output.close()
    System.err.println("Done.")
  }


}
