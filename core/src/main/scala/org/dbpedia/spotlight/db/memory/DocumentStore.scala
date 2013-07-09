package org.dbpedia.spotlight.db.memory

import com.esotericsoftware.kryo.io.Output
import java.io.{FileInputStream, FileOutputStream}
import org.dbpedia.spotlight.db.memory.MemoryStore
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.{Output, Input}
import org.dbpedia.spotlight.db.entitytopic.Document


class DocumentStore {
  var output:Output =null
  var input: Input=null
  var kryo: Kryo=null

  val filename:String="documentStore.dat"

  def initSave(){
    output= new Output(new FileOutputStream(filename))
    kryo=MemoryStore.kryos.get(classOf[Document].getSimpleName).get
  }
  def save(doc:Document){
    kryo.writeClassAndObject(output,doc)
  }

  def finishSave(){
    output.close()
  }

  def initLoad(){
    input= new Input(new FileInputStream(filename))
    kryo=MemoryStore.kryos.get(classOf[Document].getSimpleName).get
  }

  def load():Document={
    val doc=kryo.readClassAndObject(input)
    doc.asInstanceOf[Document]
  }

  def finishLoad(){
    input.close()
  }
}
