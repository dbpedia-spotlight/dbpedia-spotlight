package org.dbpedia.spotlight.db.entitytopic

import java.util.{Properties, HashMap}
import java.util.Random
import Document._
import org.dbpedia.spotlight.db.memory.MemoryCandidateMapStore
import java.lang.Math


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
class Document (val mentions:Array[Int],
                val words:Array[Int],
                val entityOfMention:Array[Int],
                val topicOfMention:Array[Int],
                val entityOfWord:Array[Int],
                val topicCount:HashMap[Int,Int],
                val entityForMentionCount:HashMap[Int,Int],
                val entityForWordCount:HashMap[Int,Int]) extends  Serializable{

  def updateAssignment(){
    for(i<-0 until mentions.size){
      val mention=mentions(i)
      var topic=topicOfMention(i)
      var entity=entityOfMention(i)

      decCount(topicCount, topic)
      topicentityCount.decCount(topic,entity)
      topic=Document.sampleTopic(topicCount, entity)
      topicOfMention(i)=topic
      incCount(topicCount, topic)


      decCount(entityForMentionCount, entity)
      entitymentionCount.decCount(entity, mention)
      entity=Document.sampleEntityForMention(entityForMentionCount, entityForWordCount,topic,mentions(i))
      entityOfMention(i)=entity
      incCount(entityForMentionCount, entity)
      entitymentionCount.incCount(entity, mention)
      topicentityCount.incCount(topic, entity)
    }

    for(i<-0 until words.size){
      val word=words(i)
      var entity=entityOfWord(i)

      decCount(entityForWordCount, entity)
      entity=Document.sampleEntityForWord(word,entityForMentionCount,entityOfMention)
      incCount(entityForWordCount,entity)
      entitywordCount.incCount(entity,word)
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
      case a: Int=>map.put(key,a-1)
    }
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

  def init(_topicentityCount:GlobalCounter, _entitymentionCount:GlobalCounter, _entitywordCount:GlobalCounter, candmapStore:MemoryCandidateMapStore, properties: Properties){
    topicentityCount=_topicentityCount
    entitymentionCount=_entitymentionCount
    entitywordCount=_entitywordCount

    alpha=properties.getProperty("alpha").toFloat
    beta=properties.getProperty("beta").toFloat
    gama=properties.getProperty("gama").toFloat
    delta=properties.getProperty("delta").toFloat

    T=properties.getProperty("topicNum").toFloat
    E=properties.getProperty("resourceNum").toFloat
    K=properties.getProperty("surfaceNum").toFloat
    V=properties.getProperty("tokenNum").toFloat

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
    }while(threshold>=cumulative)

    id(i-1)
  }

  def sampleTopic(topicCount:HashMap[Int,Int], entity: Int):Int={
    val probs: Array[Float]=topics.map((t:Int)=>(topicCount.get(t)+alpha)*(topicentityCount.getCount(t,entity)+beta)/topicentityCount.getCountSum(t)+E*beta).toArray
    multinomialSample(probs, topics)
  }

  def sampleEntityForMention(docEntityForMentionCount:HashMap[Int,Int], docEntityForWordCount: HashMap[Int,Int], topic:Int, mention: Int):Int={
    val candidates:Array[Int]=candmap(mention)
    val probs: Array[Float]=candidates.map((cand:Int)=>{
      val first:Float=topicentityCount.getCount(topic,mention)+beta
      val second:Float=(entitymentionCount.getCount(cand,mention)+gama)/(entitymentionCount.getCountSum(cand)+K*gama)
      val third:Float=Math.pow((1/docEntityForMentionCount.get(cand)+1.0).toDouble,docEntityForWordCount.get(cand).toDouble).toFloat
      first*second*third
    })
    multinomialSample(probs, candidates)
  }

  def sampleEntityForWord(word:Int, docEntityForMentionCount:HashMap[Int,Int], entities: Array[Int]):Int={
    val probs:Array[Float]=entities.map((entity:Int)=>{
      val first:Float=docEntityForMentionCount.get(entity).toFloat
      val second:Float=(entitywordCount.getCount(entity, word)+delta)/(entitywordCount.getCountSum(entity)+V*delta)
      first*second
    })

    multinomialSample(probs, entities)
  }

}
