package org.dbpedia.spotlight.db.entitytopic

import java.util.HashMap
import java.io._
import org.apache.commons.logging.LogFactory
import scala.collection.JavaConverters._
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * Global counter is to store the counts of entity-token, entity-mention, topic-entity pairs
 * Since the # of entities and mentions are large, e.g., >1M, we cannot use matrix.
 * Here we use one HashMap for each row of the 'matrix' *
 * rowSum is the sum of entries in one row
 *
 * @param matrix
 * @param rowSum
 */

class GlobalCounter(val name:String, val matrix: Array[HashMap[Int,Float]],val rowSum: Array[Float],var samples:Int=1) {

  //add locks ensuring concurrency for reading and writing global counters
  val readwriteLock=new Array[ReentrantReadWriteLock](rowSum.size)
  (0 until rowSum.size).foreach(i=>readwriteLock(i)=new ReentrantReadWriteLock())
  val readlock=readwriteLock.map(lock=>lock.readLock())
  val writelock=readwriteLock.map(lock=>lock.writeLock())

  def incCount(row: Int, col: Int){
    writelock(row).lock()
    try{
      val rowMap=matrix(row)
      rowMap.get(col) match{
        case v:Float=>rowMap.put(col,v+1)
        case _=>rowMap.put(col,1)
      }
    }finally {
      writelock(row).unlock()
    }

    rowSum(row)+=1
  }

  def decCount(row: Int, col: Int){
    writelock(row).lock()
    try{

        val rowMap=matrix(row)
      rowMap.get(col) match{
        case v:Float=>if (v>0) rowMap.put(col, v-1) else throw new IllegalArgumentException("decrease 0 matrix entry %d %d %s".format(row,col,name))
        case _=> throw new IllegalArgumentException("decrease null entry %d %d %s".format(row,col,name))
      }
      if(rowSum(row)>0)
        rowSum(row)-=1
      else throw new IllegalArgumentException("decrease 0 rowSum %d %d %s".format(row,col,name))
    }finally {
      writelock(row).unlock()
    }
  }

  def getCount(row: Int, col: Int):Float={
    readlock(row).lock()
    try{
      val rowMap=matrix(row)
      rowMap.get(col) match{
        case v:Float=>v
        case _=>0
      }
    }finally {
      readlock(row).unlock()
    }
  }

  def getCountSum(row: Int):Float={
    readlock(row).lock()
    try{
      rowSum(row)
    }finally {
      readlock(row).unlock()
    }
  }

  def add(other:GlobalCounter){
    (matrix, other.matrix).zipped.foreach((map1:HashMap[Int,Float],map2:HashMap[Int,Float])=>{
      map2.keySet().asScala.foreach((col:Int)=>{
        val v2=map2.get(col)
        if (v2>0){
          map1.get(col) match{
            case v1:Float=>map1.put(col,v1+v2)
            case _=>map1.put(col,v2)
          }
        }
      })
    })

    (0 until rowSum.length).foreach((i:Int)=>{
      rowSum(i)+=other.rowSum(i)
    })
  }

  def writeToFile(filePath:String){
    val writer=new BufferedWriter(new FileWriter(filePath))

    writer.write("rows:%d samples:%d name:%s".format(matrix.length,samples,name))
    (matrix).zipWithIndex.foreach{case (map:HashMap[Int,Float],id:Int)=>{
      writer.write("\n%d %.3f".format(id, rowSum(id)))
      map.asScala.foreach{case (k,v)=>{
        if(v>0)
          writer.write(" %d %.3f".format(k,v))
      }}
    }}

    writer.flush()
    writer.close()
  }

  /**
   * this is for aggregation counters. the final global knowledge is the average of all samples
   * @param filePath
   */
  def writeAvgToFile(filePath:String){
    val writer=new BufferedWriter(new FileWriter(filePath))

    writer.write("rows:%d samples:%d name:%s".format(matrix.length,1,name))
    (matrix).zipWithIndex.foreach{case (map:HashMap[Int,Float],id:Int)=>{
      writer.write("\n%d %.3f".format(id, rowSum(id)))
      map.asScala.foreach{case (k,v)=>{
        if(v>0)
          writer.write(" %d %.3f".format(k,v/samples))
      }}
    }}

    writer.flush()
    writer.close()
  }
}

object GlobalCounter{
  val LOG = LogFactory.getLog(this.getClass)

  def apply(name:String, rows:Int, cols:Int=30):GlobalCounter={
    val matrix=new Array[HashMap[Int,Float]](rows)
    (0 until rows).foreach((i:Int)=>{
      matrix(i)=new HashMap[Int,Float](cols)
    })

    val rowSum=new Array[Float](rows)
    new GlobalCounter(name, matrix, rowSum)
  }

  def apply(name:String, other:GlobalCounter):GlobalCounter={
    apply(name,other.matrix.length)
  }

  def readFromFile(filePath:String):GlobalCounter={
    LOG.info("reading global counter...")

    val file=new File(filePath)
    val reader=new BufferedReader(new FileReader(file))
    val metaString=reader.readLine()
    val fields=metaString.split("[: ]")
    val rows=fields(1).toInt
    val samples=fields(3).toInt

    val matrix=new Array[HashMap[Int,Float]](rows)
    val rowSum=new Array[Float](rows)


    (0 until rows).foreach((row:Int)=>{
      val string=reader.readLine()
      val fields=string.split(" ")
      val entrynum:Int=(fields.length-2)*3/4+1
      val map=new HashMap[Int,Float](entrynum)
      assert(row==fields(0).toInt)
      rowSum(row)+=fields(1).toFloat
      var i=2
      while(i<fields.length){
        val col=fields(i).toInt
        val v=fields(i+1).toFloat
        map.put(col,v)
        i+=2
      }
      matrix(row)=map
    })
    reader.close()
    new GlobalCounter(file.getName(), matrix, rowSum, samples)
  }
}
