package org.dbpedia.spotlight.model

import org.dbpedia.spotlight.db.memory.{MemoryResourceStore, MemoryContextStore, MemoryCandidateMapStore}
import scala.collection.mutable
import org.dbpedia.spotlight.io.{EntityTopicModelDocumentsSource, WikiOccurrenceSource}
import scala.util.Random
import scala.collection.JavaConversions._
import com.esotericsoftware.kryo.Kryo
import java.io.{FileInputStream, FileOutputStream, File}
import scala.Array
import com.esotericsoftware.kryo.io.{Input, Output}
import org.dbpedia.extraction.util.Language
import org.dbpedia.spotlight.db.{WikipediaToDBpediaClosure, SpotlightModel}
import org.dbpedia.spotlight.db.tokenize.LanguageIndependentTokenizer
import org.dbpedia.spotlight.db.stem.SnowballStemmer
import java.util.Locale
import org.dbpedia.spotlight.log.SpotlightLog

/**
 * see "An entity-topic model for entity linking. X. Han and L. Sun" This implementation is very basic but at the same time efficient and fast.
 * Serialization through: SimpleEntityTopicModel.toFile(...), and Deserialization through: SimpleEntityTopicModel .fromFile(...)
 *
 */
class SimpleEntityTopicModel(val numTopics: Int, val numEntities: Int, val vocabularySize: Int, val numMentions: Int,
                             val alpha: Double, val beta: Double, val gamma: Double, val delta: Double, create:Boolean = false,
                             var candMap: MemoryCandidateMapStore = null, var contextStore:MemoryContextStore = null) {

  private val resStore:MemoryResourceStore = if(candMap != null) candMap.resourceStore.asInstanceOf[MemoryResourceStore] else null

  var entityTopicMatrix:Array[Array[Int]] = null
  var sparseWordEntityMatrix:Array[java.util.HashMap[Int,Int]] = null
  var sparseMentionEntityMatrix:Array[java.util.HashMap[Int,Int]] = null

  var topicCounts:Array[Int] = null
  var entityCounts:Array[Int] = null
  var assignmentCounts:Array[Int] = null

  if(create) {
    entityTopicMatrix = Array.ofDim[Int](numTopics, numEntities)
    sparseWordEntityMatrix = Array.fill(vocabularySize)(new java.util.HashMap[Int, Int]())
    topicCounts = new Array[Int](numTopics)
    entityCounts = new Array[Int](numEntities)
    assignmentCounts = new Array[Int](numEntities)
    sparseMentionEntityMatrix = Array.fill(numMentions)(new java.util.HashMap[Int, Int]())
  }

  private def getCountMap(elements: Array[Int]): mutable.HashMap[Int, Int] =
    elements.foldLeft(mutable.HashMap[Int, Int]())((acc, element) => {
      if (element >= 0)
        acc += (element -> (acc.getOrElse(element, 0) + 1))
      acc
    })
  

  private def updateCountMap(oldKey:Int,newKey:Int,map:mutable.Map[Int,Int]) {
    if(oldKey != newKey) {
      if(oldKey >= 0) {
        val oldValue = map.getOrElse(oldKey,1)
        if(oldValue == 1) map.remove(oldKey)
        else map += oldKey -> (oldValue-1)
      }
      if(newKey >= 0)
        map += newKey -> (map.getOrElse(newKey,0) + 1)
    }
  }

  def trainWithDocument(doc:EntityTopicDocument, firstTime:Boolean, iterations:Int = 1) {
    if(doc.mentions.length > 0) {
      val oldDoc = doc.clone()
      gibbsSampleDocument(doc, iterations = iterations, training = true, init = firstTime)

      //Perform synchronized update
      this.synchronized {
        (0 until oldDoc.mentions.length).foreach(idx => {
          val mention = doc.mentions(idx)
          val oldEntity = oldDoc.mentionEntities(idx)
          val oldTopic = oldDoc.entityTopics(idx)
          val newEntity = doc.mentionEntities(idx)
          val newTopic = doc.entityTopics(idx)

          //Update total topic counts
          if(firstTime || newTopic != oldTopic) {
            if(!firstTime && oldTopic >= 0)
              topicCounts(oldTopic) -= 1
            topicCounts(newTopic) += 1
          }

          if(firstTime || oldEntity != newEntity) {
            //update total entity counts
            if(!firstTime && oldEntity >= 0)
              entityCounts(oldEntity) -= 1
            entityCounts(newEntity) += 1

            //update entity-mention counts
            val mentionCounts = sparseMentionEntityMatrix(mention)
            updateCountMap({ if(firstTime) -1 else oldEntity },newEntity,mentionCounts)
          }

          //update entity-topic counts
          if(firstTime || oldEntity!= newEntity || oldTopic != newTopic) {
            if (!firstTime && oldTopic >= 0 && oldEntity >= 0)
              entityTopicMatrix(oldTopic)(oldEntity) -= 1
            entityTopicMatrix(newTopic)(newEntity) += 1
          }
        })

        (0 until doc.tokens.length).foreach(idx => {
          val token = doc.tokens(idx)
          val oldEntity = oldDoc.tokenEntities(idx)
          val newEntity = doc.tokenEntities(idx)

          if(firstTime || oldEntity != newEntity) {
            //update total assignment counts
            if(!firstTime && oldEntity >= 0)
              assignmentCounts(oldEntity) -= 1
            assignmentCounts(newEntity) += 1

            //update context counts
            val tokenCounts = sparseWordEntityMatrix(token)
            updateCountMap({ if(firstTime) -1 else oldEntity },newEntity,tokenCounts)
          }
        })
      }
    }
  }

  def gibbsSampleDocument(doc: EntityTopicDocument, iterations:Int = 300, training: Boolean = false, init: Boolean = false, returnStatistics:Boolean = false) = {
    val docTopicCounts = getCountMap(doc.entityTopics)
    val docEntityCounts = getCountMap(doc.mentionEntities)
    val docAssignmentCounts = getCountMap(doc.tokenEntities)

    val stats = if(returnStatistics) doc.mentionEntities.map(_ => mutable.Map[Int,Int]()) else null

    require(!training || iterations == 1, "At the moment training is only possible with iterations=1, because after one iteration information about old assignments is lost!")

    (0 until iterations).foreach(i => {

      { //Sample Topics & Entities
        (0 until doc.entityTopics.length).foreach(idx => {
          val oldEntity = doc.mentionEntities(idx)
          val oldTopic = doc.entityTopics(idx)
          val mention = doc.mentions(idx)

          //entity
          val newEntity = if (doc.isInstanceOf[EntityTopicTrainingDocument] && doc.asInstanceOf[EntityTopicTrainingDocument].entityFixed(idx))
            oldEntity
          else {
            val mentionCounts = sparseMentionEntityMatrix(mention)
            var cands:Iterable[Int] =
              if(candMap == null) mentionCounts.keySet()
              else candMap.candidates(mention)

            val candCounts:Map[Int,Int] = if(init && candMap != null) cands.zip(candMap.candidateCounts(mention).map(candMap.qc)).toMap else null

            if(cands == null || cands.isEmpty) {
              SpotlightLog.debug(getClass,s"There are no candidates for mention id $mention")
              cands = Iterable[Int]()
            }

            sampleFromProportionals(entity => {
              val localAdd = if (entity == oldEntity) -1 else 0
              val globalAdd = if (training) localAdd else 0
              val cte =
                if(oldTopic >= 0) entityTopicMatrix(oldTopic)(entity)
                else 0
              //if initial phase use counts from statistical model
              val (cem,ce) =
                if(init && candMap != null) (candCounts(entity) ,resStore.qc(resStore.supportForID(entity)))
                else (mentionCounts.getOrElse(entity,0)+globalAdd,entityCounts(entity)+globalAdd)

              val docAssCount = docAssignmentCounts.getOrElse(entity, 0)
              val docCount = docEntityCounts.getOrElse(entity,0) + localAdd

              (cte + globalAdd + beta) *
                (cem + gamma) / (ce + numMentions * gamma) *
                { if(docCount > 0 && docAssCount > 0 ) math.pow((docCount + 1) / docCount, docAssCount)
                else 1
                }
            }, cands)
          }

          doc.mentionEntities(idx) = newEntity

          //local update
          updateCountMap(oldEntity,newEntity,docEntityCounts)

          if(returnStatistics)
            stats(idx) += newEntity -> (stats(idx).getOrElse(newEntity,0) + 1)

          //topic
          val newTopic =
            sampleFromProportionals(topic => {
              val localAdd = if (topic == oldTopic) -1 else 0
              val globalAdd = if (training) localAdd else 0
              if (newEntity >= 0)
                (docTopicCounts.getOrElse(topic, 0) + localAdd + alpha) *
                  (entityTopicMatrix(topic)(newEntity) + {if(newEntity == oldEntity) globalAdd else 0} + beta) / (topicCounts(topic) + globalAdd + numTopics * beta)
              else
                (docTopicCounts.getOrElse(topic, 0) + localAdd + alpha) / (topicCounts(topic) + globalAdd + numTopics * beta)
            }, 0 until numTopics)

          doc.entityTopics(idx) = newTopic

          //local update
          if(i < iterations - 1)
            updateCountMap(oldTopic, newTopic, docTopicCounts)
        })
      }

      { //Sample new assignments
        val candidateEntities = docEntityCounts.keySet
        if (!candidateEntities.isEmpty)
          (0 until doc.tokenEntities.length).foreach(idx => {
            val oldEntity = doc.tokenEntities(idx)
            val token = doc.tokens(idx)
            val entityTokenCounts = sparseWordEntityMatrix(token)

            val newEntity =
              sampleFromProportionals(entity => {
                //if initial phase and context model is given, use counts from statistical model
                val add = if (training && entity == oldEntity) -1 else 0
                val (cew,ce) =
                  if(init && contextStore != null) {
                    val tokens = contextStore.tokens(entity)
                    if(tokens != null) {
                      val ct = (0 until tokens.length).
                        find(i => tokens(i) == token).map(i => contextStore.qc(contextStore.counts(entity)(i)))
                      (ct.getOrElse(0), contextStore.totalTokenCounts(entity))
                    } else (entityTokenCounts.getOrElse(entity, 0)+add, assignmentCounts(entity)+add)
                  } else  {
                    (entityTokenCounts.getOrElse(entity, 0)+add, assignmentCounts(entity)+add)
                  }

                docEntityCounts(entity) *
                  (cew + delta) / (ce + vocabularySize * delta)
              }, candidateEntities)

            doc.tokenEntities(idx) = newEntity

            //local update
            if(i < iterations - 1)
              updateCountMap(oldEntity,newEntity,docAssignmentCounts)
          })
      }
    })

    stats
  }

  private[model] def sampleFromProportionals(calcProportion: Int => Double, candidates: Traversable[Int]) = {
    var sum = 0.0
    val cands = candidates.map(cand => {
      var res = calcProportion(cand)
      require(res >= 0.0,s"Calculating negative proportion is impossible! Candidate: $cand")
      sum += res
      (cand, res)
    }).toList

    if(cands.isEmpty)
      Int.MinValue
    else if(cands.tail.isEmpty)
      cands.head._1
    else {
      var random = Random.nextDouble() * sum
      var selected = (Int.MinValue, 0.0)
      val it = cands.toIterator
      while (random >= 0.0 && it.hasNext) {
        selected = it.next()
        random -= selected._2
      }
      selected._1
    }
  }

}

