package org.dbpedia.spotlight.train

import org.dbpedia.spotlight.io.EntityTopicDocument
import org.dbpedia.spotlight.storage.CountStore
import scala.util.Random
import scala.collection.mutable

/**
 * @author dirk
 *         Date: 4/9/14
 *         Time: 4:33 PM
 */
object SamplingFromCountStore {

  private def incr(key: String, value: Int, updates: mutable.Map[String, Int]) = updates += key -> (updates.getOrElse(key, 0) + value)

  def gibbsSampleDocument(doc: EntityTopicDocument, store: CountStore,
                          numTopics: Int, candidates: Array[Array[Int]], mentionSize: Int, vocabSize: Int,
                          alpha: Double, beta: Double, gamma: Double, delta: Double,
                          init: Boolean = false) = {

    (sampleNewTopics(doc, numTopics, store, alpha, beta, init) ++
      sampleNewEntities(doc, candidates, store, init, beta, gamma, mentionSize) ++
      sampleNewAssignments(doc, store, delta, vocabSize, init)).filterNot(_._2 == 0)
  }


  private[train] def sampleNewAssignments(doc: EntityTopicDocument, store: CountStore, delta: Double, vocabSize: Int, init: Boolean) = {
    val updates = mutable.Map[String, Int]()

    //Sample new assignments
    val (wordEntityCounts, entityCountsForWords) = getAssignmentCountsForDoc(doc, store)
    val docEntityCounts = doc.mentionEntities.foldLeft(mutable.Map[Int, Int]())((acc, entity) => {
      if (entity >= 0)
        acc += (entity -> (acc.getOrElse(entity, 0) + 1))
      acc
    })
    val candidateEntities = docEntityCounts.keySet
    (0 until doc.tokenEntities.length).par.foreach(idx => {
      val oldEntity = doc.tokenEntities(idx)
      val token = doc.tokens(idx)
      val newEntity = sampleFromProportionals(entity => {
        val add = {
          if (entity == oldEntity) -1 else 0
        }
        docEntityCounts(entity) *
          (wordEntityCounts(idx)(entity) + add + delta) / (entityCountsForWords(entity) + add + vocabSize * delta)
      }, candidateEntities)

      doc.tokenEntities(idx) = newEntity

      updates.synchronized {
        if (!init && oldEntity >= 0) {
          incr(getCEWKey(oldEntity, token), -1, updates)
          incr(getCEofWKey(oldEntity), -1, updates)
        }
        if (newEntity >= 0) {
          incr(getCEWKey(newEntity, token), 1, updates)
          incr(getCEofWKey(newEntity), 1, updates)
        }
      }
    })
    updates
  }

