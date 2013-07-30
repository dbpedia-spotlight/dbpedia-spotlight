package org.dbpedia.spotlight.db.entitytopic

import scala.collection.mutable.ListBuffer
import org.dbpedia.spotlight.spot.Spotter
import org.dbpedia.spotlight.db.DBCandidateSearcher
import org.dbpedia.spotlight.db.model.TextTokenizer
import org.dbpedia.spotlight.model._
import opennlp.tools.util.Span
import java.util.{Properties, HashMap}
import scala.util.Random
import org.dbpedia.spotlight.model.Factory.DBpediaResourceOccurrence

class DocumentInitializer(val topicentityCount:GlobalCounter,
                          val entitymentionCount:GlobalCounter,
                          val entitywordCount:GlobalCounter,
                          val tokenizer:TextTokenizer,
                          val searcher: DBCandidateSearcher,
                          val topicNum:Int,
                          val MaxSurfaceformLength:Int,
                          val isTraning:Boolean=false
) extends Runnable{

  val documents:ListBuffer[Document]=new ListBuffer[Document]()
  var newestDoc:Document=null
  var isRunning:Boolean=false

  def incCount(map:HashMap[Int,Int],key:Int){
    map.get(key) match{
      case a: Int=>map.put(key,a+1)
      case _ =>map.put(key,1)
    }
  }

  def spot(text:String, tokens:List[Token], start:Int, end:Int, map:collection.mutable.HashMap[String, DBpediaResourceOccurrence]):ListBuffer[DBpediaResourceOccurrence]={
    val resOccrs=new ListBuffer[DBpediaResourceOccurrence]()
    (start until end).foreach(k=>{
      val maxLen=if (end-k>MaxSurfaceformLength) MaxSurfaceformLength else end-start
      (k until k+maxLen ).foreach(e=>{
        val gram=text.substring(tokens(k).offset, tokens(e).offset+tokens(e).token.length)
        map.get(gram) match{
          case Some(resOccr)=> resOccrs+=DBpediaResourceOccurrence.from(new SurfaceFormOccurrence(resOccr.surfaceForm, null, tokens(k).offset),resOccr.resource,0.0)
        }
      })
    })
    resOccrs
  }

  def restrictedSpot(text:Text, resOccrs:Array[DBpediaResourceOccurrence]):Array[DBpediaResourceOccurrence]={
    val sfMap=new collection.mutable.HashMap[String, DBpediaResourceOccurrence]()
    resOccrs.foreach((resOccr)=>{
       sfMap.put(resOccr.surfaceForm.name, resOccr)
    })

    val tokens:List[Token]=text.featureValue[List[Token]]("tokens").get
    val retResOccrs=new ListBuffer[DBpediaResourceOccurrence]()

    var k=0
    resOccrs.foreach((resOccr)=>{
      val start=k
      while(k<tokens.size&&tokens(k).offset<resOccr.textOffset)
        k+=1
      if(k<tokens.size&&k>start)
        retResOccrs++=spot(text.text, tokens, start, k, sfMap)
    })

    if(k<tokens.size)
      retResOccrs++=spot(text.text, tokens, k, tokens.size, sfMap)

    retResOccrs.toArray
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
  var resourceOccrs: Array[DBpediaResourceOccurrence]=null

  def initDocument(text:Text, resOccrs:Array[DBpediaResourceOccurrence]):Document={
    set(text, resourceOccrs)
    run()
    newestDoc
  }

  def set(t:Text, resOccrs:Array[DBpediaResourceOccurrence]){
    text=t
    resourceOccrs=resOccrs
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
    val tokens:List[Token]=text.featureValue[List[Token]]("tokens").get.filter((token:Token)=>token.tokenType.id>0)


    //spotterDebug(text, surfaceOccr, spans)
    /*tokens and mentions within two succinct link anchors are processed by associating a token
     *with the nearest anchor's entity, and sampling an entity for a mention based on its entity distribution
     */
    var i=0
    var prevRes=resourceOccrs(0).resource
    var prevOffset=0
    if(isTraning)
      resourceOccrs=restrictedSpot(text,resourceOccrs)
    else
      (resourceOccrs).foreach((resOccr:DBpediaResourceOccurrence)=>
        resOccr.surfaceForm.id=searcher.sfStore.getSurfaceForm(resOccr.surfaceForm.name).id
      )

    (resourceOccrs).foreach((resOccr:DBpediaResourceOccurrence)=>{
      val offset=resOccr.textOffset
      val res=resOccr.resource
      //for tokens
      while(i<tokens.length && tokens(i).offset<offset){
        words+=tokens(i).tokenType.id
        if(offset-tokens(i).offset>tokens(i).offset-prevOffset)
          entityOfWord+=prevRes.id
        else entityOfWord+=res.id

        incCount(entityForWordCount,entityOfWord.last)
        i+=1
      }

      //tokens of the link anchor are assigned with the link's target entity
      while(i<tokens.length && tokens(i).offset<offset+resOccr.surfaceForm.name.length){
        words+=tokens(i).tokenType.id
        entityOfWord+=res.id
        incCount(entityForWordCount,entityOfWord.last)
        i+=1
      }

      //mention of the link anchor are assigned with the link's target entity
      if(searcher.getCandidates(resOccr.surfaceForm).size>0){
        mentions+=new SurfaceFormOccurrence(resOccr.surfaceForm, null, offset)
        entityOfMention+=res.id
        topicOfMention+=DocumentInitializer.RandomGenerator.nextInt(topicNum)

        incCount(topicCount,topicOfMention.last)
        incCount(entityForMentionCount,entityOfMention.last)
      }
      prevRes=res
      prevOffset=offset
    })

    //for the tokens after the last link anchor
    while(i<tokens.length){
      words+=tokens(i).tokenType.id
      entityOfWord+=resourceOccrs.last.resource.id
      incCount(entityForWordCount,entityOfWord.last)
      i+=1
    }

    newestDoc=new Document(mentions.toArray,words.toArray,entityOfMention.toArray,topicOfMention.toArray,entityOfWord.toArray, topicCount,entityForMentionCount,entityForWordCount)
    if(isTraning)
      documents+=newestDoc
    isRunning=false
  }

}


object DocumentInitializer{
  val RandomGenerator=new Random();
  def apply(tokenizer:TextTokenizer, searcher: DBCandidateSearcher, properties:Properties, isTraining:Boolean=false):DocumentInitializer={
    val topicNum=properties.getProperty("topicNum").toInt
    val maxSurfaceformLen=properties.getProperty("maxSurfaceformLen").toInt

    if(isTraining){
      val T=properties.getProperty("topicNum").toFloat
      val E=properties.getProperty("resourceNum").toFloat
      val K=properties.getProperty("surfaceNum").toFloat
      val V=properties.getProperty("tokenNum").toFloat

      val topicentityCount=GlobalCounter(T.toInt+1,E.toInt+1)
      val entitymentionCount=GlobalCounter(E.toInt+1,K.toInt+1)
      val entitywordCount=GlobalCounter(E.toInt+1,V.toInt+1)


      new DocumentInitializer(topicentityCount,entitymentionCount,entitywordCount,tokenizer,searcher,topicNum,maxSurfaceformLen,true)
     }else{
       new DocumentInitializer(null,null,null,tokenizer,searcher,topicNum,maxSurfaceformLen)
    }
  }

}