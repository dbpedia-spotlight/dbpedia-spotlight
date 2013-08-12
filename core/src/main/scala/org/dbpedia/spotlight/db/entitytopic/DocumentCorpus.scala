package org.dbpedia.spotlight.db.entitytopic

import org.dbpedia.spotlight.db.entitytopic.Document
import com.esotericsoftware.kryo.io.{Output, Input}
import java.io._
import com.esotericsoftware.kryo.Kryo
import scala.collection.mutable.ListBuffer

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
    while((doc=Document.load(inputStream))!=null)
      retDocs+=doc
    retDocs.toArray
  }

  def closeInputStream(){
    inputStream.close()
    inputStream=null
  }
}
