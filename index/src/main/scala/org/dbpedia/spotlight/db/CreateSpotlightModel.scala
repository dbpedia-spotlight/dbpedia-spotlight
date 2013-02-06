package org.dbpedia.spotlight.db

import io._
import java.io.{FileOutputStream, FileInputStream, File}
import memory.MemoryStore
import model.Tokenizer
import scala.io.Source
import org.tartarus.snowball.SnowballProgram
import java.util.Properties
import org.dbpedia.spotlight.io.WikipediaHeldoutCorpus
import org.apache.commons.io.FileUtils
import opennlp.tools.tokenize.{TokenizerModel, TokenizerME}
import opennlp.tools.sentdetect.{SentenceModel, SentenceDetectorME}
import opennlp.tools.postag.{POSModel, POSTaggerME}
import org.dbpedia.spotlight.spot.opennlp.OpenNLPSpotter
import opennlp.tools.chunker.ChunkerModel

/**
 * This script creates a Spotlight model folder from the results of
 * Apache Pig jobs. For a tutorial, see:
 *
 * https://github.com/dbpedia-spotlight/dbpedia-spotlight/wiki/Internationalization-(DB-backed-core)
 *
 * @author Joachim Daiber
 */

object CreateSpotlightModel {

  def main(args: Array[String]) {

    val (language: String, rawDataFolder: File, outputFolder: File, opennlpFolder: File, stopwordsFile: File, stemmer: SnowballProgram) = try {
      (
        args(0),
        new File(args(1)),
        new File(args(2)),
        new File(args(3)),
        new File(args(4)),
        if (args(5) equals "None") null else Class.forName("org.tartarus.snowball.ext.%s".format(args(5))).newInstance()
      )
    } catch {
      case e: Exception => {
        e.printStackTrace()
        System.err.println("Usage:")
        System.err.println(" - English:    mvn scala:run -DmainClass=org.dbpedia.spotlight.db.CreateSpotlightModel -Dexec.args=\"en /data/input /data/output /data/opennlp /data/stopwords.list EnglishStemmer\"")
        System.err.println(" - no stemmer: mvn scala:run -DmainClass=org.dbpedia.spotlight.db.CreateSpotlightModel -Dexec.args=\"en /data/input /data/output /data/opennlp /data/stopwords.list None\"")
        System.exit(1)
      }
    }

    if(!outputFolder.mkdir()) {
      System.err.println("Folder %s already exists, I am too afraid to overwrite it!".format(outputFolder.toString))
      System.exit(1)
    }

    FileUtils.copyFile(stopwordsFile, new File(outputFolder, "stopwords.list"))

    val opennlpModels = opennlpFolder.listFiles()
    def getModel(name: String) = opennlpModels.filter(_.getName.endsWith(name)).headOption

    val opennlpOut = new File(outputFolder, "opennlp")
    opennlpOut.mkdir()

    try {
      FileUtils.copyFile(getModel("-sent.bin").get, new File(opennlpOut, "sent.bin"))
      FileUtils.copyFile(getModel("-token.bin").get, new File(opennlpOut, "token.bin"))
      FileUtils.copyFile(getModel("-pos-maxent.bin").get, new File(opennlpOut, "pos-maxent.bin"))

      getModel("-chunker.bin") match {
        case Some(model) => FileUtils.copyFile(model, new File(opennlpOut, "chunker.bin"))
        case _ =>
      }
    } catch {
      case _: Exception => {
        System.err.println(
          """Problem with OpenNLP models:
            | You need to have at least the following model files in your opennlp folder:
            | *-sent.bin
            | *-token.bin
            | *-pos-maxent.bin
            |
            | For the best result, you should also have:
            | *-chunker.bin
          """.stripMargin)
        System.exit(1)
      }
    }


    val namespace = if (language.equals("en")) {
      "http://dbpedia.org/resource/"
    } else {
      "http://%s.dbpedia.org/resource/".format(language)
    }


    //Set default properties
    val defaultProperties = new Properties()
    defaultProperties.setProperty("stemmer",   args(5))
    defaultProperties.setProperty("namespace", namespace)


    defaultProperties.store(new FileOutputStream(new File(outputFolder, "model.properties")), null)

    //Create models:
    val modelDataFolder = new File(outputFolder, "model")
    modelDataFolder.mkdir()

    val memoryIndexer = new MemoryStoreIndexer(modelDataFolder)
    //val diskIndexer = new JDBMStoreIndexer(new File("data/"))

    val wikipediaToDBpediaClosure = new WikipediaToDBpediaClosure(
      namespace,
      new FileInputStream(new File(rawDataFolder, "redirects.nt")),
      new FileInputStream(new File(rawDataFolder, "disambiguations.nt"))
    )


    val onlpTokenizer = new TokenizerME(new TokenizerModel(new FileInputStream(new File(opennlpOut, "token.bin"))))

    memoryIndexer.tokenizer = Some(onlpTokenizer)
    memoryIndexer.addSurfaceForms(
      SurfaceFormSource.fromPigFiles(
        new File(rawDataFolder, "sfAndTotalCounts"),
        wikiClosure=wikipediaToDBpediaClosure
      )
    )

    memoryIndexer.addResources(
      DBpediaResourceSource.fromPigFiles(
        wikipediaToDBpediaClosure,
        new File(rawDataFolder, "uriCounts"),
        (if (new File(rawDataFolder, "instance_types.nt").exists())
          new File(rawDataFolder, "instance_types.nt")
        else if (new File(rawDataFolder, "instanceTypes.tsv").exists())
          new File(rawDataFolder, "instanceTypes.tsv")
        else
          null
        ),
        namespace
      )
    )

    val resStore = MemoryStore.loadResourceStore(new FileInputStream(new File(modelDataFolder, "res.mem")))
    val sfStore  = MemoryStore.loadSurfaceFormStore(new FileInputStream(new File(modelDataFolder, "sf.mem")))

    memoryIndexer.addCandidatesByID(
      CandidateMapSource.fromPigFiles(
        new File(rawDataFolder, "pairCounts"),
        wikipediaToDBpediaClosure,
        resStore,
        sfStore
      ),
      sfStore.size
    )

    memoryIndexer.addTokenTypes(
      TokenSource.fromPigFile(
        new File(rawDataFolder, "token_counts")
      )
    )

    val tokenStore = MemoryStore.loadTokenTypeStore(new FileInputStream(new File(modelDataFolder, "tokens.mem")))

    memoryIndexer.createContextStore(resStore.size)
    memoryIndexer.addTokenOccurrences(
      TokenOccurrenceSource.fromPigFile(
        new File(rawDataFolder, "token_counts"),
        tokenStore,
        wikipediaToDBpediaClosure,
        resStore
      )
    )
    memoryIndexer.writeTokenOccurrences()

    val stopwords = SpotlightModel.loadStopwords(outputFolder)

    //Tune Spotter:
    val tokenizer: Tokenizer = new DefaultTokenizer(
      onlpTokenizer,
      stopwords,
      stemmer,
      new SentenceDetectorME(new SentenceModel(new FileInputStream(new File(opennlpOut, "sent.bin")))),
      new POSTaggerME(new POSModel(new FileInputStream(new File(opennlpOut, "pos-maxent.bin")))),
      tokenStore
    )


    val spotter = new OpenNLPSpotter(
      Some(new ChunkerModel(new FileInputStream(new File(opennlpOut, "chunker.bin")))),
      List(),
      sfStore,
      stopwords,
      None,
      Set("NP", "MWU", "PP"), "N"
    )

    SpotterTuner.tuneOpenNLP(
      WikipediaHeldoutCorpus.fromFile(new File(rawDataFolder, "test.txt")),
      tokenizer,
      spotter,
      new File(outputFolder, "opennlp_chunker_thresholds.txt")
    )

  }

}
