package org.dbpedia.spotlight.train

import java.io.{File}
import org.dbpedia.spotlight.db.memory.{MemorySurfaceFormStore, MemoryResourceStore}
import org.dbpedia.spotlight.db.{DBCandidateSearcher, DBTwoStepDisambiguator, SpotlightModel}
import cc.factorie.variable._
import org.dbpedia.spotlight.model._
import org.dbpedia.spotlight.io.AllOccurrenceSource
import org.dbpedia.extraction.util.Language
import scala.collection.JavaConversions._
import scala.util.Random
import scala.collection.mutable.ArrayBuffer
import cc.factorie.directed._
import org.dbpedia.spotlight.model.SpotterConfiguration.SpotterPolicy
import org.dbpedia.spotlight.model.SpotlightConfiguration.DisambiguationPolicy
import cc.factorie.model.Factor
import scala.Some
import scala.Some
import scala.Some
import cc.factorie.la.SparseIndexedTensor1

/**
 * Created by dirkw on 3/5/14.
 */
//TODO: incomplete... try summingbird for now
object TrainEntityTopicModelWithFactory {

  def main(args: Array[String]) {
    //testEntityTopicModel

    val wikiDump = new File(args(0))
    val modelFolder = new File(args(1))

    val model = SpotlightModel.fromFolder(modelFolder)

    val spotter = model.spotters(SpotterPolicy.Default)
    val searcher = model.disambiguators(DisambiguationPolicy.Default).disambiguator.asInstanceOf[DBTwoStepDisambiguator].candidateSearcher

    val entityDomain = new CategoricalDomain[String]()
    searcher.resStore.asInstanceOf[MemoryResourceStore].idFromURI
      .withFilter(e => e._1 != null && searcher.resStore.getResource(e._2).support > 100)
      .foreach(entity => entityDomain.index(entity._1))

    val mentionDomain = new CategoricalDomain[String]()
    searcher.sfStore.asInstanceOf[MemorySurfaceFormStore].idForString
      .withFilter(e => e._1 != null && searcher.getCandidates(searcher.sfStore.getSurfaceForm(e._1)).exists(c => c.resource.support > 100))
      .foreach(sf => mentionDomain.index(sf._1))

    val entityTopicModel = new EntityTopicModel(300, entityDomain, mentionDomain, 100000)

    var currentContext: Text = new Text("")
    var currentAnnotations = List[DBpediaResourceOccurrence]()

    var docs = List[entityTopicModel.Document]()

    def isResourceAllowed(uri: String) = entityDomain.indexOnly(uri) >= 0
    def isSurfaceFormAllowed(sf: String) = mentionDomain.indexOnly(sf) >= 0

    AllOccurrenceSource.fromXMLDumpFile(wikiDump, Language.English).take(100).foreach(resOcc => {
      if (currentContext == resOcc.context) {
        currentAnnotations ::= resOcc
      }
      else {
        if (currentContext.text != "") {
          val tokens = model.tokenizer.tokenize(currentContext)
          currentContext.setFeature(new Feature("tokens", tokens))

          val spots = spotter.extract(currentContext)
          val entityToMentions =
            currentAnnotations.
              withFilter(occ => isResourceAllowed(occ.resource.uri) && isSurfaceFormAllowed(occ.surfaceForm.name)).
              map(occ => (occ.resource.uri, occ.surfaceForm.name)) ++
              spots.withFilter(spot => !currentAnnotations.exists(_.textOffset == spot.textOffset) && isSurfaceFormAllowed(spot.surfaceForm.name)).flatMap(spot => {
                //sample candidate from its support
                val candidates = searcher.getCandidates(spot.surfaceForm).filter(c => isResourceAllowed(c.resource.uri))
                val total = candidates.map(_.support).sum
                if (total > 0) {
                  var rand = Random.nextInt(total)
                  Some((candidates.dropWhile(cand => {
                    rand -= cand.support; rand < 0
                  }).head.resource.uri, spot.surfaceForm.name))
                } else None
              })

          docs ::= entityTopicModel.addVariablesToModel(tokens.filterNot(_.tokenType == TokenType.STOPWORD).map(_.token), entityToMentions, currentAnnotations.head.id)
        }

        currentContext = resOcc.context
        currentAnnotations = List(resOcc)
      }
    })

    val collapse = new ArrayBuffer[Var]
    collapse ++= docs.map(_.theta)
    collapse += entityTopicModel.phis
    collapse += entityTopicModel.psis
    collapse += entityTopicModel.zetas

    implicit val random = new Random()

    val sampler = new CollapsedGibbsSampler(collapse, entityTopicModel.model)
    sampler.handlers += EntityCollapsedGibbsSamplerHandler
    sampler.handlers += AssignmentsCollapsedGibbsSamplerHandler

    for (i <- 1 to 20) {
      var ctr = 0
      for (doc <- docs) {
        sampler.process(doc.topics)
        sampler.process(doc.entities)
        sampler.process(doc.assignments)
        ctr += 1
        if (ctr % 1000 == 0)
          println(ctr + " documents processed in iteration " + i)
      }
    }
  }


