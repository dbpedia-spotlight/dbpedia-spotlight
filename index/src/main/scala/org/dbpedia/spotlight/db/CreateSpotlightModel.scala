package org.dbpedia.spotlight.db

import io._
import java.io.{FileOutputStream, FileInputStream, File}
import memory.MemoryStore
import model.{TextTokenizer, StringTokenizer, Stemmer}
import scala.io.Source
import org.tartarus.snowball.SnowballProgram
import java.util.{Locale, Properties}
import org.dbpedia.spotlight.io.WikipediaHeldoutCorpus
import org.apache.commons.io.FileUtils
import opennlp.tools.tokenize.{TokenizerModel, TokenizerME}
import opennlp.tools.sentdetect.{SentenceModel, SentenceDetectorME}
import opennlp.tools.postag.{POSModel, POSTaggerME}
import opennlp.tools.chunker.ChunkerModel
import stem.SnowballStemmer
import tokenize._
import scala.Some

/**
 * This script creates a Spotlight model folder from the results of
 * Apache Pig jobs. For a tutorial, see:
 *
 * https://github.com/dbpedia-spotlight/dbpedia-spotlight/wiki/Internationalization-(DB-backed-core)
 *
 * @author Joachim Daiber
 */

object CreateSpotlightModel {

  val OPENNLP_FOLDER = "opennlp"

  def main(args: Array[String]) {

    val (localeCode: String, rawDataFolder: File, outputFolder: File, opennlpFolder: Option[File], stopwordsFile: File, stemmer: Stemmer) = try {
      (
        args(0),
        new File(args(1)),
        new File(args(2)),
        if (args(3) equals "None") None else Some(new File(args(3))),
        new File(args(4)),
        if (args(5) equals "None") new Stemmer() else new SnowballStemmer(args(5))
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

    val Array(lang, country) = localeCode.split("_")
    val locale = new Locale(lang, country)

    if(!outputFolder.mkdir()) {
      System.err.println("Folder %s already exists, I am too afraid to overwrite it!".format(outputFolder.toString))
      System.exit(1)
    }

    FileUtils.copyFile(stopwordsFile, new File(outputFolder, "stopwords.list"))


    if (opennlpFolder.isDefined) {

      val opennlpModels = opennlpFolder.get.listFiles()
      val opennlpOut = new File(outputFolder, OPENNLP_FOLDER)
      opennlpOut.mkdir()

      def getModel(name: String) = opennlpModels.filter(_.getName.endsWith(name)).headOption

      try {
        FileUtils.copyFile(getModel("-token.bin").get, new File(opennlpOut, "token.bin"))
        FileUtils.copyFile(getModel("-sent.bin").get, new File(opennlpOut, "sent.bin"))
      } catch {
        case _: Exception => {
          System.err.println(
            """Problem with OpenNLP models:
              | You need to have at least the following model files in your opennlp folder:
              | *-sent.bin
              | *-token.bin
              |
              | For the best result, you should also have:
              | *-chunker.bin
              | *-pos-maxent.bin
            """.stripMargin)
          System.exit(1)
        }
      }

      try {
        FileUtils.copyFile(getModel("-pos-maxent.bin").get, new File(opennlpOut, "pos-maxent.bin"))
      } catch {
        case _: Exception => //Ignore
      }

      try {
        getModel("-chunker.bin") match {
          case Some(model) => FileUtils.copyFile(model, new File(opennlpOut, "chunker.bin"))
          case _ =>
        }
      } catch {
        case _: Exception => //Ignore
      }

    }

    val rawTokenizer: StringTokenizer = if (opennlpFolder.isDefined) {
      val opennlpOut = new File(outputFolder, OPENNLP_FOLDER)
      val onlpTokenizer = new TokenizerME(new TokenizerModel(new FileInputStream(new File(opennlpOut, "token.bin"))))

      new OpenNLPStringTokenizer(
        onlpTokenizer,
        stemmer
      )

    } else {
      new LanguageIndependentStringTokenizer(locale, stemmer)
    }


    val namespace = if (locale.getLanguage.equals("en")) {
      "http://dbpedia.org/resource/"
    } else {
      "http://%s.dbpedia.org/resource/".format(locale.getLanguage)
    }


    //Set default properties
    val defaultProperties = new Properties()
    defaultProperties.setProperty("stemmer",   args(5))
    defaultProperties.setProperty("namespace", namespace)
    defaultProperties.setProperty("locale", localeCode)


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

    memoryIndexer.tokenizer = Some(rawTokenizer)
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
        new File(rawDataFolder, "tokenCounts"),
        additionalTokens = Some(TokenSource.fromSFStore(sfStore, rawTokenizer))
      )
    )

    val tokenStore = MemoryStore.loadTokenTypeStore(new FileInputStream(new File(modelDataFolder, "tokens.mem")))

    memoryIndexer.createContextStore(resStore.size)
    memoryIndexer.addTokenOccurrences(
      TokenOccurrenceSource.fromPigFile(
        new File(rawDataFolder, "tokenCounts"),
        tokenStore,
        wikipediaToDBpediaClosure,
        resStore
      )
    )
    memoryIndexer.writeTokenOccurrences()


    val tokenizer: TextTokenizer = if (opennlpFolder.isDefined) {
      val opennlpOut = new File(outputFolder, OPENNLP_FOLDER)
      val oToken = new TokenizerME(new TokenizerModel(new FileInputStream(new File(opennlpOut, "token.bin"))))
      val oSent = new SentenceDetectorME(new SentenceModel(new FileInputStream(new File(opennlpOut, "sent.bin"))))

      new OpenNLPTokenizer(
        oToken,
        Set[String](),
        stemmer,
        oSent,
        null,
        tokenStore
      )

    } else {
      new LanguageIndependentTokenizer(Set[String](), stemmer, locale, tokenStore)
    }
    val fsaDict = FSASpotter.buildDictionary(sfStore, tokenizer)

    MemoryStore.dump(fsaDict, new File(outputFolder, "fsa_dict.mem"))

    FileUtils.write(
      new File(outputFolder, "spotter_thresholds.txt"),
      "1.0 0.2 -0.2 0.1" //Defaults!
    )

  }

}
