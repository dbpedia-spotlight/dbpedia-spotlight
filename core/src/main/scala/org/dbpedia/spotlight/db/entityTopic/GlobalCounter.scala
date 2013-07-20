package org.dbpedia.spotlight.db.entitytopic

import java.util.HashMap
import breeze.linalg.CSCMatrix
import java.io.{FileReader, BufferedReader, FileWriter, BufferedWriter}


/**
 * Global Counter implemented by sparse matrix from breeze
 * matrix is the matrix
 * rowSum is the sum of entries in one row
 *
 * @param matrix
 * @param rowSum
 */

class GlobalCounter( val matrix: CSCMatrix[Int],val rowSum: HashMap[Int,Int]) {

  def incCount(row: Int, col: Int){
    matrix.update(row,col,matrix(row,col)+1)
    rowSum.get(row) match{
      case num:Int=>rowSum.put(row,num+1)
      case _=>rowSum.put(row,1)
    }
  }

  def decCount(row: Int, col: Int){
    matrix.update(row,col,matrix(row,col)-1)
    rowSum.get(row) match{
      case num:Int=>rowSum.put(row,num-1)
      case _=>rowSum.put(row,0)
    }
  }

  def getCount(row: Int, col: Int):Int={
    matrix(row,col)
  }

  def getCountSum(row: Int):Int={
    rowSum.get(row)
  }


  def writeToFile( filePath:String){
    val writer=new BufferedWriter(new FileWriter(filePath))
    writer.write("%d x %d : %d  CSCMatrix\n".format(matrix.rows, matrix.cols, matrix.activeSize))
    matrix.activeIterator.foreach { case ((r,c),v) =>
      writer.write("%d %d %d\n".format(r,c,v.toString))
    }
    writer.flush()
    writer.close()
  }
}

object GlobalCounter{

  def apply(rows:Int, cols:Int):GlobalCounter={
    val matrix: CSCMatrix[Int]=CSCMatrix.zeros(rows,cols)
    val rowSum: HashMap[Int,Int]=new HashMap[Int, Int]()
    new GlobalCounter(matrix, rowSum)
  }

  def readFromFile(filePath:String):GlobalCounter={
    val rowSum: HashMap[Int,Int]=new HashMap[Int, Int]()

    val reader=new BufferedReader(new FileReader(filePath))
    val metaString=reader.readLine()
    val fields=metaString.split(" ")
    val rows=fields(0).toInt
    val cols=fields(2).toInt
    val size=fields(4).toInt
    val builder=new CSCMatrix.Builder[Int](rows,cols,size)
    (0 until size).foreach(_=>{
      val line=reader.readLine()
      val triple=line.split(" ")
      val row=triple(0).toInt
      builder.add(row, triple(1).toInt, triple(2).toInt)
      rowSum.get(row) match{
        case num:Int=>rowSum.put(row,num-1)
        case _=>rowSum.put(row,0)
      }
    })
    val matrix=builder.result()
    new GlobalCounter(matrix, rowSum)
  }
}
