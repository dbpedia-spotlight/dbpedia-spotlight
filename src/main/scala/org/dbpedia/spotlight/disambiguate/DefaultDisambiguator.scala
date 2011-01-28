package org.dbpedia.spotlight.disambiguate

import org.dbpedia.spotlight.lucene.disambiguate.MergedOccurrencesDisambiguator
import org.dbpedia.spotlight.lucene.LuceneManager
import org.dbpedia.spotlight.lucene.search.MergedOccurrencesContextSearcher
import org.dbpedia.spotlight.model.{DBpediaResource, SurfaceForm, SurfaceFormOccurrence, DBpediaResourceOccurrence}
import java.io.File
import org.dbpedia.spotlight.lucene.similarity._

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 20.09.2010
 * Time: 16:31:30
 * To change this template use File | Settings | File Templates.
 */

class DefaultDisambiguator(val indexDir : File) extends Disambiguator
{

    // Disambiguator
    val dir = LuceneManager.pickDirectory(indexDir)

    //val luceneManager = new LuceneManager(dir)                              // use this if surface forms in the index are case-sensitive
    val luceneManager = new LuceneManager.CaseInsensitiveSurfaceForms(dir)  // use this if all surface forms in the index are lower-cased
    val cache = new JCSTermCache(luceneManager);
    luceneManager.setContextSimilarity(new CachedInvCandFreqSimilarity(cache))        // set most successful Similarity
    //luceneManager.setContextSimilarity(new NewSimilarity(cache))        // set most successful Similarity

    val contextSearcher = new MergedOccurrencesContextSearcher(luceneManager)
    val disambiguator : Disambiguator = new MergedOccurrencesDisambiguator(contextSearcher)


  def spotProbability(sfOccurrences: java.util.List[SurfaceFormOccurrence]): java.util.List[SurfaceFormOccurrence] = {
      disambiguator.spotProbability(sfOccurrences)
  }

    def disambiguate(sfOccurrences: java.util.List[SurfaceFormOccurrence]): java.util.List[DBpediaResourceOccurrence] = {
        disambiguator.disambiguate(sfOccurrences)
    }

    def bestK(sfOccurrence: SurfaceFormOccurrence, k: Int): java.util.List[DBpediaResourceOccurrence] = {
        disambiguator.bestK(sfOccurrence, k)
    }

    def name() : String = {
        "Default:"+disambiguator.name
    }

    def ambiguity(sf : SurfaceForm) : Int = {
        disambiguator.ambiguity(sf)
    }

    def trainingSetSize(resource : DBpediaResource) : Int = {
        disambiguator.trainingSetSize(resource)
    }

}