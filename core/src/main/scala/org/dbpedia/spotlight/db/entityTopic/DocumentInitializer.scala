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

  val RandomGenerator=new Random();
  val documents:ListBuffer[Document]=new ListBuffer[Document]()
  var isRunning:Boolean=false

  def incCount(map:HashMap[Int,Int],key:Int){
    map.get(key) match{
      case a: Int=>map.put(key,a+1)
      case _ =>map.put(key,1)
    }
  }

  def restrictedSpot(text:Text, surfaces:Array[SurfaceForm], spans:Array[Span]):Array[SurfaceFormOccurrence]={
    val docStr=text.text
    val sfMap=new collection.mutable.HashMap[String, Int]()
    (surfaces, spans).zipped.foreach((sf,span)=>{
      if(sf.name!=docStr.substring(span.getStart,span.getEnd))
        System.out.println(sf.name, docStr.substring(span.getStart,span.getEnd))
      else sfMap.put(sf.name, sf.id)
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
            case Some(id)=>mentions+=new SurfaceFormOccurrence(new SurfaceForm(gram,id,0,0),null,token.offset)
            case None=>{}
          }
        }
      }else{
        var k=0
        val endIndex=startIndex+MaxSurfaceformLength
        for(k<-startIndex until endIndex){
          val gram=docStr.substring(token.offset, tokens(k+1).offset)
          sfMap.get(gram) match{
            case Some(id)=>mentions+=new SurfaceFormOccurrence(new SurfaceForm(gram,id,0,0),null,token.offset)
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

  var retDoc:Document=null
  var text:Text=null
  var resources: Array[DBpediaResource]=null
  var surfaces:Array[SurfaceForm]=null
  var spans:Array[Span]=null
  def initDocument(text:Text, resources: Array[DBpediaResource], surfaces:Array[SurfaceForm], spans:Array[Span]):Document={
    set(text, resources, surfaces, spans)
    run()
    retDoc
  }

  def set(t:Text, r: Array[DBpediaResource], s:Array[SurfaceForm], sp:Array[Span]){
    text=t
    resources=r
    surfaces=s
    spans=sp
    isRunning=true
  }

  def run(){
    val mentions:ListBuffer[Int]=new ListBuffer[Int]()
    val words:ListBuffer[Int]=new ListBuffer[Int]()
    val entityOfMention:ListBuffer[Int]=new ListBuffer[Int]()
    val topicOfMention: ListBuffer[Int]=new ListBuffer[Int]()
    val entityOfWord:ListBuffer[Int]=new ListBuffer[Int]()

    val topicCount:HashMap[Int,Int]=new HashMap[Int,Int]()
    val entityForMentionCount:HashMap[Int,Int]=new HashMap[Int,Int]()
    val entityForWordCount:HashMap[Int,Int]=new HashMap[Int,Int]()

    tokenizer.tokenizeMaybe(text)
    val tokens:List[Token]=text.featureValue[List[Token]]("tokens").get
    val surfaceOccr:Array[SurfaceFormOccurrence]=restrictedSpot(text, surfaces, spans)

    //spotterDebug(text, surfaceOccr, spans)
    /*tokens and mentions within two succinct link anchors are processed by associating a token
     *with the nearest anchor's entity, and sampling an entity for a mention based on its entity distribution
     */
    var i=0
    var j=0
    var prevRes=resources(0)
    var prevSpan=new Span(0,0)
    (resources,surfaces,spans).zipped.foreach((res:DBpediaResource,surface:SurfaceForm,span:Span)=>{
      //for tokens
      while(i<tokens.length && tokens(i).offset<span.getStart()){
        words+=tokens(i).tokenType.id
        if(span.getStart-tokens(i).offset>tokens(i).offset-prevSpan.getStart)
          entityOfWord+=prevRes.id
        else entityOfWord+=res.id

        incCount(entityForWordCount,entityOfWord.last)
        entitywordCount.incCount(entityOfWord.last, words.last)

        prevRes=res
        prevSpan=span
        i+=1
      }

      //tokens of the link anchor are assigned with the link's target entity
      while(i<tokens.length && tokens(i).offset<span.getEnd()){
        words+=tokens(i).tokenType.id
        entityOfWord+=res.id
        incCount(entityForWordCount,res.id)
        entitywordCount.incCount(entityOfWord.last, words.last)
        i+=1
      }

      //for mentions
      while(j<surfaceOccr.length &&  surfaceOccr(j).textOffset<span.getStart()){
        val cands=searcher.getCandidates(surfaceOccr(j).surfaceForm)
        if(cands.size>0){
          mentions+=surfaceOccr(j).surfaceForm.id
          topicOfMention+=RandomGenerator.nextInt(topicNum)
          entityOfMention+=Document.multinomialSample(cands.map((cand:Candidate)=>cand.support.asInstanceOf[Float]).toArray, cands.map((cand:Candidate)=>cand.resource.id).toArray)

          incCount(topicCount,topicOfMention.last)
          incCount(entityForMentionCount,entityOfMention.last)
          topicentityCount.incCount(topicOfMention.last, entityOfMention.last)
          entitymentionCount.incCount(entityOfMention.last, mentions.last)
          }
        j+=1
      }

      //mention of the link anchor are assigned with the link's target entity
      if(searcher.getCandidates(surface).size>0){
        mentions+=surface.id
        entityOfMention+=res.id
        topicOfMention+=RandomGenerator.nextInt(topicNum)

        incCount(topicCount,topicOfMention.last)
        incCount(entityForMentionCount,entityOfMention.last)
        topicentityCount.incCount(topicOfMention.last, entityOfMention.last)
        entitymentionCount.incCount(entityOfMention.last, mentions.last)
      }
    })

    //for the tokens after the last link anchor
    while(i<tokens.length){
      words+=tokens(i).tokenType.id
      entityOfWord+=resources.last.id

      incCount(entityForWordCount,resources.last.id)
      entitywordCount.incCount(entityOfWord.last, words.last)
      i+=1
    }

    //for the mentions after the last link anchor
    while(j<surfaceOccr.length){
      val cands=searcher.getCandidates(surfaceOccr(j).surfaceForm)
      if(cands.size>0){
        mentions+=surfaceOccr(j).surfaceForm.id
        topicOfMention+=RandomGenerator.nextInt(topicNum)

        entityOfMention+=Document.multinomialSample(cands.map((cand:Candidate)=>cand.support.asInstanceOf[Float]).toArray, cands.map((cand:Candidate)=>cand.resource.id).toArray)
        incCount(topicCount,topicOfMention.last)
        incCount(entityForMentionCount,entityOfMention.last)

        topicentityCount.incCount(topicOfMention.last, entityOfMention.last)
        entitymentionCount.incCount(entityOfMention.last, mentions.last)
      }
      j+=1
    }

    retDoc=new Document(mentions.toArray,words.toArray,entityOfMention.toArray,topicOfMention.toArray,entityOfWord.toArray, topicCount,entityForMentionCount,entityForWordCount)
    documents+=retDoc
    isRunning=false
  }

}


object DocumentInitializer{
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