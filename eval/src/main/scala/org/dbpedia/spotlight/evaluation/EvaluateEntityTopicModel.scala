package org.dbpedia.spotlight.evaluation

import org.dbpedia.spotlight.corpus.{CSAWCorpus, MilneWittenCorpus}
import java.io.{FileInputStream, File}
import org.dbpedia.spotlight.model.{DBpediaResourceOccurrence, AnnotatedParagraph, EntityTopicModelDisambiguator, SimpleEntityTopicModel}
import org.dbpedia.spotlight.db.{FSASpotter, WikipediaToDBpediaClosure, SpotlightModel}
import java.util.Locale
import org.dbpedia.spotlight.db.memory.{MemoryCandidateMapStore, MemoryStore}
import org.dbpedia.spotlight.spot.Spotter
import org.dbpedia.spotlight.db.tokenize.LanguageIndependentTokenizer
import org.dbpedia.spotlight.db.stem.SnowballStemmer
import org.dbpedia.spotlight.exceptions.{SurfaceFormNotFoundException, DBpediaResourceNotFoundException, NotADBpediaResourceException}
import org.dbpedia.spotlight.filter.occurrences.OccurrenceFilter
import org.dbpedia.spotlight.io.AnnotatedTextSource

/**
 * @author dirk
 *          Date: 5/15/14
 *          Time: 10:19 AM
 */
object EvaluateEntityTopicModel {

  def main(args:Array[String]) {
    val corpus:AnnotatedTextSource =  {
      val c = MilneWittenCorpus.fromDirectory(new File(args(0)))

      if(c.documents.isEmpty)
        CSAWCorpus.fromDirectory(new File(args(0)))
      else
        c
    }

    val modelDir = new File(args(1))
    val entityTopicModel = SimpleEntityTopicModel.fromFile(new File(args(2)))
    val burnIn = if(args.size > 3) args(3).toInt else 10

    val (tokenStore, sfStore, resStore, candMap, _) = SpotlightModel.storesFromFolder(modelDir)

    val locale = new Locale("en", "US")
    val namespace = if (locale.getLanguage.equals("en")) {
      "http://dbpedia.org/resource/"
    } else {
      "http://%s.dbpedia.org/resource/"
    }
    val wikipediaToDBpediaClosure = new WikipediaToDBpediaClosure(
      namespace,
      new FileInputStream(new File(modelDir, "redirects.nt")),
      new FileInputStream(new File(modelDir, "disambiguations.nt"))
    )

    val stopwords: Set[String] = SpotlightModel.loadStopwords(modelDir)

    val tokenizer =
      new LanguageIndependentTokenizer(stopwords, new SnowballStemmer("EnglishStemmer"), locale, tokenStore)


    val mappedCorpus = corpus.mapConserve(p => {
      p.occurrences.foreach(occ => {
        try {
          val uri = wikipediaToDBpediaClosure.wikipediaToDBpediaURI(occ.resource.uri)
          occ.resource.id = resStore.getResourceByName(uri).id
        }
        catch {
          case e:NotADBpediaResourceException => //
          case e:DBpediaResourceNotFoundException => //
        }
      } )
      p
    })

    val disambiguator = new EntityTopicModelDisambiguator(entityTopicModel,resStore,sfStore,candMap.asInstanceOf[MemoryCandidateMapStore], tokenizer = tokenizer,burnIn = burnIn)

    EvaluateParagraphDisambiguator.evaluate(mappedCorpus, disambiguator, List(),List(new OccurrenceFilter {
      //TODO move to OccurrenceAnnotationFilter, since it looks at each occurrence at a time
      def touchOcc(occ: DBpediaResourceOccurrence) = {
        try {
          sfStore.getSurfaceForm(occ.surfaceForm.name)
          if(occ.resource.id >= 0)
            Some(occ)
          else
            None
        } catch {
          case e:SurfaceFormNotFoundException => None
        }
      }
    }))

  }

}
