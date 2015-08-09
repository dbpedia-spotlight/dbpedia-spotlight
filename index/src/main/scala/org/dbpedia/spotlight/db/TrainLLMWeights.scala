package org.dbpedia.spotlight.db

import java.io.{PrintWriter, File, FileInputStream}
import java.util.Locale

import org.dbpedia.spotlight.db.io.ranklib.{TrainingDataEntry, RanklibTrainingDataWriter}
import org.dbpedia.spotlight.db.memory.{MemoryQuantizedCountStore, MemoryStore}
import org.dbpedia.spotlight.db.model._
import org.dbpedia.spotlight.db.similarity.VectorContextSimilarity
import org.dbpedia.spotlight.db.stem.SnowballStemmer
import org.dbpedia.spotlight.disambiguate.mixtures.UnweightedMixture
import org.dbpedia.spotlight.io.WikipediaHeldoutCorpus
import org.dbpedia.spotlight.model._

/**
 * Created by dowling on 06/08/15.
 */
object TrainLLMWeights {
  def main(args: Array[String]) {

    val (localeCode: String, rawDataFolder: File, outputFolder: File) = try {
      (
        args(0),
        new File(args(0)), // raw data folder
        new File(args(1)) // output folder
        )
    } catch {
      case e: Exception => {
        e.printStackTrace()
        System.err.println("Usage:")
        System.err.println("mvn scala:run -DmainClass=org.dbpedia.spotlight.db.TrainLLMWeights -Dexec.args=\"en_US /data/input /data/output\"")
        System.exit(1)
      }
    }
    val modelDataFolder = new File(outputFolder, "model")
    val (tokenStore, sfStore, resStore, candidateMapStore, contextStore, memoryVectorStore) = SpotlightModel.storesFromFolder(modelDataFolder)

    val quantizedCountStore = new MemoryQuantizedCountStore()
    val memoryIndexer = new MemoryStoreIndexer(modelDataFolder, quantizedCountStore)

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

    // generate training data for ranklib so we can estimate the log-linear model parameters
    val memoryCandidateMapStore = MemoryStore.loadCandidateMapStore(
      new FileInputStream(new File(modelDataFolder, "candmap.mem")),
      resStore,
      quantizedCountStore
    )
    val dBCandidateSearcher = new DBCandidateSearcher(resStore, sfStore, memoryCandidateMapStore)

    // TODO need the real file here
    val heldoutCorpus = WikipediaHeldoutCorpus.fromFile(new File("heldout.txt"), wikipediaToDBpediaClosure, dBCandidateSearcher)
    var qid = 0

    // cycle through heldout data, make annotations, create result objects with ground truth, write to file
    val disambiguator = new DBTwoStepDisambiguator(
      tokenStore,
      sfStore,
      resStore,
      dBCandidateSearcher,
      new UnweightedMixture(Set("P(e)", "P(c|e)", "P(s|e)")),
      new VectorContextSimilarity(tokenStore,
        MemoryStore.loadVectorStore(new FileInputStream(new File(modelDataFolder, "vectors.mem")))
      )
    )
    val ranklibOutputWriter = new RanklibTrainingDataWriter(new PrintWriter("ranklib-data.txt"))

    heldoutCorpus.foreach {
      annotatedParagraph: AnnotatedParagraph => // we want to iterate through annotatedParagraph and the non-annotated version in parallel
        val paragraph = Factory.Paragraph.from(annotatedParagraph)

        paragraph.occurrences.foreach { aSfOcc =>
          val tokenizer: TextTokenizer = disambiguator.asInstanceOf[DBTwoStepDisambiguator].tokenizer
          val contextSimilarity = disambiguator.asInstanceOf[DBTwoStepDisambiguator].contextSimilarity

          aSfOcc.setFeature(
            new Feature(
              "token_types",
              tokenizer.tokenize(new Text(aSfOcc.surfaceForm.name)).map(_.tokenType).toArray
            )
          )
        }
        val bestK: Map[SurfaceFormOccurrence, List[DBpediaResourceOccurrence]] = disambiguator.bestK(paragraph, 100)
        val goldOccurrences = annotatedParagraph.occurrences.toTraversable // TODO this is filtered or something in eval

        goldOccurrences.foreach(correctOccurrence => {
          if (wikipediaToDBpediaClosure != null)
            correctOccurrence.resource.uri = wikipediaToDBpediaClosure.getEndOfChainURI(correctOccurrence.resource.uri)

          val disambResult = new TrainingDataEntry(
            correctOccurrence, // correct
            bestK.getOrElse(
              Factory.SurfaceFormOccurrence.from(correctOccurrence), // predicted
              List[DBpediaResourceOccurrence]()
            )
          )
          ranklibOutputWriter.write(disambResult)
        })
    }
  }
}

