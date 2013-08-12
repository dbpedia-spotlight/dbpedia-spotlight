package org.dbpedia.spotlight.db.entitytopic

import java.util.{Properties, HashMap}
import java.util.Random
import Document._
import org.dbpedia.spotlight.db.memory.MemoryCandidateMapStore
import java.lang.Math
import scala.collection.JavaConverters._
import org.dbpedia.spotlight.model.{SurfaceForm, DBpediaResourceOccurrence, SurfaceFormOccurrence}
import java.io.{BufferedReader, BufferedWriter}
import scala.Array
import org.dbpedia.spotlight.model.Factory.SurfaceFormOccurrence

/**
 *
 * @param mentions
 * @param words
 * @param entityOfMention
 * @param topicOfMention
 * @param entityOfWord
 * @param topicCount
 * @param entityForMentionCount count of entity e being assigned to mention in this document
 * @param entityForWordCount count of entity e being assigned to word in this document
 */
class Document(val mentions:Array[SurfaceFormOccurrence],
               val words:Array[Int],
               val entityOfMention:Array[Int],
               val topicOfMention:Array[Int],
               val entityOfWord:Array[Int],
               val topicCount:HashMap[Int,Int],
               val entityForMentionCount:HashMap[Int,Int],
               val entityForWordCount:HashMap[Int,Int]
) extends  Serializable{

  /**
   * update document's assignments: topic for each mention, entity for each mention, entity for each word
   * each assignment is updated: dec count, sample new assignment, inc count
   *
   */
  def updateAssignment(training:Boolean){

    if(training){
      for(i<-0 until mentions.size){
        val mention=mentions(i).surfaceForm.id
        var topic=topicOfMention(i)
        var entity=entityOfMention(i)

        //update topic for mention
        decCount(topicCount, topic)
        topicentityCount.decCount(topic,entity)
        topic=Document.sampleTopic(topicCount, entity)
        topicOfMention(i)=topic
        incCount(topicCount, topic)

        //update entity for mention
        decCount(entityForMentionCount, entity)
        entitymentionCount.decCount(entity, mention)
        entity=Document.sampleEntityForMention(entityForMentionCount, entityForWordCount,topic,mentions(i).surfaceForm.id)
        entityOfMention(i)=entity
        incCount(entityForMentionCount, entity)
        entitymentionCount.incCount(entity, mention)
        topicentityCount.incCount(topic, entity)
      }

      //update words' assignments
      for(i<-0 until words.size){
        val word=words(i)
        var entity=entityOfWord(i)

        decCount(entityForWordCount, entity)
        entitywordCount.decCount(entity,word)
        entity=Document.sampleEntityForWord(word,entityForMentionCount)
        entityOfWord(i)=entity
        incCount(entityForWordCount,entity)
        entitywordCount.incCount(entity,word)
      }
    }else{
      for(i<-0 until mentions.size){
        val mention=mentions(i).surfaceForm.id
        var topic=topicOfMention(i)
        var entity=entityOfMention(i)

        //update topic for mention
        decCount(topicCount, topic)
        topic=Document.sampleTopic(topicCount, entity)
        topicOfMention(i)=topic
        incCount(topicCount, topic)

        //update entity for mention
        decCount(entityForMentionCount, entity)
        entity=Document.sampleEntityForMention(entityForMentionCount, entityForWordCount,topic,mention)
        entityOfMention(i)=entity
        incCount(entityForMentionCount, entity)
      }

      //update words' assignments
      for(i<-0 until words.size){
        val word=words(i)
        var entity=entityOfWord(i)

        decCount(entityForWordCount, entity)
        entity=Document.sampleEntityForWord(word,entityForMentionCount)
        entityOfWord(i)=entity
        incCount(entityForWordCount,entity)
      }
    }
  }

  def incCount(map:HashMap[Int,Int],key:Int){
    map.get(key) match{
      case a: Int=>map.put(key,a+1)
      case _ =>map.put(key,1)
    }
  }

  def decCount(map:HashMap[Int,Int],key:Int){
    map.get(key) match{
      case a: Int=>if (a>0) map.put(key,a-1) else throw new IllegalArgumentException("dec empty entry of doc counter")
      case _=> throw new IllegalArgumentException("dec null entry of doc counter")
    }
  }

  def saveIntArray(out:BufferedWriter, content:Array[Int]){
    out.write(content.length.toString)
    out.write(":")
    content.foreach((k:Int)=>{
      out.write(k.toString)
      out.write(",")
    })
    out.write("\t")
  }

  def saveHashMap(out:BufferedWriter, map:HashMap[Int,Int]){
    var num=0
    map.values().asScala.foreach((v:Int)=> {
      if (v>0)
        num+=1
    })
    out.write(num.toString)
    out.write(":")
    map.entrySet().asScala.foreach((entry)=>{
      if(entry.getValue>0){
        out.write(entry.getKey.toString)
        out.write("=")
        out.write(entry.getValue.toString)
        out.write(",")
      }
    })
    out.write("\t")
  }

  def saveSfOccrArray(out:BufferedWriter, sfoccrs:Array[SurfaceFormOccurrence]){
    out.write(sfoccrs.length.toString)
    //out.write(":")
    sfoccrs.foreach((sfoccr:SurfaceFormOccurrence)=>{
      out.write("["+sfoccr.surfaceForm.name+"]")
      out.write(sfoccr.surfaceForm.id.toString)
      out.write(" ")
      out.write(sfoccr.textOffset.toString)
    })
    out.write("\t")
  }

  def save(out:BufferedWriter){
    saveSfOccrArray(out,mentions)
    saveIntArray(out,words)
    saveIntArray(out,entityOfMention)
    saveIntArray(out,topicOfMention)
    saveIntArray(out,entityOfWord)
    saveHashMap(out,topicCount)
    saveHashMap(out,entityForMentionCount)
    saveHashMap(out,entityForWordCount)
    out.newLine()
  }

}

