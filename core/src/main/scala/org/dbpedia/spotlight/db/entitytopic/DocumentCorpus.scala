package org.dbpedia.spotlight.db.entitytopic

import java.io._
import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks._

/**
 * DocumentCorpus is to store intermedia documents during training
 * each doc corpus hold a buffer of docs, save it to disk automatically when full
 * @param diskPath
 * @param capacity
 */
class DocumentCorpus (val diskPath:String, val capacity:Int) extends Runnable{

  val docs=new ListBuffer[Document]()
  var size:Int=0
  var total:Int=0

  var outputStream:BufferedWriter=null
  var inputStream:BufferedReader=null
  var isRunning=false

  def this(diskPath:String){
    this(diskPath, 10000)
  }

  def add(doc:Document){
    if (size==capacity)
      saveDocs()

    docs+=doc
    size+=1
    total+=1
  }

  def saveDocs() {
    if(outputStream==null)
      outputStream=new BufferedWriter(new FileWriter(diskPath))

    (0 until size).foreach((i:Int)=>{
      docs(i).save(outputStream)
    })
    docs.clear()
    size=0
  }

  def closeOutputStream(){
    saveDocs()
    outputStream.flush()
    outputStream.close()
    outputStream=null
  }

  /**
   * docs are loaded during assignments update, where only one thread is running,
   * so all docs in the corpus are loaded.
   *
   * @return
   */
  def loadDocs():ListBuffer[Document]={
    if(inputStream==null)
      inputStream=new BufferedReader(new FileReader(diskPath))

    var doc:Document=null
    var num=0
    //val LOG = LogFactory.getLog(this.getClass)
    breakable{
      while(true){
        doc=Document.load(inputStream)
        if(doc==null)
          break
        docs+=doc
        num+=1
      }
    }
    closeInputStream()
    docs
  }

  /**
   * dump docs onto disk
   */
  def dump()={
    if(outputStream==null)
      outputStream=new BufferedWriter(new FileWriter(diskPath))

    docs.foreach(doc=>
      doc.save(outputStream)
    )
    outputStream.flush()
    outputStream.close()
    outputStream=null
  }

  def closeInputStream(){
    inputStream.close()
    inputStream=null
  }

  /**
   * run updates for docs in this corpus
   */
  def run{
    isRunning=true
    docs.foreach((doc:Document)=>{
      doc.updateAssignment(true)
    })
    isRunning=false
  }
}
