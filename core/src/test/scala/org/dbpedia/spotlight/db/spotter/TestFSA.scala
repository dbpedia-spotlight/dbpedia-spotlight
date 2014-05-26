package org.dbpedia.spotlight.db.spotter

/**
 * Created by dav009 on 24/03/2014.
 */

import org.junit.Test
import org.junit.Assert.assertEquals
import org.dbpedia.spotlight.db.{FSADictionary, FSASpotter}
import scala.Array


object TestFSA{

    def getMockedFSA():FSADictionary ={
      val testFSADict:FSADictionary = new FSADictionary()

      testFSADict.transitionsTokens = Array[Array[Int]](
      Array[Int](100, 200, 200, 300, 400), //state 0
      Array[Int](500) //state 0
      )
      testFSADict.transitionsStates = Array[Array[Int]](
      Array[Int](-1, -1, 1, -1, 1),
      Array[Int](-1)
      )

      testFSADict

    }

}
class TestFSA  {

  @Test
  def testFSATransitions(){

    val testFSADict:FSADictionary = TestFSA.getMockedFSA()

    // binary search odd case
    val(status, nextState) = testFSADict.next(0, 200)
    assertEquals(status, FSASpotter.ACCEPTING_STATE)
    assertEquals(nextState, 1)

    // token no given for transition
    val (status2, nextState2) = testFSADict.next(0, 900)
    assertEquals(status2, FSASpotter.REJECTING_STATE)
    assertEquals(nextState2, FSASpotter.REJECTING_STATE)

    // current str is not yet a match
    val (status3, nextState3) = testFSADict.next(0, 400)
    assertEquals(status3, FSASpotter.REJECTING_STATE)
    assertEquals(nextState3, 1)

  }

  @Test
  def testSearchTokensInTransition(){
    val testFSADict:FSADictionary = TestFSA.getMockedFSA()

    // checks odd binary serach case
    assertEquals(testFSADict.searchTokenInTransitions(0, 200), 1)

    // non existent item
    assert(testFSADict.searchTokenInTransitions(0, 900) < 0)

    //last item
    assertEquals(testFSADict.searchTokenInTransitions(0, 400), 4)
  }

}
