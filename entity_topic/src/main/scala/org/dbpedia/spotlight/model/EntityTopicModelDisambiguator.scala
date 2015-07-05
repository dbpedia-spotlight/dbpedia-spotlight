package org.dbpedia.spotlight.model

import org.dbpedia.spotlight.disambiguate.ParagraphDisambiguator
import org.dbpedia.spotlight.exceptions.{SurfaceFormNotFoundException}
import org.dbpedia.spotlight.db.model.{TextTokenizer, SurfaceFormStore, ResourceStore}
import org.dbpedia.spotlight.db.memory.MemoryCandidateMapStore

/**
 * @author dirk
 *         Date: 5/15/14
 *         Time: 9:41 AM
 */
class EntityTopicModelDisambiguator(val model:SimpleEntityTopicModel,
                                    val resStore:ResourceStore,
                                    val sfStore:SurfaceFormStore,
                                    val candMap:MemoryCandidateMapStore = null,
                                    val burnIn:Int = 10,
                                    val nrOfSamples:Int = 100,
                                    val tokenizer:TextTokenizer = null) extends ParagraphDisambiguator {

  model.candMap = candMap

  /**
   * Executes disambiguation per paragraph (collection of occurrences).
   * Can be seen as a classification task: unlabeled instances in, labeled instances out.
   *
   * @param paragraph
   * @return
   * @throws SearchException
   * @throws InputException
   */
  def disambiguate(paragraph: Paragraph) = {
    val best = bestK(paragraph,1)
    paragraph.occurrences.map(sfOcc => best.get(sfOcc).map(_.headOption.getOrElse(null)).getOrElse(null))
  }

  /**
   * Executes disambiguation per occurrence, returns a list of possible candidates.
   * Can be seen as a ranking (rather than classification) task: query instance in, ranked list of target URIs out.
   *
   * @param sfOccurrences
   * @param k
   * @return
   * @throws SearchException
   * @throws ItemNotFoundException    when a surface form is not in the index
   * @throws InputException
   */
  def bestK(paragraph: Paragraph, k: Int): Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]] = {
    paragraph.occurrences.foreach(occ => {
      try {
        if(occ.surfaceForm.id < 0)
          occ.surfaceForm.id = sfStore.getSurfaceForm(occ.surfaceForm.name).id
      }
      catch {
        case e:SurfaceFormNotFoundException => //Nothing to do here
      }
    })

    if(tokenizer != null)
      tokenizer.tokenizeMaybe(paragraph.text)

    val doc = EntityTopicDocument.fromParagraph(paragraph)
    //Burn In
    model.gibbsSampleDocument(doc,burnIn)

    //Take samples
    val stats = model.gibbsSampleDocument(doc, nrOfSamples, returnStatistics = true)

    paragraph.occurrences.filter(_.surfaceForm.id >= 0).zip(stats).foldLeft(Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]]()) {
      case (acc,(sfOcc,counts)) =>
        var chosen = counts.toSeq.sortBy(-_._2).take(k)

        if(chosen.size < k && candMap != null) {
          val diff = k - chosen.size
          val cands = candMap.getCandidates(sfOcc.surfaceForm)
          if(cands != null && !cands.isEmpty) {
            val rest = cands.filter(r => !counts.contains(r.resource.id)).take(diff)
            chosen ++= rest.map(r => (r.resource.id,0))
          }
        }

        acc + (sfOcc -> chosen.flatMap {case (resId, count) => if(resId >= 0) Some(new DBpediaResourceOccurrence(
          "",resStore.getResource(resId),
          sfOcc.surfaceForm,
          sfOcc.context,
          sfOcc.textOffset,
          contextualScore = count.toDouble/nrOfSamples))
          else None
        }.toList)
    }
  }

  /**
   * Every disambiguator has a name that describes its settings (used in evaluation to compare results)
   * @return a short description of the Disambiguator
   */
  def name = "Entity-Topic-Model-Disambiguator"

}