object SimpleEntityTopicModel {
  private val kryo = new Kryo()

  kryo.register(classOf[Int])
  kryo.register(classOf[Double])
  kryo.register(classOf[Array[Int]])
  kryo.register(classOf[java.util.HashMap[Int, Int]])
  kryo.register(classOf[Array[Array[Int]]])
  kryo.register(classOf[Array[java.util.HashMap[Int, Int]]])

  def fromFile(file: File) = {
    SpotlightLog.info(getClass,s"Loading Entity-Topic-Model from ${file.getAbsolutePath}...")
    val start = System.currentTimeMillis()

    val in = new Input(new FileInputStream(file))
    val numTopics = kryo.readObject(in, classOf[Int])
    val numEntities = kryo.readObject(in, classOf[Int])
    val vocabularySize = kryo.readObject(in, classOf[Int])
    val numMentions = kryo.readObject(in, classOf[Int])

    val alpha = kryo.readObject(in, classOf[Double])
    val beta = kryo.readObject(in, classOf[Double])
    val gamma = kryo.readObject(in, classOf[Double])
    val delta = kryo.readObject(in, classOf[Double])

    val model = new SimpleEntityTopicModel(numTopics, numEntities, vocabularySize, numMentions, alpha, beta, gamma, delta)
    model.entityTopicMatrix = kryo.readObject(in, classOf[Array[Array[Int]]])
    model.sparseWordEntityMatrix = kryo.readObject(in, classOf[Array[java.util.HashMap[Int, Int]]])
    model.sparseMentionEntityMatrix= kryo.readObject(in, classOf[Array[java.util.HashMap[Int, Int]]])

    model.topicCounts = kryo.readObject(in, classOf[Array[Int]])
    model.entityCounts = kryo.readObject(in, classOf[Array[Int]])
    model.assignmentCounts = kryo.readObject(in, classOf[Array[Int]])

    in.close()
    SpotlightLog.info(getClass,s"loaded in ${System.currentTimeMillis()-start}ms!")
    model
  }

  def toFile(file: File, model: SimpleEntityTopicModel) {
    val out = new Output(new FileOutputStream(file))
    kryo.writeObject(out, model.numTopics)
    kryo.writeObject(out, model.numEntities)
    kryo.writeObject(out, model.vocabularySize)
    kryo.writeObject(out, model.numMentions)

    kryo.writeObject(out, model.alpha)
    kryo.writeObject(out, model.beta)
    kryo.writeObject(out, model.gamma)
    kryo.writeObject(out, model.delta)

    kryo.writeObject(out, model.entityTopicMatrix)
    kryo.writeObject(out, model.sparseWordEntityMatrix)
    kryo.writeObject(out, model.sparseMentionEntityMatrix)

    kryo.writeObject(out, model.topicCounts)
    kryo.writeObject(out, model.entityCounts)
    kryo.writeObject(out, model.assignmentCounts)
    out.close()
  }
}
