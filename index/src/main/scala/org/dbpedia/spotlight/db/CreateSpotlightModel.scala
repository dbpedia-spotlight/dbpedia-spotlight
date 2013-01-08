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
import org.dbpedia.spotlight.spot.opennlp.OpenNLPChunkerSpotterDB

/**
 * @author Joachim Daiber
 */

object CreateSpotlightModel {

  def main(args: Array[String]) {

    val (rawDataFolder: File, outputFolder: File, opennlpFolder: File, stopwordsFile: File, stemmer: SnowballProgram) = try {
      (
        new File(args(0)),
        new File(args(1)),
        new File(args(2)),
        new File(args(3)),
        if (args(4) equals "None") null else Class.forName("org.tartarus.snowball.ext.%s".format(args(4))).newInstance()
      )
    } catch {
      case e: Exception => {
        e.printStackTrace()
        System.err.println("Usage:")
        System.err.println(" - English:    mvn scala:run -DmainClass=org.dbpedia.spotlight.db.CreateSpotlightModel -Dexec.args=\"/data/input /data/output /data/opennlp /data/stopwords.list EnglishStemmer\"")
        System.err.println(" - no stemmer: mvn scala:run -DmainClass=org.dbpedia.spotlight.db.CreateSpotlightModel -Dexec.args=\"/data/input /data/output /data/opennlp /data/stopwords.list None\"")
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
    //TODO: add NER

    //Set default properties
    val defaultProperties = new Properties()
    defaultProperties.setProperty("stemmer", args(4))

    defaultProperties.store(new FileOutputStream(new File(outputFolder, "model.properties")), null)

    //Create models:
    val modelDataFolder = new File(outputFolder, "model")
    modelDataFolder.mkdir()

    val memoryIndexer = new MemoryStoreIndexer(modelDataFolder)
    //val diskIndexer = new JDBMStoreIndexer(new File("data/"))

    val wikipediaToDBpediaClosure = new WikipediaToDBpediaClosure(
      new FileInputStream(new File(rawDataFolder, "redirects.nt")),
      new FileInputStream(new File(rawDataFolder, "disambiguations.nt"))
    )

    memoryIndexer.addResources(
      DBpediaResourceSource.fromPigFiles(
        wikipediaToDBpediaClosure,
        new File(rawDataFolder, "uriCounts"),
        null //new File("raw_data/pig/instanceTypes.tsv")
      )
    )

    val resStore = MemoryStore.loadResourceStore(new FileInputStream(new File(modelDataFolder, "res.mem")))

    memoryIndexer.addSurfaceForms(
      SurfaceFormSource.fromPigFiles(
        new File(rawDataFolder, "sfAndTotalCounts"),
        wikiClosure=wikipediaToDBpediaClosure,
        resStore
      )
    )

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
      new TokenizerME(new TokenizerModel(new FileInputStream(new File(opennlpOut, "token.bin")))),
      stopwords,
      stemmer,
      new SentenceDetectorME(new SentenceModel(new FileInputStream(new File(opennlpOut, "sent.bin")))),
      new POSTaggerME(new POSModel(new FileInputStream(new File(opennlpOut, "pos-maxent.bin")))),
      tokenStore
    )

    val spotter = new OpenNLPChunkerSpotterDB(
      new FileInputStream(new File(opennlpOut, "chunker.bin")),
      sfStore,
      stopwords,
      None,
      Set("NP", "MWU", "PP"), "N"
    )

    SpotterTuner.tuneOpenNLP(
      new WikipediaHeldoutCorpus(Source.fromFile(new File(rawDataFolder, "test.txt")).getLines()),
      tokenizer,
      spotter,
      new File(outputFolder, "opennlp_chunker_thresholds.txt")
    )

  }

}
