package org.dbpedia.spotlight.db.entitytopic

import java.util.HashMap
import java.io.{FileReader, BufferedReader, FileWriter, BufferedWriter}
import org.apache.commons.logging.LogFactory
import scala.collection.JavaConverters._



/**
 * Global Counter implemented by sparse matrix from breeze
 * matrix is the matrix
 * rowSum is the sum of entries in one row
 *
 * @param matrix
 * @param rowSum
 */

class GlobalCounter( val matrix: Array[HashMap[Int,Int]],val rowSum: Array[Int]) {

  def incCount(row: Int, col: Int){
    val rowMap=matrix(row)
    rowMap.get(col) match{
      case v:Int=>rowMap.put(col,v+1)
      case _=>rowMap.put(col,1)
    }

    rowSum(row)+=1
  }

  def decCount(row: Int, col: Int){
    val rowMap=matrix(row)
    rowMap.get(col) match{
      case v:Int=>if (v>0) rowMap.put(col, v-1) else throw new IllegalArgumentException("decrease 0 matrix entry")
      case _=> throw new IllegalArgumentException("decrease null entry")
    }
    rowSum(row)-=1
  }

  def getCount(row: Int, col: Int):Int={
    val rowMap=matrix(row)
    rowMap.get(col) match{
      case v:Int=>v
      case _=>0
    }
  }

  def getCountSum(row: Int):Int={
    rowSum(row)
  }

  def add(other:GlobalCounter){
    (matrix, other.matrix).zipped.foreach((map1:HashMap[Int,Int],map2:HashMap[Int,Int])=>{
      map2.keySet().asScala.foreach((col:Int)=>{
        val v2=map2.get(col)
        if (v2>0){
          map1.get(col) match{
            case v1:Int=>map1.put(col,v1+v2)
            case _=>map1.put(col,v2)
          }
        }
      })
    })
  }

  def writeToFile(filePath:String){
    val writer=new BufferedWriter(new FileWriter(filePath))

    writer.write("%d  CSCMatrix".format(matrix.length))
    (matrix).zipWithIndex.foreach{case (map:HashMap[Int,Int],id:Int)=>{
      writer.write("\n%d %d".format(id, rowSum(id)))
      map.asScala.foreach{case (k,v)=>{
        writer.write(" %d %d".format(k,v))
      }}
    }}

    writer.flush()
    writer.close()
  }
}

object GlobalCounter{
  val LOG = LogFactory.getLog(this.getClass)
  def apply(rows:Int, cols:Int):GlobalCounter={
    val matrix=new Array[HashMap[Int,Int]](rows)
    (0 until rows).foreach((i:Int)=>{
      matrix(i)=new HashMap[Int,Int](cols/1000)
    })

    val rowSum=new Array[Int](rows)
    new GlobalCounter(matrix, rowSum)
  }

  def readFromFile(filePath:String):GlobalCounter={
    LOG.info("reading global counter...")

    val reader=new BufferedReader(new FileReader(filePath))
    val metaString=reader.readLine()
    val fields=metaString.split(" ")
    val rows=fields(0).toInt

    val counter=GlobalCounter(rows,0)

    (0 until rows).foreach((row:Int)=>{
      val string=reader.readLine()
      val fields=string.split(" ")
      val map=counter.matrix(row)
      assert(row==fields(0).toInt)
      counter.rowSum(row)+=fields(1).toInt
      var i=2
      while(i<fields.length){
        val col=fields(i).toInt
        val v=fields(i+1).toInt
        map.put(col,v)
        i+=2
      }
    })
    reader.close()
    counter
  }
}
