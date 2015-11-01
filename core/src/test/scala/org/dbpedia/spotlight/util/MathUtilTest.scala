package org.dbpedia.spotlight.util

import breeze.linalg.DenseVector
import org.junit.Test
import org.dbpedia.spotlight.util.MathUtil.magnitude

/**
 * Created by dowling on 01/11/15.
 */
class MathUtilTest {
  @Test
  def testMagnitude {
    val doubleExamples = Map(
      DenseVector.zeros[Double](5).t -> 0,
      DenseVector.ones[Double](5).t -> 0
    )

    doubleExamples.keys.foreach( vector => {
      assert(magnitude(vector).equals(doubleExamples(vector)))
    })
  }

}
