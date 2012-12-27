package org.dbpedia.spotlight.db.memory


import com.esotericsoftware.kryo.io.{Output, Input}
import org.dbpedia.spotlight.model.{DBpediaType, FreebaseType, SchemaOrgType, OntologyType}
import java.io._
import scala.Predef._
import com.esotericsoftware.kryo.serializers.{DefaultArraySerializers, JavaSerializer}
import java.lang.{System, Short, String}
import collection.mutable.HashMap
import org.apache.commons.logging.LogFactory
import com.esotericsoftware.kryo.serializers.DefaultSerializers.KryoSerializableSerializer
import com.esotericsoftware.kryo.Kryo
import org.dbpedia.spotlight.db.model.{TokenTypeStore, ResourceStore}


/**
 * Base class for all memory stores.
 *
 * @author Joachim Daiber
 */

@SerialVersionUID(1001001)
abstract class MemoryStore extends Serializable {

  @transient
  protected val LOG = LogFactory.getLog(this.getClass)

  /**
   * Method called after the store has been deserialized.
   * Implementations may override this method in order to finish setting
   * up the store.
   */
  def loaded() {}

  def size: Int
}


/**
 * Utility object for loading memory stores.
 */
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


  kryos.put(classOf[MemoryTokenTypeStore].getSimpleName,
    {
      val kryo = new Kryo()
      kryo.setRegistrationRequired(true)

      kryo.register(classOf[Array[Int]],    new DefaultArraySerializers.IntArraySerializer())
      kryo.register(classOf[Array[String]], new DefaultArraySerializers.StringArraySerializer())
      kryo.register(classOf[MemoryTokenTypeStore])

      kryo
    }
  )

  def load[T](in: InputStream, simpleName: String): T = {

    val kryo: Kryo = kryos.get(simpleName).get

    LOG.info("Loading %s...".format(simpleName))
    val sStart = System.currentTimeMillis()
    val input = new Input(in)

    val s = kryo.readClassAndObject(input).asInstanceOf[T]
    s.asInstanceOf[MemoryStore].loaded()

    input.close()
    LOG.info("Done (%d ms)".format(System.currentTimeMillis() - sStart))
    s
  }

  def loadTokenTypeStore(in: InputStream): MemoryTokenTypeStore = {
    load[MemoryTokenTypeStore](in, classOf[MemoryTokenTypeStore].getSimpleName)
  }

  def loadSurfaceFormStore(in: InputStream): MemorySurfaceFormStore = {
    load[MemorySurfaceFormStore](in, classOf[MemorySurfaceFormStore].getSimpleName)
  }

  def loadResourceStore(in: InputStream): MemoryResourceStore = {
    load[MemoryResourceStore](in, classOf[MemoryResourceStore].getSimpleName)
  }

  def loadCandidateMapStore(in: InputStream, resourceStore: ResourceStore): MemoryCandidateMapStore = {
    val s = load[MemoryCandidateMapStore](in, classOf[MemoryCandidateMapStore].getSimpleName)
    s.resourceStore = resourceStore
    s
  }

  def loadContextStore(in: InputStream, tokenStore: TokenTypeStore): MemoryContextStore = {
    val s = load[MemoryContextStore](in, classOf[MemoryContextStore].getSimpleName)
    s.tokenStore = tokenStore
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
