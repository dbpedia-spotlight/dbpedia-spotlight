package org.dbpedia.spotlight.db.entitytopic

import java.io._
import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks._

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 9/8/13
 * Time: 3:26 PM
 * To change this template use File | Settings | File Templates.
 */
class DocumentCorpus (val diskPath:String, val capacity:Int){

  val docs=new Array[Document](capacity)
  var size:Int=0
  var total:Int=0

  var outputStream:BufferedWriter=null
  var inputStream:BufferedReader=null

  def this(diskPath:String){
    this(diskPath, 1000)
  }

  def add(doc:Document){
    if (size==capacity)
      saveDocs()

    docs(size)=doc
    size+=1
    total+=1
  }

  def saveDocs() {
    if(outputStream==null)
      outputStream=new BufferedWriter(new FileWriter(diskPath))

    (0 until size).foreach((i:Int)=>{
      docs(i).save(outputStream)
    })
    size=0
  }

  def closeOutputStream(){
    saveDocs()
    outputStream.flush()
    outputStream.close()
    outputStream=null
  }

  def loadDocs():Array[Document]={
    if(inputStream==null)
      inputStream=new BufferedReader(new FileReader(diskPath))

    val retDocs=new ListBuffer[Document]()
    var doc:Document=null
    var num=0
    //val LOG = LogFactory.getLog(this.getClass)
    breakable{
      while(true){
        doc=Document.load(inputStream)
        if(doc==null)
          break
        retDocs+=doc
        num+=1
      }
    }
    closeInputStream()
    retDocs.toArray
  }

  def closeInputStream(){
    inputStream.close()
    inputStream=null
  }
}
