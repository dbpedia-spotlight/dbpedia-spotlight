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
import org.apache.commons.logging.LogFactory
import com.esotericsoftware.kryo.serializers.DefaultSerializers.KryoSerializableSerializer
import org.dbpedia.spotlight.db.memory.MemoryStore._


/**
 * @author Joachim Daiber
 */

@SerialVersionUID(1001001)
abstract class MemoryStore extends Serializable {

  @transient
  private val LOG = LogFactory.getLog(this.getClass)

  def loaded() {
    //Implementations may execute code after the store is loaded
  }

  def size: Int

}

object MemoryStore {

  private val LOG = LogFactory.getLog(this.getClass)

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
      kryo.register(classOf[MemorySurfaceFormStore])

      kryo
    }
  )

  kryos.put(classOf[MemoryContextStore].getSimpleName,
    {
      val kryo = new Kryo()
      kryo.setRegistrationRequired(true)

      kryo.register(classOf[MemoryContextStore], new KryoSerializableSerializer())

      kryo
    }
  )

  kryos.put(classOf[MemoryCandidateMapStore].getSimpleName,
    {
      val kryo = new Kryo()
      kryo.setRegistrationRequired(true)

      kryo.register(classOf[MemoryCandidateMapStore], new JavaSerializer())

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

  def load[T](in: InputStream, ms: MemoryStore): T = {

    val kryo: Kryo = kryos.get(ms.getClass.getSimpleName).get

    LOG.info("Loading %s...".format(ms.getClass.getSimpleName))
    val sStart = System.currentTimeMillis()
    val input = new Input(in)

    val s = kryo.readClassAndObject(input).asInstanceOf[T]
    s.asInstanceOf[MemoryStore].loaded()

    input.close()
    LOG.info("Done (%d ms)".format(System.currentTimeMillis() - sStart))
    s
  }


  def dump(store: MemoryStore, out: File) {
    val kryo = kryos.get(store.getClass.getSimpleName).get

    LOG.info("Writing %s...".format(store.getClass.getSimpleName))
    val output = new Output(new FileOutputStream(out))
    kryo.writeClassAndObject(output, store)

    output.close()
    LOG.info("Done.")
  }


}
