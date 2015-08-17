package org.dbpedia.spotlight.db

import java.io.{PrintWriter, File, FileInputStream}
import java.util
import java.util.{Properties, Locale}


import org.dbpedia.spotlight.db.io.ranklib.{TrainingDataEntry, RanklibTrainingDataWriter}
import org.dbpedia.spotlight.db.memory.{MemoryQuantizedCountStore, MemoryStore}
import org.dbpedia.spotlight.db.model._
import org.dbpedia.spotlight.db.similarity.VectorContextSimilarity
import org.dbpedia.spotlight.db.stem.SnowballStemmer
import org.dbpedia.spotlight.db.tokenize.LanguageIndependentTokenizer
import org.dbpedia.spotlight.disambiguate.mixtures.UnweightedMixture
import org.dbpedia.spotlight.io.WikipediaHeldoutCorpus
import org.dbpedia.spotlight.model._

/**
 * Created by dowling on 06/08/15.
 */
object CreateLLMTrainData {
  def main(args: Array[String]) {

    val (localeCode: String, rawDataFolder: File, outputFolder: File) = try {
      (
        args(0),
        new File(args(1)), // raw data folder
        new File(args(2)) // output folder
        )
    } catch {
      case e: Exception => {
        e.printStackTrace()
        System.err.println("Usage:")
        System.err.println("mvn scala:run -DmainClass=org.dbpedia.spotlight.db.TrainLLMWeights -Dexec.args=\"en_US /data/input /data/output\"")
        System.exit(1)
      }
    }

    val modelStoresFolder = new File(outputFolder, "model")
    val (tokenStore, sfStore, resStore, candidateMapStore, contextStore, memoryVectorStore) = SpotlightModel.storesFromFolder(outputFolder)

    val properties = new Properties()
    properties.load(new FileInputStream(new File(outputFolder, "model.properties")))

    val quantizedCountStore = new MemoryQuantizedCountStore()
    //val memoryIndexer = new MemoryStoreIndexer(modelStoresFolder, quantizedCountStore)

    val Array(lang, country) = localeCode.split("_")
    val locale = new Locale(lang, country)

    val namespace = if (locale.getLanguage.equals("en")) {
      "http://dbpedia.org/resource/"
    } else {
      "http://%s.dbpedia.org/resource/".format(locale.getLanguage)
    }

    val wikipediaToDBpediaClosure = new WikipediaToDBpediaClosure(
      namespace,
      new FileInputStream(new File(rawDataFolder, "redirects.nt")),
      new FileInputStream(new File(rawDataFolder, "disambiguations.nt"))
    )

    //// generate training data for ranklib so we can estimate the log-linear model parameters

    val dBCandidateSearcher = new DBCandidateSearcher(resStore, sfStore, candidateMapStore)

    val heldoutCorpus = WikipediaHeldoutCorpus.fromFile(new File(rawDataFolder, "heldout.txt"), wikipediaToDBpediaClosure, dBCandidateSearcher)

    var qid = 0

    // cycle through heldout data, make annotations, create result objects with ground truth, write to file
    val disambiguator = new DBTwoStepDisambiguator(
      tokenStore,
      sfStore,
      resStore,
      dBCandidateSearcher,
      new UnweightedMixture(SpotlightModel.featureNames.toSet),
      new VectorContextSimilarity(tokenStore, memoryVectorStore)
    )

    def stemmer(): Stemmer = properties.getProperty("stemmer") match {
      case s: String if s equals "None" => null
      case s: String => new SnowballStemmer(s)
    }

    val tokenizer: TextTokenizer = new LanguageIndependentTokenizer(
      scala.io.Source.fromFile(new File(outputFolder, "stopwords.list")).getLines().map(_.trim()).toSet,
      stemmer(),
      locale,
      tokenStore
    )
    disambiguator.asInstanceOf[DBTwoStepDisambiguator].tokenizer = tokenizer
    val ranklibOutputWriter = new RanklibTrainingDataWriter(new PrintWriter(new File(outputFolder, "ranklib-training-data.txt")))

    heldoutCorpus.foreach {
      annotatedParagraph: AnnotatedParagraph => // we want to iterate through annotatedParagraph and the non-annotated version in parallel
        val paragraph = Factory.Paragraph.from(annotatedParagraph)

        paragraph.occurrences.foreach { aSfOcc =>

          //val contextSimilarity = disambiguator.asInstanceOf[DBTwoStepDisambiguator].contextSimilarity

          aSfOcc.setFeature(
            new Feature(
              "token_types",
              tokenizer.tokenize(new Text(aSfOcc.surfaceForm.name)).map(_.tokenType).toArray
            )
          )
        }
        val bestK: Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]] = disambiguator.bestK(paragraph, 100)

        val goldOccurrences = annotatedParagraph.occurrences.toTraversable // TODO this is filtered or something in eval

        goldOccurrences.foreach((correctOccurrence: DBpediaResourceOccurrence) => {

          val correctSF: SurfaceForm = sfStore.getSurfaceForm(correctOccurrence.surfaceForm.name)

          //val fixedOcc = new DBpediaResourceOccurrence(correctOccurrence.id, correctOccurrence.resource, correctSF, correctOccurrence.context, correctOccurrence.textOffset)

          val predicted: SurfaceFormOccurrence = bestK.keys.find(_.surfaceForm equals correctSF).orNull
          // val predicted: SurfaceFormOccurrence = Factory.SurfaceFormOccurrence.from(fixedOcc) // predicted

          if (predicted == null)
            println("Couldn't match %s to any in bestK".format(correctSF))
          //if (wikipediaToDBpediaClosure != null)
          //  correctOccurrence.resource.uri = wikipediaToDBpediaClosure.getEndOfChainURI(correctOccurrence.resource.uri)
          val result: List[DBpediaResourceOccurrence] = bestK.getOrElse(
            predicted,
            List[DBpediaResourceOccurrence]()
          )
          val disambResult = new TrainingDataEntry(
            correctOccurrence, // correct
            result
          )
          ranklibOutputWriter.write(disambResult)

        })
    }
  }
}

