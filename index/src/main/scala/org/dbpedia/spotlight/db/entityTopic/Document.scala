package org.dbpedia.spotlight.db.entityTopic

import scala.collection.mutable.ListBuffer
import scala.util.Random
import org.dbpedia.spotlight.spot.Spotter
import org.dbpedia.spotlight.db.{DBCandidateSearcher}
import scala.collection.mutable.HashMap
import org.dbpedia.spotlight.db.model.TextTokenizer
import org.dbpedia.spotlight.model._
import opennlp.tools.util.Span
import org.dbpedia.spotlight.db.entityTopic.Document





object DocumentObj {
  val RandomGenerator=new Random();

  var spotter:Spotter=null
  var tokenizer:TextTokenizer=null
  var searcher: DBCandidateSearcher = null
  var topicNum:Int=0

  def init(model:EntityTopicModel, topicN:Int){
    spotter=model.spotter
    tokenizer=model.tokenizer
    searcher=model.searcher
    topicNum=topicN
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

  def incCount(map:HashMap[Int,Int],key:Int){
    map.get(key) match{
      case a:Option[Int]=>map.put(key,a.getOrElse(0)+1)
    }
  }

  def initDocument(text:Text, resources: Array[DBpediaResource], surfaces:Array[SurfaceForm], spans:Array[Span]):Document={
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
    val surfaceOccr:java.util.List[SurfaceFormOccurrence]=spotter.extract(text)



    /*tokens and mentions within two succinct link anchors are processed by assigning a token
     *with nearest anchor's entity, and sampling an entity for a mention based on its entity distribution
     */
    var i=0
    var j=0
    var prevRes=resources(0)
    var prevSpan=new Span(0,0)
    (resources,surfaces,spans).zipped.foreach((res:DBpediaResource,surface:SurfaceForm,span:Span)=>{
      //for tokens
      while(tokens(i).offset<span.getStart()){
        words+=tokens(i).tokenType.id
        if(span.getStart-tokens(i).offset>tokens(i).offset-prevSpan.getStart)
          entityOfWord+=prevRes.id
        else entityOfWord+=res.id

        incCount(entityForWordCount,entityOfWord.last)

        prevRes=res
        prevSpan=span
        i+=1
      }

      //tokens of the link anchor are assigned with the link's target entity
      while(tokens(i).offset<span.getEnd()){
        words+=tokens(i).tokenType.id
        entityOfWord+=res.id
        incCount(entityForWordCount,res.id)
        i+=1
      }


      //for mentions
      while(j<surfaceOccr.size &&  surfaceOccr.get(j).textOffset<span.getStart()){
        mentions+=surfaceOccr.get(j).surfaceForm.id
        topicOfMention+=RandomGenerator.nextInt(topicNum)
        incCount(topicCount,topicOfMention.last)

        val cands=searcher.getCandidates(surfaceOccr.get(j).surfaceForm)
        entityOfMention+=multinomialSample(cands.map((cand:Candidate)=>cand.support.asInstanceOf[Float]).toArray, cands.map((cand:Candidate)=>cand.resource.id).toArray)
        incCount(entityForMentionCount,entityOfMention.last)
        j+=1
      }

      //mention of the link anchor are assigned with the link's target entity
      mentions+=surface.id
      incCount(topicCount,topicOfMention.last)
      entityOfMention+=res.id
      incCount(entityForMentionCount,entityOfMention.last)
    })

    //for the tokens after the last link anchor
    while(i<tokens.size){
      words+=tokens(i).tokenType.id
      entityOfWord+=resources.last.id
      incCount(entityForWordCount,resources.last.id)
      i+=1
    }

    //for the mentions after the last link anchor
    while(j<surfaceOccr.size()){
      mentions+=surfaceOccr.get(i).surfaceForm.id
      topicOfMention+=RandomGenerator.nextInt(topicNum)
      incCount(topicCount,topicOfMention.last)

      val cands=searcher.getCandidates(surfaceOccr.get(i).surfaceForm)
      entityOfMention+=multinomialSample(cands.map((cand:Candidate)=>cand.support.asInstanceOf[Float]).toArray, cands.map((cand:Candidate)=>cand.surfaceForm.id).toArray)
      incCount(entityForMentionCount,entityOfMention.last)
      j+=1
    }

    new Document(mentions.toArray,words.toArray,entityOfMention.toArray,topicOfMention.toArray,entityOfWord.toArray, topicCount,entityForMentionCount,entityForWordCount)
  }

}
