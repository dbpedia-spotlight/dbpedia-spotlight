package org.dbpedia.spotlight.db.entitytopic

import scala.collection.mutable.ListBuffer
import org.dbpedia.spotlight.spot.Spotter
import org.dbpedia.spotlight.db.DBCandidateSearcher
import org.dbpedia.spotlight.db.model.TextTokenizer
import org.dbpedia.spotlight.model._
import opennlp.tools.util.Span
import java.util.{Properties, HashMap}
import scala.util.Random

class DocumentInitializer(val topicentityCount:GlobalCounter,
                          val entitymentionCount:GlobalCounter,
                          val entitywordCount:GlobalCounter,
                          val tokenizer:TextTokenizer,
                          val searcher: DBCandidateSearcher,
                          val topicNum:Int,
                          val MaxSurfaceformLength:Int
) extends Runnable{


  val documents:ListBuffer[Document]=new ListBuffer[Document]()
  var isRunning:Boolean=false

  def incCount(map:HashMap[Int,Int],key:Int){
    map.get(key) match{
      case a: Int=>map.put(key,a+1)
      case _ =>map.put(key,1)
    }
  }

  def restrictedSpot(text:Text, surfaceOccrs:Array[SurfaceFormOccurrence]):Array[SurfaceFormOccurrence]={
    val docStr=text.text
    val sfMap=new collection.mutable.HashMap[String, SurfaceForm]()
    surfaceOccrs.foreach((sfOccr)=>{
       sfMap.put(sfOccr.surfaceForm.name, sfOccr.surfaceForm)
    })

    val tokens:List[Token]=text.featureValue[List[Token]]("tokens").get
    val tokenNum=tokens.size
    val mentions=new ListBuffer[SurfaceFormOccurrence]()
    tokens.zipWithIndex.foreach{case (token, startIndex)=>{
      if (tokenNum<=startIndex+MaxSurfaceformLength){
        val endIndex=tokenNum
        var k=0
        for(k<-startIndex until endIndex){
          val endOffset=if (k+1==tokenNum) docStr.length else tokens(k+1).offset
          val gram=docStr.substring(token.offset, endOffset).trim
          sfMap.get(gram) match{
            case Some(sf)=>mentions+=new SurfaceFormOccurrence(sf,null,token.offset)
            case None=>{}
          }
        }
      }else{
        var k=0
        val endIndex=startIndex+MaxSurfaceformLength
        for(k<-startIndex until endIndex){
          val gram=docStr.substring(token.offset, tokens(k+1).offset)
          sfMap.get(gram) match{
            case Some(sf)=>mentions+=new SurfaceFormOccurrence(sf,null,token.offset)
            case None=>{}
          }
        }
      }

    }}
    mentions.toArray
  }

  def spotterDebug(text:Text,surfaceOccr:Array[SurfaceFormOccurrence], spans:Array[Span])={
    var pos=0
    val docStr=text.text
    var k=0
    while(k<surfaceOccr.length){
      val sf:SurfaceFormOccurrence=surfaceOccr(k)
      val curPos=sf.textOffset
      System.out.print(docStr.substring(pos,curPos)+"<")
      pos=curPos+sf.surfaceForm.name.length
      System.out.print(docStr.substring(curPos,pos)+">")
      k+=1
    }

    System.out.println("\n wiki annotation")
    pos=0
    spans.map((span:Span)=>{
      val curPos=span.getStart
      System.out.print(docStr.substring(pos,curPos)+"[")
      pos=span.getEnd
      System.out.print(docStr.substring(curPos,pos)+"]")
    })
  }


  var text:Text=null
  var goldResources: Array[DBpediaResource]=null
  var goldSurfaceOccrs:Array[SurfaceFormOccurrence]=null


  def initDocument(text:Text, resources: Array[DBpediaResource], surfaces:Array[SurfaceFormOccurrence]):Document={
    set(text, resources, surfaces)
    run()
    documents.last
  }

  def set(t:Text, resources: Array[DBpediaResource], surfaceOccrs:Array[SurfaceFormOccurrence]){
    text=t
    goldResources=resources
    goldSurfaceOccrs=surfaceOccrs
    isRunning=true
  }

  def run(){
    val mentions=new ListBuffer[SurfaceFormOccurrence]()
    val words:ListBuffer[Int]=new ListBuffer[Int]()
    val entityOfMention:ListBuffer[Int]=new ListBuffer[Int]()
    val topicOfMention: ListBuffer[Int]=new ListBuffer[Int]()
    val entityOfWord:ListBuffer[Int]=new ListBuffer[Int]()

    val topicCount:HashMap[Int,Int]=new HashMap[Int,Int]()
    val entityForMentionCount:HashMap[Int,Int]=new HashMap[Int,Int]()
    val entityForWordCount:HashMap[Int,Int]=new HashMap[Int,Int]()

    tokenizer.tokenizeMaybe(text)
    val tokens:List[Token]=text.featureValue[List[Token]]("tokens").get
    val surfaceOccrs:Array[SurfaceFormOccurrence]=restrictedSpot(text,goldSurfaceOccrs)

    //spotterDebug(text, surfaceOccr, spans)
    /*tokens and mentions within two succinct link anchors are processed by associating a token
     *with the nearest anchor's entity, and sampling an entity for a mention based on its entity distribution
     */
    var i=0
    var j=0
    var prevRes=goldResources(0)
    var prevOffset=0
    (goldResources,goldSurfaceOccrs).zipped.foreach((res:DBpediaResource,sfOccr:SurfaceFormOccurrence)=>{
      //for tokens
      while(i<tokens.length && tokens(i).offset<sfOccr.textOffset){
        words+=tokens(i).tokenType.id
        if(sfOccr.textOffset-tokens(i).offset>tokens(i).offset-prevOffset)
          entityOfWord+=prevRes.id
        else entityOfWord+=res.id

        incCount(entityForWordCount,entityOfWord.last)
        entitywordCount.incCount(entityOfWord.last, words.last)

        prevRes=res
        prevOffset=sfOccr.textOffset
        i+=1
      }

      //tokens of the link anchor are assigned with the link's target entity
      while(i<tokens.length && tokens(i).offset<sfOccr.textOffset+sfOccr.surfaceForm.name.length){
        words+=tokens(i).tokenType.id
        entityOfWord+=res.id
        incCount(entityForWordCount,res.id)
        entitywordCount.incCount(entityOfWord.last, words.last)
        i+=1
      }

      //for mentions
      while(j<surfaceOccrs.length &&  surfaceOccrs(j).textOffset<sfOccr.textOffset){
        val cands=searcher.getCandidates(surfaceOccrs(j).surfaceForm)
        if(cands.size>0){
          mentions+=surfaceOccrs(j)
          topicOfMention+=DocumentInitializer.RandomGenerator.nextInt(topicNum)
          entityOfMention+=Document.multinomialSample(cands.map((cand:Candidate)=>cand.support.asInstanceOf[Float]).toArray, cands.map((cand:Candidate)=>cand.resource.id).toArray)

          incCount(topicCount,topicOfMention.last)
          incCount(entityForMentionCount,entityOfMention.last)
          topicentityCount.incCount(topicOfMention.last, entityOfMention.last)
          entitymentionCount.incCount(entityOfMention.last, mentions.last.surfaceForm.id)
          }
        j+=1
      }

      //mention of the link anchor are assigned with the link's target entity
      if(searcher.getCandidates(sfOccr.surfaceForm).size>0){
        mentions+=sfOccr
        entityOfMention+=res.id
        topicOfMention+=DocumentInitializer.RandomGenerator.nextInt(topicNum)

        incCount(topicCount,topicOfMention.last)
        incCount(entityForMentionCount,entityOfMention.last)
        topicentityCount.incCount(topicOfMention.last, entityOfMention.last)
        entitymentionCount.incCount(entityOfMention.last, mentions.last.surfaceForm.id)
      }
    })

    //for the tokens after the last link anchor
    while(i<tokens.length){
      words+=tokens(i).tokenType.id
      entityOfWord+=goldResources.last.id

      incCount(entityForWordCount,goldResources.last.id)
      entitywordCount.incCount(entityOfWord.last, words.last)
      i+=1
    }

    //for the mentions after the last link anchor
    while(j<surfaceOccrs.length){
      val cands=searcher.getCandidates(surfaceOccrs(j).surfaceForm)
      if(cands.size>0){
        mentions+=surfaceOccrs(j)
        topicOfMention+=DocumentInitializer.RandomGenerator.nextInt(topicNum)

        entityOfMention+=Document.multinomialSample(cands.map((cand:Candidate)=>cand.support.asInstanceOf[Float]).toArray, cands.map((cand:Candidate)=>cand.resource.id).toArray)
        incCount(topicCount,topicOfMention.last)
        incCount(entityForMentionCount,entityOfMention.last)

        topicentityCount.incCount(topicOfMention.last, entityOfMention.last)
        entitymentionCount.incCount(entityOfMention.last, mentions.last.surfaceForm.id)
      }
      j+=1
    }

    documents+=new Document(mentions.toArray,words.toArray,entityOfMention.toArray,topicOfMention.toArray,entityOfWord.toArray, topicCount,entityForMentionCount,entityForWordCount)
    isRunning=false
  }

}


object DocumentInitializer{
  val RandomGenerator=new Random();
  def apply(tokenizer:TextTokenizer, searcher: DBCandidateSearcher, properties:Properties):DocumentInitializer={
    val T=properties.getProperty("topicNum").toFloat
    val E=properties.getProperty("resourceNum").toFloat
    val K=properties.getProperty("surfaceNum").toFloat
    val V=properties.getProperty("tokenNum").toFloat

    val topicentityCount=GlobalCounter(T.toInt+1,E.toInt+1)
    val entitymentionCount=GlobalCounter(E.toInt+1,K.toInt+1)
    val entitywordCount=GlobalCounter(E.toInt+1,V.toInt+1)

    val topicNum=properties.getProperty("topicNum").toInt
    val maxSurfaceformLen=properties.getProperty("maxSurfaceformLen").toInt
    new DocumentInitializer(topicentityCount,entitymentionCount,entitywordCount,tokenizer,searcher,topicNum,maxSurfaceformLen)
  }
}