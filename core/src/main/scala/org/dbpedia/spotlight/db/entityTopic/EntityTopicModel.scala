package org.dbpedia.spotlight.db.entitytopic

import java.io.{FileInputStream, File}
import java.util.{Properties}
import org.dbpedia.spotlight.db.{DBCandidateSearcher, SpotlightModel}
import org.dbpedia.spotlight.db.model.TextTokenizer
import org.dbpedia.spotlight.model._
import scala.collection.JavaConverters._
import opennlp.tools.util.Span
import scala.collection.mutable.ListBuffer
import org.dbpedia.spotlight.disambiguate.ParagraphDisambiguator
import org.dbpedia.spotlight.db.memory.MemoryCandidateMapStore

class EntityTopicModel(val tokenizer:TextTokenizer,
                       val searcher: DBCandidateSearcher,
                       val properties: Properties) extends ParagraphDisambiguator{

  val docInitializer=DocumentInitializer(tokenizer,searcher,properties)

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
    val map=Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]]()
    (doc.mentions,doc.entityOfMention).zipped.foreach((mention,entity)=>map+(mention->searcher.resStore.getResource(entity)))
    map
  }

  def inference(paragraph: Paragraph):Document={
    val surfaceOccrs=paragraph.getOccurrences().asScala
    val mentions=new ListBuffer[SurfaceFormOccurrence]()
    val resources=new ListBuffer[DBpediaResource]()

    surfaceOccrs.foreach((sfOccr:SurfaceFormOccurrence)=>{
      val cands=searcher.getCandidates(sfOccr.surfaceForm)
      if(cands.size>0){
        val resId=Document.multinomialSample(cands.map((cand:Candidate)=>cand.support.asInstanceOf[Float]).toArray, cands.map((cand:Candidate)=>cand.resource.id).toArray)
        mentions+=sfOccr
        resources+=searcher.resStore.getResource(resId)
        //spans+=new Span(sfOccr.textOffset,sfOccr.textOffset+sfOccr.surfaceForm.name.length)
      }
    })

    val doc=docInitializer.initDocument(paragraph.text,resources.toArray,mentions.toArray)
    doc.updateAssignment(false)
    doc
  }
}


object EntityTopicModel{

  def fromFolder(modelFolder: File, topicNum:Int=100): EntityTopicModel = {
    val properties = new Properties()
    properties.load(new FileInputStream(new File(modelFolder, "model.properties")))

    val stopwords = SpotlightModel.loadStopwords(modelFolder)
    val c = properties.getProperty("opennlp_parallel", Runtime.getRuntime.availableProcessors().toString).toInt
    val cores = (1 to c)
    val (tokenTypeStore, sfStore, resStore, candMapStore, _) = SpotlightModel.storesFromFolder(modelFolder)
    val tokenizer: TextTokenizer= SpotlightModel.createTokenizer(modelFolder,tokenTypeStore,properties,stopwords,cores)
    val searcher:DBCandidateSearcher = new DBCandidateSearcher(resStore, sfStore, candMapStore)
    //val spotter=SpotlightModel.createSpotter(modelFolder,sfStore,stopwords,cores)

    val topicentity=GlobalCounter.readFromFile(modelFolder+"/entitytopic/topicentity_count")
    val entitymention=GlobalCounter.readFromFile(modelFolder+"/entitytopic/entitymention_count")
    val entityword=GlobalCounter.readFromFile(modelFolder+"/entitytopic/entityword_count")

    Document.init(topicentity,entitymention,entityword,candMapStore.asInstanceOf[MemoryCandidateMapStore],properties)
    new EntityTopicModel(tokenizer, searcher, properties)
  }

}