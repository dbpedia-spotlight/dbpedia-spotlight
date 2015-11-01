package org.dbpedia.spotlight.util

import breeze.linalg.DenseVector
import org.junit.Test
import org.scalatest.Assertions.intercept
import org.dbpedia.spotlight.util.MathUtil.{magnitude, cosineSimilarity}

/**
 * Created by dowling on 01/11/15.
 */
class MathUtilTest {
  @Test
  def testMagnitudeDouble {
    val doubleExamples = Map(
      DenseVector.zeros[Double](5).t -> 0.0,
      DenseVector.ones[Double](5).t -> 2.23606797749979,
      DenseVector.ones[Double](10).t -> 3.1622776601683795
    )

    doubleExamples.keys.foreach( vector => {
      val m = magnitude(vector)
      printf("%-30s=%30s \n",doubleExamples(vector),m)
      assert(m.equals(doubleExamples(vector)))
    })
  }

  @Test
  def testMagnitudeFloat{
    val floatExamples = Map(
      DenseVector.zeros[Float](5).t -> 0.0.toFloat,
      DenseVector.ones[Float](5).t -> 2.23606797749979.toFloat,
      DenseVector.ones[Float](10).t -> 3.1622776601683795.toFloat
    )

    floatExamples.keys.foreach( vector => {
      val m = magnitude(vector)
      printf("%-30s=%30s \n",floatExamples(vector),m)
      assert(m.equals(floatExamples(vector)))
    })
  }

  @Test
  def testCosineSimilarityDouble{
    val epsilon = 0.0001
    val doubleExamples = Map(
      (DenseVector.ones[Double](5).t, DenseVector.ones[Double](5).t) -> 1.0,
      (DenseVector(1.0, 0.0, 0.0).t, DenseVector(0.0, 1.0, 0.0).t) -> 0.0,
      (DenseVector(1.0, 1.0, 0.0).t, DenseVector(0.0, 1.0, 1.0).t) -> 0.5,
      (DenseVector(1.0, 1.0, 0.0, 0.0, 0.0, 0.0).t, DenseVector(0.0, 1.0, 1.0, 0.0, 0.0, 0.0).t) -> 0.5

    )
    doubleExamples.keys.foreach( vectors => {
      val sim = cosineSimilarity(vectors._1, vectors._2)
      printf("%-30s=%30s (+/-(%s)) \n",doubleExamples(vectors), sim, epsilon)
      assert((sim - doubleExamples(vectors)) < epsilon)
    })
  }

  @Test
  def testCosineSimilarityFloat{
    val epsilon = 0.0001
    val doubleExamples = Map(
      (DenseVector.ones[Float](5).t, DenseVector.ones[Float](5).t) -> 1.0.toFloat

    )
    doubleExamples.keys.foreach( vectors => {
      val sim = cosineSimilarity(vectors._1, vectors._2)
      printf("%-30s=%30s (+/-(%s)) \n",doubleExamples(vectors), sim, epsilon)
      assert((sim - doubleExamples(vectors)) < epsilon)
    })
  }

  @Test(expected = classOf[IllegalArgumentException])
  def testCosineSimilarityThrowsOnWrongDimensions{
    printf("Testing that cosine similarity fails on dimension mismatch..")
    val epsilon = 0.0001
    val doubleExamples = Map(
      (DenseVector.ones[Float](6).t, DenseVector.ones[Float](5).t) -> 1.0.toFloat

    )
    
    doubleExamples.keys.foreach( vectors => {
        val sim = cosineSimilarity(vectors._1, vectors._2)
        printf("%-30s=%30s (+/-(%s)) \n",doubleExamples(vectors), sim, epsilon)
        assert((sim - doubleExamples(vectors)) < epsilon)

    })
  }




}
