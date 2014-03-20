import java.util
import java.util.Locale


import org.dbpedia.spotlight.db.memory.{MemoryQuantizedCountStore, MemorySurfaceFormStore}
import org.junit.Test
import org.junit.Assert.assertTrue
import org.scalatest.junit.AssertionsForJUnit
import org.scalatest.mock.MockitoSugar.mock
import org.mockito.Mockito

/**
 * A class for testing the LanguageIndependentTokenizer
 * @author Thiago Galery (thiago.galery@idioplatform.com)
 * @author David Przybilla (david.przybilla@idioplatform.com)
 */


class MemorySurfaceFormStoreTests extends AssertionsForJUnit{
  @Test
  def noWhiteSpaceTest {
    var store =  new MemorySurfaceFormStore()
    store.stringForID =  Array[String]("Barack Obama")
    store.annotatedCountForID = Array[Short](1)
    store.totalCountForID = Array[Short](1)
    store.idForString = new java.util.HashMap[String, java.lang.Integer]()
    store.idForString.put("Barack Obama", 0)
    val quantStore = new MemoryQuantizedCountStore()
    quantStore.countMap = new util.HashMap[Short, Int]()
    quantStore.countMap.put(1,1)
    store.quantizedCountStore = quantStore
    val b = store.getSurfaceForm("Barack Obama")
    assertTrue(b.id == 0)
  }

  @Test
  def WhiteSpaceTest {
    var store =  new MemorySurfaceFormStore()
    store.stringForID =  Array[String]("Barack Obama")
    store.annotatedCountForID = Array[Short](1)
    store.totalCountForID = Array[Short](1)
    store.idForString = new java.util.HashMap[String, java.lang.Integer]()
    store.idForString.put("Barack Obama", 0)
    val quantStore = new MemoryQuantizedCountStore()
    quantStore.countMap = new util.HashMap[Short, Int]()
    quantStore.countMap.put(1,1)
    store.quantizedCountStore = quantStore
    val b = store.getRankedSurfaceFormCandidates("Barack   Obama")
    assertTrue(b(0)._1.id == 0)
  }

  @Test
  def loweCaseWhiteSpaceTest {
    var store =  new MemorySurfaceFormStore()
    store.stringForID =  Array[String]("Barack Obama")
    store.annotatedCountForID = Array[Short](1)
    store.totalCountForID = Array[Short](1)
    store.idForString = new java.util.HashMap[String, java.lang.Integer]()
    store.idForString.put("Barack Obama", 0)
    val quantStore = new MemoryQuantizedCountStore()
    quantStore.countMap = new util.HashMap[Short, Int]()
    quantStore.countMap.put(1,1)
    store.quantizedCountStore = quantStore
    store.lowercaseMap = new java.util.HashMap[String, Array[Int]]()
    val baracksIds = Array[Int](0)
    store.lowercaseMap.put("barack obama", baracksIds)
    val b = store.getRankedSurfaceFormCandidates("barack   Obama")
    assertTrue(b(0)._1.id == 0)
  }
}
