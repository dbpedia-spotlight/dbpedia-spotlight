package org.dbpedia.spotlight.evaluation

import org.dbpedia.spotlight.io.AnnotatedTextSource
import org.dbpedia.spotlight.corpus.{CSAWCorpus, MilneWittenCorpus}
import java.io.{FileInputStream, File}
import org.dbpedia.spotlight.model.{AnnotatedParagraph, DBpediaResourceOccurrence, EntityTopicModelDisambiguator, SimpleEntityTopicModel}
import org.dbpedia.spotlight.db.{DBTwoStepDisambiguator, WikipediaToDBpediaClosure, SpotlightModel}
import java.util.Locale
import org.dbpedia.spotlight.db.tokenize.LanguageIndependentTokenizer
import org.dbpedia.spotlight.db.stem.SnowballStemmer
import org.dbpedia.spotlight.exceptions.{SurfaceFormNotFoundException, DBpediaResourceNotFoundException, NotADBpediaResourceException}
import org.dbpedia.spotlight.db.memory.MemoryCandidateMapStore
import org.dbpedia.spotlight.filter.occurrences.OccurrenceFilter
import org.dbpedia.spotlight.model.SpotlightConfiguration.DisambiguationPolicy
import org.dbpedia.spotlight.db.model.{CandidateMapStore, ResourceStore, SurfaceFormStore}

/**
 * @author dirk
 *          Date: 5/16/14
 *          Time: 12:47 PM
 */
object EvaluateDBDisambiguator {

  def main(args:Array[String]) {
    val corpus:AnnotatedTextSource =  {
      val c = MilneWittenCorpus.fromDirectory(new File(args(0)))

      if(c.documents.isEmpty)
        CSAWCorpus.fromDirectory(new File(args(0)))
      else
        c
    }

    val modelDir = new File(args(1))

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

    val model = SpotlightModel.fromFolder(modelDir)

    val disambiguator = model.disambiguators.get(DisambiguationPolicy.Default).disambiguator.asInstanceOf[DBTwoStepDisambiguator]
    disambiguator.tokenizer = model.tokenizer
    val baseline = new DBBaselineDisambiguator(disambiguator.candidateSearcher.sfStore,
      disambiguator.candidateSearcher.resStore,
      disambiguator.candidateSearcher.candidateMap)

    val mappedCorpus = corpus.mapConserve(p => {
      p.occurrences.foreach(occ => {
        try {
          val uri = wikipediaToDBpediaClosure.wikipediaToDBpediaURI(occ.resource.uri)
          occ.resource.id = disambiguator.candidateSearcher.resStore.getResourceByName(uri).id
        }
        catch {
          case e:NotADBpediaResourceException => //
          case e:DBpediaResourceNotFoundException => //
        }
      } )
      p
    })

    EvaluateParagraphDisambiguator.evaluate(mappedCorpus  , disambiguator, List(),List(new OccurrenceFilter {
      //TODO move to OccurrenceAnnotationFilter, since it looks at each occurrence at a time
      def touchOcc(occ: DBpediaResourceOccurrence) = {
        try {
          disambiguator.candidateSearcher.sfStore.getSurfaceForm(occ.surfaceForm.name)
          if(occ.resource.id >= 0)
            Some(occ)
          else
            None
        } catch {
          case e:SurfaceFormNotFoundException => None
        }
      }
    }))

    EvaluateParagraphDisambiguator.evaluate(mappedCorpus  , baseline, List(),List(new OccurrenceFilter {
      //TODO move to OccurrenceAnnotationFilter, since it looks at each occurrence at a time
      def touchOcc(occ: DBpediaResourceOccurrence) = {
        try {
          disambiguator.candidateSearcher.sfStore.getSurfaceForm(occ.surfaceForm.name)
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
