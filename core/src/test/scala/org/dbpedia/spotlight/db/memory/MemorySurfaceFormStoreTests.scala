import java.util
import org.dbpedia.spotlight.db.memory.{MemoryQuantizedCountStore, MemorySurfaceFormStore}
import org.junit.Test
import org.junit.Assert.assertTrue
import org.scalatest.junit.AssertionsForJUnit


/**
 * A class for testing the Memory Surface Form Store
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
    //Run the test with only one intervening whitespace
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
    //Run the test with many intervening whitespaces, result should be the same
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
    // Mock the lower case map, first element in the array is lc sf count
    val baracksIds = Array[Int](20, 0)
    store.lowercaseMap.put("barack obama", baracksIds)
    //Run the test with mixed case and many intervening whitespaces
    val b = store.getRankedSurfaceFormCandidates("barack   Obama")
    assertTrue(b(0)._1.id == 0)
  }
}