object Document{
  var candmap: Array[Array[Int]]=null
  var topicentityCount:GlobalCounter=null
  var entitymentionCount:GlobalCounter=null
  var entitywordCount:GlobalCounter=null

  var alpha: Float=0f
  var beta: Float=0f
  var gama: Float=0f
  var delta: Float=0f

  var T: Float=0f
  var E: Float=0f
  var K: Float=0f
  var V: Float=0f
  var topics:Array[Int]=null

  val RandomGenerator=new Random()

  def loadSfoccrArray(str:String):Array[SurfaceFormOccurrence]={
    val fields=str.split("[\\[\\]]")
    val num=fields(0).toInt
    //val sfoccrs=new Array[SurfaceFormOccurrence](num)
    var k=0
    val sfoccrs=(0 until num).map((i:Int)=>{
      val sfName=fields(k+1)
      val idoffsetstr=fields(k+2).split(" ")
      val id=idoffsetstr(0).toInt
      val textoffset=idoffsetstr(1).toInt
      k+=2
      new SurfaceFormOccurrence(new SurfaceForm(sfName,id,0,0),null,textoffset)
    }).toArray
    sfoccrs
  }

  def loadIntArray(str:String):Array[Int]={
    val fields=str.split("[:,]")
    val num=fields(0).toInt
    var k=0
    val retArray=(0 until num).map((i:Int)=>{
      k+=1
      fields(k).toInt
    }).toArray
    retArray
  }

  def loadHashMap(str:String):HashMap[Int,Int]={
    val fields=str.split("[:=,]")
    val num=fields(0).toInt
    var k=0
    val map=new HashMap[Int,Int](num*2)
    (0 until num).foreach(_=>{
      val key=fields(k+1).toInt
      val value=fields(k+2).toInt
      k+=2
      map.put(key,value)
    })
    map
  }

  def load(in:BufferedReader):Document={
    val line=in.readLine()
    if (line!=null){
      val fields=line.split("\t")
      val mentions=loadSfoccrArray(fields(0))
      val words=loadIntArray(fields(1))
      val entityOfMention=loadIntArray(fields(2))
      val topicOfMention=loadIntArray(fields(3))
      val entityOfWord=loadIntArray(fields(4))
      val topicCount=loadHashMap(fields(5))
      val entityForMentionCount=loadHashMap(fields(6))
      val entityForWordCount=loadHashMap(fields(7))
      new Document(mentions,words,entityOfMention,topicOfMention,entityOfWord,topicCount,entityForMentionCount,entityForWordCount)
    }else{
      null
    }
  }