  def testEntityTopicModel {
    val entityDomain1 = new CategoricalDomain[String]()
    entityDomain1.index("a")
    val mentionDomain1 = new CategoricalDomain[String]()
    mentionDomain1.index("a")
    val entityTopicModel1 = new EntityTopicModel(300, entityDomain1, mentionDomain1, 10)

    entityTopicModel1.addVariablesToModel(List("a"), List(("a", "a")))
  }
}

object EntityCollapsedGibbsSamplerHandler extends CollapsedGibbsSamplerHandler {
  def sampler(v: Iterable[Var], factors: Iterable[Factor], sampler: CollapsedGibbsSampler)(implicit random: scala.util.Random): CollapsedGibbsSamplerClosure = {
    if (v.size != 1 || factors.size != 3) return null
    val parentFactor = factors.collectFirst({
      case f: PlatedDiscreteMixture.Factor => f
    })
    val childMixtureFactor = factors.collectFirst({
      case f: PlatedCategoricalMixture.Factor => f
    })
    val childPlatedDiscreteFactor = factors.collectFirst({
      case f: PlatedDiscreteFromInducedDistribution.Factor => f
    })
    if (parentFactor == None || childMixtureFactor == None || childPlatedDiscreteFactor == None) return null
    assert(parentFactor.get._1 == childMixtureFactor.get._3 && parentFactor.get._1 == childPlatedDiscreteFactor.get._2)
    new Closure(sampler, parentFactor.get, childMixtureFactor.get, childPlatedDiscreteFactor.get)
  }

  class Closure(val sampler: CollapsedGibbsSampler,
                val parentDiscreteFactor: PlatedDiscreteMixture.Factor,
                val childMixtureFactor: PlatedCategoricalMixture.Factor,
                val childPlatedDiscreteFactor: PlatedDiscreteFromInducedDistribution.Factor)(implicit random: scala.util.Random) extends CollapsedGibbsSamplerClosure {
    def sample(implicit d: DiffList = null): Unit = {
      val gates = childMixtureFactor._3.asInstanceOf[DiscreteSeqVariable]
      val domainSize = gates.domain.elementDomain.size // domain.size
      val topics = parentDiscreteFactor._3.asInstanceOf[DiscreteSeqVariable]
      val assignmentCounts = childPlatedDiscreteFactor._1.asInstanceOf[DiscreteSeqVariable].intValues.groupBy(i => i).mapValues(_.length)

      val entityFromTopic = parentDiscreteFactor._2.asInstanceOf[Mixture[ProportionsVariable]]
      val parentCollapsed = sampler.isCollapsed(entityFromTopic)
      val mentionFromEntity = childMixtureFactor._2.asInstanceOf[Mixture[ProportionsVariable]]
      val childCollapsed = sampler.isCollapsed(mentionFromEntity)

      for (index <- 0 until gates.length) {
        val mentionIdx = childMixtureFactor._1(index).intValue

        val topic = topics(index).intValue
        // Remove sufficient statistics from collapsed dependencies
        var entityIdx: Int = gates(index).intValue
        if (parentCollapsed) entityFromTopic(topic).incrementMasses(entityIdx, -1.0)
        if (childCollapsed) mentionFromEntity(entityIdx).incrementMasses(mentionIdx, -1.0)
        // Calculate distribution of new value
        val proportions = new SparseIndexedTensor1(domainSize)
        (0 until domainSize).foreach(possibleEntity => {
          val props = mentionFromEntity(possibleEntity).value.asInstanceOf[Proportions1]
          // only allow entities with this mention
          if (props.masses.activeDomain1.contains(mentionIdx)) {
            val count = (0 until gates.length).count(j => j != index && gates(j).intValue == possibleEntity)
            proportions +=(possibleEntity, entityFromTopic(topic).value(possibleEntity) * props(mentionIdx) * (0 until assignmentCounts.getOrElse(possibleEntity, 0)).foldLeft(1.0)((acc, _) => acc * (count + 1.0) / count))
          }
        })

        // Sample
        // sum can be zero for a new word in the domain and a non-collapsed growable Proportions has not yet placed non-zero mass there
        entityIdx = proportions.sampleIndex
        // Put back sufficient statistics of collapsed dependencies
        if (parentCollapsed) entityFromTopic(topic).incrementMasses(entityIdx, 1.0)
        if (childCollapsed) mentionFromEntity(entityIdx).incrementMasses(mentionIdx, 1.0)
      }
    }
  }

}


