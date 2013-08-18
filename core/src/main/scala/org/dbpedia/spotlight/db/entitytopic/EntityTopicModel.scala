package org.dbpedia.spotlight.db.entitytopic

import java.io.{FileInputStream, File}
import java.util.{Properties}
import org.dbpedia.spotlight.db.{DBCandidateSearcher, SpotlightModel}
import org.dbpedia.spotlight.db.model.TextTokenizer
import org.dbpedia.spotlight.model._
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import org.dbpedia.spotlight.disambiguate.ParagraphDisambiguator
import org.dbpedia.spotlight.db.memory.MemoryCandidateMapStore
import org.dbpedia.spotlight.model.Factory.DBpediaResourceOccurrence

class EntityTopicModel(val tokenizer:TextTokenizer,
                       val searcher: DBCandidateSearcher,
                       val properties: Properties,
                       val gibbsSteps: Int) extends ParagraphDisambiguator{

  val docInitializer=DocumentInitializer(tokenizer,searcher,properties)

  //val gibbsSteps=properties.getProperty("gibbsSteps").toInt

  def name="Entity Topic Model"

  def disambiguate(paragraph:Paragraph):  List[DBpediaResourceOccurrence]={
    bestK(paragraph, 1)
      .filter(kv =>
      kv._2.nonEmpty)
      .map( kv =>
      kv._2.head)
      .toList
      .sortBy(_.textOffset)
  }

  def bestK(paragraph: Paragraph, k: Int): Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]]={
    if(paragraph.getOccurrences().size()>0){
      val doc=inference(paragraph)
      mapResult(doc)
    }else{
      null
    }
  }

  def mapResult(doc:Document):Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]]={
    var map=Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]]()
    if(doc!=null)
      (doc.mentions,doc.entityOfMention).zipped.foreach((mention,entity)=>map+=(mention->List(Factory.DBpediaResourceOccurrence.from(mention,searcher.resStore.getResource(entity),0.0))))
    map
  }

  def inference(paragraph: Paragraph):Document={
    val surfaceOccrs=paragraph.getOccurrences().asScala
    val resOccrs=new ListBuffer[DBpediaResourceOccurrence]()
    surfaceOccrs.foreach((sfOccr:SurfaceFormOccurrence)=>{
      val cands=searcher.getCandidates(sfOccr.surfaceForm)
      if(cands.size>0){
        val resId=Document.multinomialSample(cands.map((cand:Candidate)=>cand.support.asInstanceOf[Float]).toArray, cands.map((cand:Candidate)=>cand.resource.id).toArray)
        resOccrs+=DBpediaResourceOccurrence.from(sfOccr, searcher.resStore.getResource(resId), 0.0)
      }
    })

    if(resOccrs.size>0){
      val doc=docInitializer.initDocument(paragraph.text,resOccrs.toArray)
      (0 until gibbsSteps).foreach(_=>
        doc.updateAssignment(false)
      )
      doc
    }else{
      null
    }

  }
}


object EntityTopicModel{

  def fromFolder(spotlightFolder: File, entitytopicFolder:String, gibbsSteps:Int=30): EntityTopicModel = {
    val properties = new Properties()
    properties.load(new FileInputStream(new File(spotlightFolder, "model.properties")))

    val stopwords = SpotlightModel.loadStopwords(spotlightFolder)
    val c = properties.getProperty("opennlp_parallel", Runtime.getRuntime.availableProcessors().toString).toInt
    val cores = (1 to c)
    val (tokenTypeStore, sfStore, resStore, candMapStore, _) = SpotlightModel.storesFromFolder(spotlightFolder)
    val tokenizer: TextTokenizer= SpotlightModel.createTokenizer(spotlightFolder,tokenTypeStore,properties,stopwords,cores)
    val searcher:DBCandidateSearcher = new DBCandidateSearcher(resStore, sfStore, candMapStore)

    val topicentity=GlobalCounter.readAvgFromFile(entitytopicFolder+"/topicentity_sum")
    val entitymention=GlobalCounter.readAvgFromFile(entitytopicFolder+"/entitymention_sum")
    val entityword=GlobalCounter.readAvgFromFile(entitytopicFolder+"/entityword_sum")

    Document.init(topicentity,entitymention,entityword,candMapStore.asInstanceOf[MemoryCandidateMapStore],properties)
    new EntityTopicModel(tokenizer, searcher, properties,gibbsSteps)
  }

}