  def init(candmapStore:MemoryCandidateMapStore, properties: Properties){
    alpha=properties.getProperty("alpha").toFloat
    beta=properties.getProperty("beta").toFloat
    gama=properties.getProperty("gama").toFloat
    delta=properties.getProperty("delta").toFloat

    T=properties.getProperty("topicNum").toFloat
    E=properties.getProperty("resourceNum").toFloat
    K=properties.getProperty("surfaceNum").toFloat
    V=properties.getProperty("tokenNum").toFloat

    topicentityCount=GlobalCounter("topicentity_count",T.toInt+1,E.toInt+1)
    entitymentionCount=GlobalCounter("entitymention_count",E.toInt+1,K.toInt+1)
    entitywordCount=GlobalCounter("entityword_count",E.toInt+1,V.toInt+1)

    topics=(0 until T.toInt).toArray
    candmap=candmapStore.candidates
  }

  def init( entitymention:GlobalCounter, entityword:GlobalCounter,topicentity:GlobalCounter, candmapStore:MemoryCandidateMapStore, properties: Properties){
    alpha=properties.getProperty("alpha").toFloat
    beta=properties.getProperty("beta").toFloat
    gama=properties.getProperty("gama").toFloat
    delta=properties.getProperty("delta").toFloat

    T=properties.getProperty("topicNum").toFloat
    E=properties.getProperty("resourceNum").toFloat
    K=properties.getProperty("surfaceNum").toFloat
    V=properties.getProperty("tokenNum").toFloat

    topicentityCount=topicentity
    entitymentionCount=entitymention
    entitywordCount=entityword

    topics=(0 until T.toInt).toArray
    candmap=candmapStore.candidates
  }

  def multinomialSample(prob:Array[Float], id:Array[Int]):Int={
    val sum=prob.foldLeft(0.0f)((a,b)=>a+b)
    val threshold=RandomGenerator.nextFloat()*sum
    var cumulative=0.0f
    var i=0;

    do{
      cumulative+=prob(i)
      i+=1
    }while(threshold>cumulative && prob.length>i)

    id(i-1)
  }

  def sampleTopic(topicCount:HashMap[Int,Int], entity: Int):Int={
    val probs: Array[Float]=topics.map((t:Int)=>{
      val first=topicCount.get(t)+alpha//no need for the denominator
      val second=(topicentityCount.getCount(t,entity)+beta)/(topicentityCount.getCountSum(t)+E*beta)
      first*second
    }).toArray

    multinomialSample(probs, topics)
  }

  def sampleEntityForMention(docEntityForMentionCount:HashMap[Int,Int], docEntityForWordCount: HashMap[Int,Int], topic:Int, mention: Int):Int={
    val candidates:Array[Int]=candmap(mention)

    val probs: Array[Float]=candidates.map((cand:Int)=>{
      val first:Float=(topicentityCount.getCount(topic,cand)+beta)// (topicentityCount.getCountSum(topic)+E*beta)
      val second:Float=(entitymentionCount.getCount(cand,mention)+gama)/(entitymentionCount.getCountSum(cand)+K*gama)
      val third:Float=if(docEntityForMentionCount.get(cand)==0) 1.0f else Math.pow((1/docEntityForMentionCount.get(cand)+1.0).toDouble,docEntityForWordCount.get(cand).toDouble).toFloat
      first*second*third
    })
    multinomialSample(probs, candidates)
  }

  def sampleEntityForWord(word:Int, docEntityForMentionCount:HashMap[Int,Int]):Int={
    val entities=docEntityForMentionCount.keySet().asScala.toArray
    val probs:Array[Float]=entities.map((entity:Int)=>{
      val first:Float=docEntityForMentionCount.get(entity).toFloat
      val second:Float=(entitywordCount.getCount(entity, word)+delta)/(entitywordCount.getCountSum(entity)+V*delta)
      first*second
    })

    multinomialSample(probs, entities)
  }
}
