package org.dbpedia.spotlight.db.entitytopic

import java.util.HashMap

/**
 * Created with IntelliJ IDEA.
 * User: a0082293
 * Date: 7/7/13
 * Time: 3:32 PM
 * To change this template use File | Settings | File Templates.
 */
class GlobalCounter {
  val count: HashMap[(Int,Int),Int]=new HashMap[(Int,Int),Int]()
  val countSum: HashMap[Int,Int]=new HashMap[Int, Int]()

  def incCount(first: Int, second: Int){

  }

  def decCount(first: Int, second: Int){

  }

  def getCount(first: Int, second: Int):Int={

    count.get((first,second))
  }

  def getCountSum(first: Int):Int={
    countSum.get(first)
  }

}
