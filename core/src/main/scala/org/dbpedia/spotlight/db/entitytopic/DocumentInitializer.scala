package org.dbpedia.spotlight.db.entitytopic

import scala.collection.mutable.ListBuffer
import org.dbpedia.spotlight.db.DBCandidateSearcher
import org.dbpedia.spotlight.db.model.TextTokenizer
import org.dbpedia.spotlight.model._
import opennlp.tools.util.Span
import java.util.{Properties, HashMap}
import scala.util.Random
import org.dbpedia.spotlight.model.Factory.DBpediaResourceOccurrence
import org.dbpedia.spotlight.exceptions.SurfaceFormNotFoundException


class DocumentInitializer(val topicentityCount:GlobalCounter,
                          val entitymentionCount:GlobalCounter,
                          val entitywordCount:GlobalCounter,
                          val topicNum:Int,
                          val docCorpusFile:String,
                          val isTraning:Boolean=false
) extends Runnable{

  val docCorpus=new DocumentCorpus(docCorpusFile)
  var newestDoc:Document=null
  var isRunning:Boolean=false

  def incCount(map:HashMap[Int,Int],key:Int){
    map.get(key) match{
      case a: Int=>map.put(key,a+1)
      case _ =>map.put(key,1)
    }
  }

  var text:Text=null
  var resourceOccrs: Array[DBpediaResourceOccurrence]=null

  /**
   *
   * @param t the text should be already tokenized
   * @param resOccrs
   * @return
   */
  def initDocument(t:Text, resOccrs:Array[DBpediaResourceOccurrence]):Document={
    //set(text, resourceOccrs)
    text=t
    resourceOccrs=resOccrs
    run()
    newestDoc
  }

  /**
   * set text content and dbpedia resource occr
   * the document will be initialized later by calling run
   *
   * @param t the text should be already tokenized
   * @param resOccrs
   */
  def set(t:Text, resOccrs:Array[DBpediaResourceOccurrence]){
    text=t
    resourceOccrs=resOccrs
    isRunning=true
  }

  def run(){
    val mentions=new ListBuffer[SurfaceFormOccurrence]()
    val words:ListBuffer[Int]=new ListBuffer[Int]()
    val mentionEntities:ListBuffer[Int]=new ListBuffer[Int]()
    val topics: ListBuffer[Int]=new ListBuffer[Int]()
    val wordEntities:ListBuffer[Int]=new ListBuffer[Int]()

    val topicCount:HashMap[Int,Int]=new HashMap[Int,Int]()
    val entityForMentionCount:HashMap[Int,Int]=new HashMap[Int,Int]()
    val entityForWordCount:HashMap[Int,Int]=new HashMap[Int,Int]()

    //tokenizer.tokenizeMaybe(text)
    val tokens:List[Token]=text.featureValue[List[Token]]("tokens").get.filter((token:Token)=>token.tokenType.id>0)

    var i=0
    var prevRes=resourceOccrs(0).resource
    var prevOffset=0

    (resourceOccrs).foreach((resOccr:DBpediaResourceOccurrence)=>{
      val offset=resOccr.textOffset
      val res=resOccr.resource

      //tokens and mentions within two succinct link anchors are processed by associating a token with the nearest anchor's entity
      while(i<tokens.length && tokens(i).offset<offset){
        words+=tokens(i).tokenType.id
        if(offset-tokens(i).offset>tokens(i).offset-prevOffset)
          wordEntities+=prevRes.id
        else wordEntities+=res.id
        incCount(entityForWordCount,wordEntities.last)
        if(isTraning)
          entitywordCount.incCount(wordEntities.last, words.last)
        i+=1
      }

      //tokens of the link anchor are assigned with the link's target entity
      while(i<tokens.length && tokens(i).offset<offset+resOccr.surfaceForm.name.length){
        words+=tokens(i).tokenType.id
        wordEntities+=res.id
        incCount(entityForWordCount,wordEntities.last)
        if(isTraning)
          entitywordCount.incCount(wordEntities.last, words.last)
        i+=1
      }

      //mention of the link anchor are assigned with the link's target entity
      //if(searcher.getCandidates(resOccr.surfaceForm).size>0){
      mentions+=new SurfaceFormOccurrence(resOccr.surfaceForm, resOccr.context, offset)
      mentionEntities+=res.id
      topics+=DocumentInitializer.RandomGenerator.nextInt(topicNum)

      incCount(topicCount,topics.last)
      incCount(entityForMentionCount,mentionEntities.last)
      if(isTraning){
        topicentityCount.incCount(topics.last,mentionEntities.last)
        entitymentionCount.incCount(mentionEntities.last, mentions.last.surfaceForm.id)
      }
      //}
      prevRes=res
      prevOffset=offset
    })

    //for the tokens after the last link anchor
    while(i<tokens.length){
      words+=tokens(i).tokenType.id
      wordEntities+=resourceOccrs.last.resource.id
      incCount(entityForWordCount,wordEntities.last)
      if(isTraning)
        entitywordCount.incCount(wordEntities.last, words.last)
      i+=1
    }

    newestDoc=new Document(mentions.toArray,words.toArray,mentionEntities.toArray,topics.toArray,wordEntities.toArray, topicCount,entityForMentionCount,entityForWordCount)
    if(isTraning)
      docCorpus.add(newestDoc)
    isRunning=false
  }

}


object DocumentInitializer{
  val RandomGenerator=new Random();
  def apply(properties:Properties, docCorpusPath:String="", isTraining:Boolean=false):DocumentInitializer={
    val topicNum=properties.getProperty("topicNum").toInt

    if(isTraining){
      val T=properties.getProperty("topicNum").toFloat
      val E=properties.getProperty("resourceNum").toFloat
      val K=properties.getProperty("surfaceNum").toFloat
      val V=properties.getProperty("tokenNum").toFloat

      val topicentityCount=GlobalCounter("topicentity_count",T.toInt,1000)
      val entitymentionCount=GlobalCounter("entitymention_count",E.toInt)
      val entitywordCount=GlobalCounter("entityword_count",E.toInt)

      new DocumentInitializer(topicentityCount,entitymentionCount,entitywordCount,topicNum,docCorpusPath,true)
     }else{
       new DocumentInitializer(null,null,null,topicNum,docCorpusPath,isTraining)
    }
  }

}