  private[train] def sampleNewEntities(doc: EntityTopicDocument, candidates: Array[Array[Int]], store: CountStore, init: Boolean, beta: Double, gamma: Double, mentionSize: Int) = {
    val updates = mutable.Map[String, Int]()

    //Sample new entities
    val entityCounts = getEntityCountsForDoc(doc, candidates, store)
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
      //Keep original assignments in initialization phase
      val newEntity = if (init && oldEntity >= 0)
        oldEntity
      else sampleFromProportionals(entity => {
        val add = {
          if (entity == oldEntity) -1 else 0
        }
        val (cte, cem, ce) = entityCounts(idx)(entity)
        (cte + add + beta) *
          (cem + add + gamma) / (ce + add + mentionSize * gamma) *
          (0 until docAssignmentCounts.getOrElse(entity, 0)).foldLeft(1)((acc, _) => acc * (docEntityCounts(entity) + 1) / docEntityCounts(entity))
      }, entityCounts(idx).keySet)

      doc.mentionEntities(idx) = newEntity

      //TODO missing topic counts update
      if (!init && oldEntity >= 0) {
        incr(getCEMKey(oldEntity, mention), -1, updates)
        incr(getCTEKey(topic, oldEntity), -1, updates)
        incr(getCEofMKey(oldEntity), -1, updates)
      }
      //It is possible, that no entity i found for that mention
      if (newEntity >= 0) {
        incr(getCEMKey(newEntity, mention), 1, updates)
        incr(getCTEKey(topic, newEntity), 1, updates)
        incr(getCEofMKey(newEntity), 1, updates)
      }
    })
    updates
  }

  private[train] def sampleNewTopics(doc: EntityTopicDocument, numTopics: Int, store: CountStore, alpha: Double, beta: Double, init: Boolean) = {
    val updates = mutable.Map[String, Int]()

    //Sample new topics
    val (topicCounts, topicEntityCounts) = getTopicCountsForDoc(doc, numTopics, store)
    val docTopicCounts = doc.entityTopics.foldLeft(mutable.Map[Int, Int]())((acc, topic) => {
      if (topic >= 0)
        acc += (topic -> (acc.getOrElse(topic, 0) + 1))
      acc
    })

    (0 until doc.entityTopics.length).foreach(idx => {
      val entity = doc.mentionEntities(idx)
      val oldTopic = doc.entityTopics(idx)
      val topicEntities = topicEntityCounts(idx)

      val newTopic = sampleFromProportionals(topic => {
        val add = {
          if (topic == oldTopic) -1 else 0
        }
        (docTopicCounts.getOrElse(topic, 0) + add + alpha) *
          (topicEntities(topic) + add + beta) / (topicCounts(topic) + add + numTopics * beta)
      }, 0 until numTopics)

      doc.entityTopics(idx) = newTopic

      if (!init && oldTopic >= 0) {
        incr(getCTEKey(oldTopic, entity), -1, updates)
        incr(getCTKey(oldTopic), -1, updates)
      }
      incr(getCTEKey(newTopic, entity), 1, updates)
      incr(getCTKey(newTopic), 1, updates)
    })

    updates
  }

  private[train] def sampleFromProportionals(calcProportion: Int => Double, candidates: Traversable[Int]) = {
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

  private val cte_prefix = "cte_"
  private val cem_prefix = "cem_"
  private val cew_prefix = "cew_"

  private def getCTEKey(topic: Int, entity: Int) = cte_prefix + topic + "-" + entity

  private def getCTKey(topic: Int) = cte_prefix + topic

  private def getCEMKey(entity: Int, mention: Int) = cem_prefix + entity + "-" + mention

  private def getCEofMKey(entity: Int) = cem_prefix + entity

  private def getCEWKey(entity: Int, word: Int) = cew_prefix + entity + "-" + word

  private def getCEofWKey(entity: Int) = cew_prefix + entity

  //ct* -> total counts of topics; cte -> array over all topics for each entity
  private[train] def getTopicCountsForDoc(doc: EntityTopicDocument, numTopics: Int, store: CountStore) = {
    val keys = mutable.Set[String]()
    var topic = 0
    //while is faster than for
    while (topic < numTopics) {
      keys += getCTKey(topic)
      doc.mentionEntities.foreach(entity => {
        if (entity >= 0)
          keys += getCTEKey(topic, entity)
      })
      topic += 1
    }

    val result = store.multiGet(keys)

    val topicCounts = Array.tabulate(numTopics)(topic => result(getCTKey(topic)).getOrElse(0))

    val entityTopicCounts = Array.tabulate(doc.mentionEntities.length)(idx => {
      val entity = doc.mentionEntities(idx)
      Array.tabulate(numTopics)(topic => {
        if (entity < 0) 0
        else result(getCTEKey(topic, entity)).getOrElse(0)
      })
    })

    (topicCounts, entityTopicCounts)
  }

  //e -> (cte; cem; ce*) | for all entities e for each document mention m_i
  private[train] def getEntityCountsForDoc(doc: EntityTopicDocument, candidates: Array[Array[Int]], store: CountStore): Array[scala.collection.Map[Int, (Int, Int, Int)]] = {
    val keys = mutable.Set[String]()
    var idx = 0
    //while is faster than for
    while (idx < doc.mentions.length) {
      val mention = doc.mentions(idx)
      val topic = doc.entityTopics(idx)
      candidates(mention).foreach {
        case entity =>
          keys += getCEMKey(entity, mention)
          keys += getCEofMKey(entity)
          keys += getCTEKey(topic, entity)
      }
      idx += 1
    }

    val result = store.multiGet(keys)

    idx = 0
    Array.tabulate(doc.mentions.length)(idx => {
      val mention = doc.mentions(idx)
      val topic = doc.entityTopics(idx)
      candidates(mention).foldLeft(mutable.Map[Int, (Int, Int, Int)]()) {
        case (map, entity) =>
          map += entity -> {
            val cem = result(getCEMKey(entity, mention)).getOrElse(0)
            val ce = result(getCEofMKey(entity)).getOrElse(0)
            val cte = result(getCTEKey(topic, entity)).getOrElse(0)
            (cte, cem, ce)
          }
          map
      }
    })
  }

  private[train] def getAssignmentCountsForDoc(doc: EntityTopicDocument, store: CountStore) = {
    val candidates = doc.mentionEntities.toSet
    val tokenSet = doc.tokens.toSet

    val keys = mutable.Set[String]()
    candidates.foreach(entity => {
      tokenSet.foreach(word => {
        keys += getCEWKey(entity, word)
      })
      keys += getCEofWKey(entity)
    })

    val result = store.multiGet(keys)

    val wordEntities = Array.tabulate(doc.tokens.length)(idx => {
      val word = doc.tokens(idx)
      val map = mutable.Map[Int, Int]()
      candidates.foreach(entity => map += entity -> result(getCEWKey(entity, word)).getOrElse(0))
      map
    })

    val entityCounts = mutable.Map[Int, Int]()
    candidates.foreach(entity =>
      entityCounts += entity -> result(getCEofWKey(entity)).getOrElse(0)
    )

    (wordEntities, entityCounts)
  }

}
