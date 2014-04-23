package org.dbpedia.spotlight.model

import org.dbpedia.spotlight.db.memory.MemoryCandidateMapStore
import scala.collection.mutable
import org.dbpedia.spotlight.io.EntityTopicDocument
import scala.util.Random
import java.util.concurrent.ConcurrentHashMap
import scala.collection.JavaConversions._
import com.esotericsoftware.kryo.Kryo
import java.io.{FileInputStream, FileOutputStream, File}
import scala.Array
import com.esotericsoftware.kryo.io.{Input, Output}

/**
 * @author dirk
 *         Date: 4/23/14
 *         Time: 12:16 PM
 */
class SimpleEntityTopicModel(val numTopics: Int, val numEntities: Int, val vocabularySize: Int, val numMentions: Int,
                             val alpha: Double, val beta: Double, val gamma: Double, val delta: Double, candMap: MemoryCandidateMapStore = null) {

  var entityTopicMatrix: Array[Array[Int]] = null
  var sparseMentionEntityMatrix: Array[ConcurrentHashMap[Int, Int]] = null
  var sparseWordEntityMatrix: Array[ConcurrentHashMap[Int, Int]] = null
  var topicCounts: Array[Int] = null
  var entityCounts: Array[Int] = null
  var assignmentCounts: Array[Int] = null

  if (candMap != null)
    init

  def init {
    sparseMentionEntityMatrix = Array.tabulate[ConcurrentHashMap[Int, Int]](candMap.candidates.size)(i => {
      if (candMap.candidates(i) != null) {
        val candCounts = candMap.candidateCounts(i)
        candMap.candidates(i).zip(candCounts.map(candMap.qc)).foldLeft(new ConcurrentHashMap[Int, Int]())((acc, p) => {
          acc.put(p._1, p._2); acc
        })
      }
      else new ConcurrentHashMap[Int, Int]()
    })

    entityTopicMatrix = Array.ofDim[Int](numTopics, numEntities)
    sparseWordEntityMatrix = Array.fill(vocabularySize)(new ConcurrentHashMap[Int, Int]())
    topicCounts = new Array[Int](numTopics)
    entityCounts = new Array[Int](numEntities)
    assignmentCounts = new Array[Int](numEntities)
  }

  def gibbsSampleDocument(doc: EntityTopicDocument, withUpdate: Boolean = false, init: Boolean = false) {
    sampleNewTopics(doc, withUpdate, init)
    sampleNewEntities(doc, withUpdate, init)
    sampleNewAssignments(doc, withUpdate, init)
  }

  private[model] def sampleNewTopics(doc: EntityTopicDocument, withUpdate: Boolean, init: Boolean) = {
    val docTopicCounts = doc.entityTopics.foldLeft(mutable.Map[Int, Int]())((acc, topic) => {
      if (topic >= 0)
        acc += (topic -> (acc.getOrElse(topic, 0) + 1))
      acc
    })

    (0 until doc.entityTopics.length).foreach(idx => {
      val entity = doc.mentionEntities(idx)
      val oldTopic = doc.entityTopics(idx)

      val newTopic = sampleFromProportionals(topic => {
        val add = {
          if (topic == oldTopic) -1 else 0
        }
        if (entity >= 0)
          (docTopicCounts.getOrElse(topic, 0) + add + alpha) *
            (entityTopicMatrix(topic)(entity) + add + beta) / (topicCounts(topic) + add + numTopics * beta)
        else
          (docTopicCounts.getOrElse(topic, 0) + add + alpha) *
            (add + beta) / (topicCounts(topic) + add + numTopics * beta)
      }, 0 until numTopics)

      doc.entityTopics(idx) = newTopic

      if (withUpdate && oldTopic != newTopic) {
        if (!init && oldTopic >= 0) {
          topicCounts(oldTopic) -= 1
          if (entity >= 0)
            entityTopicMatrix(oldTopic)(entity) -= 1
        }
        topicCounts(newTopic) += 1
        if (entity >= 0)
          entityTopicMatrix(newTopic)(entity) += 1
      }
    })
  }

  private[model] def sampleNewEntities(doc: EntityTopicDocument, withUpdate: Boolean, init: Boolean) = {
    //Sample new entities
    val docEntityCounts = doc.mentionEntities.foldLeft(mutable.Map[Int, Int]())((acc, entity) => {
      if (entity >= 0)
        acc += (entity -> (acc.getOrElse(entity, 0) + 1))
      acc
    })
    val docAssignmentCounts = doc.tokenEntities.foldLeft(mutable.Map[Int, Int]())((acc, entity) => {
      if (entity >= 0)
        acc += (entity -> (acc.getOrElse(entity, 0) + 1))
      acc
    })
    (0 until doc.mentionEntities.length).foreach(idx => {
      val oldEntity = doc.mentionEntities(idx)
      val mention = doc.mentions(idx)
      val topic = doc.entityTopics(idx)
      val mentionCounts = sparseMentionEntityMatrix(mention)

      //Keep original assignments in initialization phase
      val newEntity = if (init && oldEntity >= 0)
        oldEntity
      else {
        val entityTopicCounts = entityTopicMatrix(topic)
        val cands = candMap.candidates(mention)

        sampleFromProportionals(entity => {
          val add = {
            if (entity == oldEntity) -1 else 0
          }
          val cte = entityTopicCounts(entity)
          val cem = mentionCounts.get(entity)
          val ce = entityCounts(entity)

          (cte + add + beta) *
            (cem + add + gamma) / (ce + add + numMentions * gamma) *
            (0 until docAssignmentCounts.getOrElse(entity, 0)).foldLeft(1)((acc, _) => acc * (docEntityCounts(entity) + 1) / docEntityCounts(entity))
        }, cands)
      }

      doc.mentionEntities(idx) = newEntity

      if (withUpdate && oldEntity != newEntity) {
        if (!init && oldEntity >= 0) {
          entityTopicMatrix(topic)(oldEntity) -= 1
          mentionCounts.put(oldEntity, mentionCounts.get(oldEntity) - 1)
          entityCounts(oldEntity) = entityCounts(oldEntity) - 1
        }

        //It is possible, that no entity is found for that mention
        if (newEntity >= 0) {
          entityTopicMatrix(topic)(newEntity) += 1
          //Initialized with anchor counts, so no updates if oldEntity was an anchor and we are in init phase
          if (!init || oldEntity < 0)
            mentionCounts.put(newEntity, mentionCounts.get(newEntity) + 1)
          entityCounts(newEntity) = entityCounts(newEntity) + 1
        }
      }
    })
  }

  private[model] def sampleNewAssignments(doc: EntityTopicDocument, withUpdate: Boolean, init: Boolean) = {
    //Sample new assignments
    val docEntityCounts = doc.mentionEntities.foldLeft(mutable.Map[Int, Int]())((acc, entity) => {
      if (entity >= 0)
        acc += (entity -> (acc.getOrElse(entity, 0) + 1))
      acc
    })
    val candidateEntities = docEntityCounts.keySet
    if (!candidateEntities.isEmpty)
      (0 until doc.tokenEntities.length).foreach(idx => {
        val oldEntity = doc.tokenEntities(idx)
        val token = doc.tokens(idx)
        val entityTokenCounts = sparseWordEntityMatrix(token)

        val newEntity = sampleFromProportionals(entity => {
          val add = {
            if (entity == oldEntity) -1 else 0
          }
          docEntityCounts(entity) *
            (entityTokenCounts.getOrElse(entity, 0) + add + delta) / (assignmentCounts(entity) + add + vocabularySize * delta)
        }, candidateEntities)

        doc.tokenEntities(idx) = newEntity

        if (withUpdate && oldEntity != newEntity) {
          if (!init && oldEntity >= 0) {
            val oldCount = entityTokenCounts(oldEntity)
            if (oldCount == 1)
              entityTokenCounts.remove(oldEntity)
            else
              entityTokenCounts(oldEntity) = oldCount - 1
            assignmentCounts(oldEntity) -= 1
          }

          entityTokenCounts += newEntity -> (1 + entityTokenCounts.getOrElse(newEntity, 0))
          assignmentCounts(newEntity) += 1
        }
      })
  }

  private[model] def sampleFromProportionals(calcProportion: Int => Double, candidates: Traversable[Int]) = {
    var sum = 0.0
    val cands = candidates.map(cand => {
      var res = calcProportion(cand)
      sum += res
      (cand, res)
    })
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

object SimpleEntityTopicModel {
  private val kryo = new Kryo()

  kryo.register(classOf[Int])
  kryo.register(classOf[Double])
  kryo.register(classOf[Array[Int]])
  kryo.register(classOf[Array[Array[Int]]])
  kryo.register(classOf[Array[ConcurrentHashMap[Int, Int]]])

  def fromFile(file: File) {
    val in = new Input(new FileInputStream(file))
    val numTopics = kryo.readObject(in, classOf[Int])
    val numEntities = kryo.readObject(in, classOf[Int])
    val vocabularySize = kryo.readObject(in, classOf[Int])
    val numMentions = kryo.readObject(in, classOf[Int])

    val alpha = kryo.readObject(in, classOf[Double])
    val beta = kryo.readObject(in, classOf[Double])
    val gamma = kryo.readObject(in, classOf[Double])
    val delta = kryo.readObject(in, classOf[Double])

    val entityTopicMatrix = kryo.readObject(in, classOf[Array[Array[Int]]])
    val sparseWordEntityMatrix = kryo.readObject(in, classOf[Array[ConcurrentHashMap[Int, Int]]])
    val sparseMentionEntityMatrix = kryo.readObject(in, classOf[Array[ConcurrentHashMap[Int, Int]]])

    val topicCounts = kryo.readObject(in, classOf[Array[Int]])
    val entityCounts = kryo.readObject(in, classOf[Array[Int]])
    val assignmentCounts = kryo.readObject(in, classOf[Array[Int]])
    in.close()

    val model = new SimpleEntityTopicModel(numTopics, numEntities, vocabularySize, numMentions, alpha, beta, gamma, delta)
    model.assignmentCounts = assignmentCounts
    model.entityCounts = entityCounts
    model.entityTopicMatrix = entityTopicMatrix
    model.sparseMentionEntityMatrix = sparseMentionEntityMatrix
    model.sparseWordEntityMatrix = sparseWordEntityMatrix
    model.topicCounts = topicCounts
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

  def main(args:Array[String]) {
    val model = fromFile(new File("/home/dirk/model0"))
    model
  }
}
