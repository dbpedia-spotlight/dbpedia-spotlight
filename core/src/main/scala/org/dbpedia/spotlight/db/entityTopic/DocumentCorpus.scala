package org.dbpedia.spotlight.db.entitytopic

import org.dbpedia.spotlight.db.entitytopic.Document
import com.esotericsoftware.kryo.io.{Output, Input}
import java.io.{FileInputStream, FileOutputStream}
import com.esotericsoftware.kryo.Kryo

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

  var outputStream:Output=null
  var inputStream:Input=null

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
      outputStream=new Output(new FileOutputStream(diskPath))
    val kryo=new Kryo()
    kryo.setRegistrationRequired(false)
    (0 until size).foreach((i:Int)=>{
      val doc=docs(i)
      kryo.writeClassAndObject(outputStream, doc)
    })
    size=0
  }

  def closeOutputStream(){
    if(size<total){
      saveDocs()
      outputStream.flush()
      outputStream.close()
    }
    outputStream=null
  }

  def loadDocs():Array[Document]={
    if(size==total)
      docs
    else{
      if(inputStream==null)
        inputStream=new Input(new FileInputStream(diskPath))

      val kryo=new Kryo()
      kryo.setRegistrationRequired(false)
      val retDocs=new Array[Document](total)
      (0 until total).foreach((i:Int)=>{
        retDocs(i)= kryo.readClassAndObject(inputStream).asInstanceOf[Document]
      })
      closeInputStream()
      retDocs
    }
  }

  def closeInputStream(){
    inputStream.close()
    inputStream=null
  }
}
