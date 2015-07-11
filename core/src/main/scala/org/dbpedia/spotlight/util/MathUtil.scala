package org.dbpedia.spotlight.util

import breeze.linalg.{DenseVector, Transpose}
import breeze.numerics._
import org.apache.commons.math.util.FastMath

/**
 * @author Joachim Daiber
 */

object MathUtil {

  val LOGZERO = Double.NegativeInfinity

  def isLogZero(x: Double): Boolean = x.isNegInfinity

  def exp(x: Double): Double = {
    if (x.isNegInfinity)
      0.0
    else
      FastMath.exp(x)
  }

  def ln(x: Double): Double = {
    if(x == 0.0)
      LOGZERO
    else
      FastMath.log(x)
  }

  def lnsum(a: Double, b: Double): Double = {
    if(a.isNegInfinity || b.isNegInfinity) {
      if(a.isNegInfinity)
        b
      else
        a
    } else {
      if(a > b)
        a + ln(1 + FastMath.exp(b-a))
      else
        b + ln(1 + FastMath.exp(a-b))
    }
  }

  def lnsum(seq: TraversableOnce[Double]): Double = {
    seq.foldLeft(MathUtil.ln(0.0))(MathUtil.lnsum)
  }

  def lnproduct(seq: TraversableOnce[Double]): Double = {
    seq.foldLeft(MathUtil.ln(1.0))(MathUtil.lnproduct)
  }

  def lnproduct(a: Double, b: Double): Double = {
    if (a.isNegInfinity || b.isNegInfinity)
      LOGZERO
    else
      a + b
  }

  def magnitude(vector: Transpose[DenseVector[Double]]): Double = {
    sqrt(vector * vector.t)
  }
  def magnitude(vector: Transpose[DenseVector[Float]]): Float = {
    sqrt(vector * vector.t)
  }

  def cosineSimilarity(vector1: Transpose[DenseVector[Double]], vector2: Transpose[DenseVector[Double]]): Double = {
    (vector1 * vector2.t) / (magnitude(vector1) * magnitude(vector2))
  }
  def cosineSimilarity(vector1: Transpose[DenseVector[Float]], vector2: Transpose[DenseVector[Float]]): Float = {
    (vector1 * vector2.t) / (magnitude(vector1) * magnitude(vector2))
  }
}