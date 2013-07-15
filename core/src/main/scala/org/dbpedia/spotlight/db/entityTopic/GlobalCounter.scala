package org.dbpedia.spotlight.db.entitytopic

import java.util.HashMap
import breeze.linalg.CSCMatrix

/**
 * Global Counter implemented by sparse matrix from breeze
 * count is the matrix
 * countSum is the sum of entries in one row
 *
 * @param rows
 * @param cols
 */

class GlobalCounter(rows:Int, cols:Int) {
  val count: CSCMatrix[Int]=CSCMatrix.zeros(rows,cols)
  val countSum: HashMap[Int,Int]=new HashMap[Int, Int]()

  def incCount(first: Int, second: Int){
    count.update(first,second,count(first,second)+1)
    countSum.get(first) match{
      case num:Int=>countSum.put(first,num+1)
      case _=>countSum.put(first,1)
    }
  }

  def decCount(first: Int, second: Int){
    count.update(first,second,count(first,second)-1)
    countSum.get(first) match{
      case num:Int=>countSum.put(first,num-1)
      case _=>countSum.put(first,0)
    }
  }

  def getCount(first: Int, second: Int):Int={
    count(first,second)
  }

  def getCountSum(first: Int):Int={
    countSum.get(first)
  }

}