object AssignmentsCollapsedGibbsSamplerHandler extends CollapsedGibbsSamplerHandler {
  def sampler(v: Iterable[Var], factors: Iterable[Factor], sampler: CollapsedGibbsSampler)(implicit random: scala.util.Random): CollapsedGibbsSamplerClosure = {
    if (v.size != 1 || factors.size != 3) return null
    val childMixtureFactor = factors.collectFirst({
      case f: PlatedCategoricalMixture.Factor => f
    })
    val parentPlatedDiscreteFactor = factors.collectFirst({
      case f: PlatedDiscreteFromInducedDistribution.Factor => f
    })
    if (parentPlatedDiscreteFactor == None || childMixtureFactor == None) return null
    assert(parentPlatedDiscreteFactor.get._1 == childMixtureFactor.get._3)
    new Closure(sampler, parentPlatedDiscreteFactor.get, childMixtureFactor.get)
  }

  class Closure(val sampler: CollapsedGibbsSampler,
                val parentDiscreteFactor: PlatedDiscreteFromInducedDistribution.Factor,
                val childMixtureFactor: PlatedCategoricalMixture.Factor)(implicit random: scala.util.Random) extends CollapsedGibbsSamplerClosure {
    def sample(implicit d: DiffList = null): Unit = {
      val gates = childMixtureFactor._3.asInstanceOf[DiscreteSeqVariable]
      val domainSize = gates.domain.elementDomain.size // domain.size
      val discreteParentCounts = parentDiscreteFactor._2.asInstanceOf[DiscreteSeqVariable].intValues.groupBy(i => i).mapValues(_.length)

      val childMixture = childMixtureFactor._2.asInstanceOf[Mixture[ProportionsVariable]]
      val childCollapsed = sampler.isCollapsed(childMixture)

      for (index <- 0 until gates.length) {
        val outcomeIntValue = childMixtureFactor._1(index).intValue
        // Remove sufficient statistics from collapsed dependencies
        var z: Int = gates(index).intValue
        if (childCollapsed) childMixture(z).incrementMasses(outcomeIntValue, -1.0)
        // Calculate distribution of new value
        val proportions = new SparseIndexedTensor1(domainSize)
        discreteParentCounts.keySet.foreach(i => {
          proportions +=(i, discreteParentCounts(i) * childMixture(i).value(outcomeIntValue))
        })
        // Sample
        // sum can be zero for a new word in the domain and a non-collapsed growable Proportions has not yet placed non-zero mass there
        z = proportions.sampleIndex
        gates.set(index, z)(null)
        // Put back sufficient statistics of collapsed dependencies
        if (childCollapsed) childMixture(z).incrementMasses(outcomeIntValue, 1.0)
      }
    }
  }

}
