package org.dbpedia.spotlight.db.entityTopic

import org.dbpedia.spotlight.db.entitytopic.Document
import com.esotericsoftware.kryo.io.{Output, Input}

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 9/8/13
 * Time: 3:26 PM
 * To change this template use File | Settings | File Templates.
 */
class DocumentCorpus (val diskPath:String, val capacity:Int){

  val docs=new Array[Document](capacity)
  val size=0
  val total=0

  var outStream:Output=null
  var inputStream:Input=null

  def this(val diskPath){
    this(diskPath, 10000)
  }

  def add(doc:Document){
    if (size+1==capacity)
      saveDocs()

    docs(size)=doc
    size+=1
  }

  def saveDocs() {
    if(outStream==null)
      outStream=new Output(new FileOutputStream(diskPath))
    total+=size
  }

  def loadDocs():Array[Document]={
    if(inputStream==null)
      inputStream=new Input(new FileInputStream(diskPath))

    val retDocs=new Array[Document](total)
    /*
    TDO: read docs
     */
    retDocs
  }

}
