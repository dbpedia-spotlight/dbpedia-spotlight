package org.dbpedia.spotlight.db.entitytopic

import java.io.{FileInputStream, File}
import java.util.{Properties}
import org.dbpedia.spotlight.db.{DBCandidateSearcher, SpotlightModel}
import org.dbpedia.spotlight.model._
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import org.dbpedia.spotlight.disambiguate.ParagraphDisambiguator
import org.dbpedia.spotlight.db.memory.MemoryCandidateMapStore
import org.dbpedia.spotlight.model.Factory.DBpediaResourceOccurrence


class EntityTopicDisambiguator(val searcher: DBCandidateSearcher,
                               val properties: Properties,
                               val gibbsSteps: Int
                              ) extends ParagraphDisambiguator{

  val docInitializer=DocumentInitializer(properties)

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
    var map=Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]]()
    inference(paragraph).foreach{case (sf,l)=>{
      map+=sf->l.sortBy(_.similarityScore).reverse.take(k)
    }}
    map
  }

  def inference(paragraph: Paragraph):Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]]={
    var map=Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]]()
    paragraph.occurrences.foreach(sfOccr=>{
      map+=(sfOccr->List[DBpediaResourceOccurrence]())
    })

    val surfaceOccrs=paragraph.getOccurrences().asScala
    val resOccrs=new ListBuffer[DBpediaResourceOccurrence]()
    //extract DbpediaResourceOccurrences
    surfaceOccrs.foreach((sfOccr:SurfaceFormOccurrence)=>{
      val cands=searcher.getCandidates(sfOccr.surfaceForm)
      if(cands.size>0){
        //random sample a resource to a mention
        val resId=Document.multinomialSample(cands.map((cand:Candidate)=>cand.support.asInstanceOf[Float]).toArray, cands.map((cand:Candidate)=>cand.resource.id).toArray)
        resOccrs+=DBpediaResourceOccurrence.from(sfOccr, searcher.resStore.getResource(resId), 0.0)
      }
    })

    if(resOccrs.size>0){
      val doc=docInitializer.initDocument(paragraph.text,resOccrs.toArray)
      if(doc!=null){
        (0 until gibbsSteps).foreach(_=>{
          doc.updateAssignment(false)
          (doc.mentions, doc.entityOfMention).zipped.foreach((mention,entity)=>{
            val newResOccr=Factory.DBpediaResourceOccurrence.from(mention,searcher.resStore.getResource(entity),1.0)
            val resOccrList=map.get(mention).get
            val oldResOccr=resOccrList.find(resOccr=>newResOccr==resOccr)
            if (oldResOccr==None)
              map.updated(mention,newResOccr::resOccrList)
            else
              //similarity score is set as the # of times this resource is assigned to the mention over all gibbsSteps
              oldResOccr.get.setSimilarityScore(oldResOccr.get.similarityScore+1)
        })}
        )
      }
    }
    map
  }
}


object EntityTopicDisambiguator{

  def fromFolder(spotlightFolder: File,  gibbsSteps:Int=30): EntityTopicDisambiguator = {
    val properties = new Properties()
    properties.load(new FileInputStream(new File(spotlightFolder, "model.properties")))


    val (_, sfStore, resStore, candMapStore, _) = SpotlightModel.storesFromFolder(spotlightFolder)
    val searcher:DBCandidateSearcher = new DBCandidateSearcher(resStore, sfStore, candMapStore)
    val entitytopicFolder=spotlightFolder+"/entitytopic"
    val topicentity=GlobalCounter.readFromFile(entitytopicFolder+"/topicentity_sum")
    val entitymention=GlobalCounter.readFromFile(entitytopicFolder+"/entitymention_sum")
    val entityword=GlobalCounter.readFromFile(entitytopicFolder+"/entityword_sum")

    Document.init(entitymention,entityword,topicentity,candMapStore.asInstanceOf[MemoryCandidateMapStore],properties)
    new EntityTopicDisambiguator(searcher, properties, gibbsSteps)
  }

}
