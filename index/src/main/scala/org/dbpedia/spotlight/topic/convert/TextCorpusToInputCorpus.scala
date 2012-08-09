package org.dbpedia.spotlight.topic.convert

import java.io._
import scala.collection.mutable.Map
import scala.Predef._
import org.apache.commons.cli._
import scala._
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.topic.util._
import org.dbpedia.spotlight.model.Topic
import org.dbpedia.spotlight.db.model.{WordIdDictionary, TopicalStatInformation}
import org.dbpedia.spotlight.util.TextVectorizer

/**
 * Iterates over a tsv-corpus file, where each line's first column is the category of the second column which is the
 * content, and produces an input corpus file for vowpal wabbit (each line of form: | wordId(numerical):wordcount ...) or
 * weka (.arrf) depending on specified corpus type.
 * It is possible to apply predefined data transformations which are described in this paper
 * (http://people.csail.mit.edu/jrennie/papers/icml03-nb.pdf), from which tf-, idf- and length transformations are derived.
 * <br/><br/>
 *
 * console options:  <br/>
 *   i  Input file or directory of tsv-corpus  <br/>
 *   o  Output directory of vowpal vectors and category.list  <br/>
 *   d  Dictionary file of word ids to integers (will be created for training set, or used and extended if existent  <br/>
 *   c  Path to information file about word distributions over categories  <br/>
 *   T  This is a test set <br/>
 *   t  Apply data transformations as described  <br/>
 *   ct type of corpus ('arff' or default:'vowpal')  <br/>
 *   a  type of analyzer, types: 'phonetic', 'english' (default: english) <br/>
 * @author dirk
 */
object TextCorpusToInputCorpus {
  val LOG = LogFactory.getLog(getClass)

  private var input: String = ""
  private var output: String = ""

  private var dictionary: WordIdDictionary = null
  //Topic in this case also consists of a word vector (tf and document occurence count for each word for each topic)
  private var topicInfo: TopicalStatInformation = null
  private var vectorizer: TextVectorizer = null

  val FILENAME_TOPICS = "topics.list"
  private val OVERALL_TOPIC = TopicUtil.OVERALL_TOPIC

  def main(args: Array[String]) {
    val options: Options = new Options();

    options.addOption("i", "input", true, "Input file or directory of tsv-corpus")
    options.addOption("o", "output", true, "Output directory of vowpal vectors and directory")
    options.addOption("d", "dictionary", true, "Dictionary file of word ids to integers")
    options.addOption("c", "categoryinfo", true, "path to information file about word distributions over categories")
    options.addOption("T", "test", false, "this is a test dataset")
    options.addOption("t", "transform", false, "apply data transformation")
    options.addOption("S", "maximum-dictionary-size", true, "maximum size of dictionary")
    options.addOption("s", "dictionary-size", true, "size of dictionary for now")
    options.addOption("ct","corpus-type",true, "supported types: 'vowpal', 'arff'")
    options.addOption("a","analyzer",true, "supported types: 'phonetic', 'english'")

    val parser: PosixParser = new PosixParser()

    val cmd: CommandLine = parser.parse(options, args)

    val pathToDictionary = cmd.getOptionValue("d")
    val pathToTopics = cmd.getOptionValue("c")
    var corpusType = "vowpal"
    if (cmd.hasOption("ct"))
      corpusType = cmd.getOptionValue("ct")

    var phonetic = false
    if (cmd.hasOption("a"))
      phonetic = cmd.getOptionValue("a").contains("phonetic")

    if (corpusType!="vowpal"&&corpusType!="arff") {
      LOG.error("corpus type not supported")
      System.exit(1)
    }

    val maxSize = cmd.getOptionValue("S").toInt
    val size = cmd.getOptionValue("s").toInt

    writeDocumentsToCorpus(cmd.getOptionValue("i"),cmd.getOptionValue("o"),cmd.hasOption("T"), cmd.hasOption("t"), corpusType, pathToTopics, pathToDictionary, size, maxSize, phonetic)
  }

  def writeDocumentsToCorpus(inputPath:String, outputPath:String, test: Boolean, transform: Boolean, corpusType: String, pathToTopics:String, pathToDictionary:String, dicSize:Int, maxSize:Int, usePhonetic:Boolean = false) {
    input = inputPath
    output = outputPath

    val dic: File = new File(pathToDictionary)
    val topicsFile: File = new File(pathToTopics)

    dic.getParentFile.mkdirs

    dictionary = new WordIdDictionary(pathToDictionary, maxSize, usePhonetic)
    topicInfo = TopicUtil.getTopicInfo(pathToTopics)

    vectorizer = new TextVectorizer(usePhonetic)

    if (!topicsFile.exists) {
      topicsFile.getParentFile.mkdirs
      topicInfo.newTopic(OVERALL_TOPIC)
    }

    val inputFile: File = new File(input)
    var inputs: Array[File] = Array(inputFile)
    if (inputFile.isDirectory) {
      inputs = inputFile.listFiles(new FilenameFilter {
        def accept(file: File, s: String): Boolean = {
          return s.endsWith(".txt")
        }
      })
    }

    writeFileSeq(inputs, test)

    LOG.info("post-processing, be patient")

    cutLowFrequencies(dicSize)

    if(!test)  {
      dictionary.persist
      topicInfo.persist
    }

    corpusType match {
      case "vowpal" => writeFinalVowpalCorpus(output+".writing", output,transform)
      case "arff" => writeFinalArffCorpus(output+".writing", output, transform)
    }

  }

  private def writeFinalArffCorpus(in:String, out:String, transform:Boolean) {
    val lines = scala.io.Source.fromFile(in).getLines()
    val topicLines = scala.io.Source.fromFile(output.substring(0, output.lastIndexOf("/") + 1) + FILENAME_TOPICS).getLines()

    var split : Array[String] = null
    var word : Array[String] = null
    var wordId = -1
    var values : List[(Int,Double)] = List()
    val docSum: Double = topicInfo.getNumDocs(OVERALL_TOPIC)

    val pw : PrintWriter = new PrintWriter(new FileWriter(out))
    var ctr = 0

    pw.println("@RELATION topics")
    for (i <- 0 until dictionary.getMaxSize)
      pw.println("@ATTRIBUTE word_"+i+" NUMERIC")

    val topics : Array[String] = topicLines.next().split(",")
    if (topics.length>10000)
      pw.println("@ATTRIBUTE class string")
    else
      pw.println("@ATTRIBUTE class {"+topics.sorted.reduceLeft((acc, cat) => acc + "," + cat)+"}")
    pw.println("@DATA")

    lines.foreach((fileLine) => {
      values = List()
      pw.print("{")

      split = fileLine.split(" ")

      var squaredSum = 0.0
      for(i <- 1 until split.length) {
        word = split(i).split(":")

        wordId = dictionary.getDictionary.getOrElse(word(0),-1)
        if (wordId > -1)  {
          val newElement = (wordId, {   if (transform) //TF-IDF
                                          math.sqrt(word(1).toDouble)*math.log( docSum / topicInfo.getDF(OVERALL_TOPIC, word(0)) )
                                        else
                                          word(1).toDouble
                                      } )
          squaredSum += newElement._2*newElement._2
          values = newElement :: values
        }
      }
      //length normalization
      squaredSum = math.sqrt(squaredSum)

      values = values.sortBy(_._1)

      values.foreach( element => pw.print(element._1+" "+ (math.round(element._2 / squaredSum * 10000.0)/10000.0) +",") )
      pw.println(dictionary.getMaxSize+" "+topicLines.next()+"}")

      ctr +=1
      if( ctr%10000 == 0)
        LOG.info(ctr+ " final examples written")
    })
    pw.flush()
    pw.close()

    new File(output.substring(0, output.lastIndexOf("/") + 1) + FILENAME_TOPICS).delete()
    new File(in).delete()
  }

  private def writeFinalVowpalCorpus(in:String, out:String, transform:Boolean) {
    val lines = scala.io.Source.fromFile(in).getLines()

    var split : Array[String] = null
    var word : Array[String] = null
    var wordId = -1
    var wordCount = 0.0
    val docSum: Double = topicInfo.getNumDocs(OVERALL_TOPIC)

    val pw : PrintWriter = new PrintWriter(new FileWriter(out))
    var line = ""
    var ctr = 0

    lines.foreach((fileLine) => {
      pw.print("|")
      split = fileLine.split(" ")

      line = ""
      for(i <- 1 until split.length) {
        word = split(i).split(":")

        wordId = dictionary.getDictionary.getOrElse(word(0),-1)
        if (wordId > -1)  {
          wordCount = word(1).toDouble
          line += " "+wordId+":"+{
            if (transform)    //TF-IDF
              math.round(math.sqrt(wordCount)*math.log( docSum / topicInfo.getDF(OVERALL_TOPIC, word(0)) ))
            else
              math.round(wordCount)
          }
        }
      }
      if (line!="")
        pw.println(line)
      ctr +=1
      if( ctr%10000 == 0)
        LOG.info(ctr+ " final examples written")
    })
    pw.flush()
    pw.close()

    new File(in).delete()
  }

  private def cutLowFrequencies(bound:Int) {
    val docSum = topicInfo.getNumDocs(OVERALL_TOPIC)
    val clone = topicInfo.getWordFrequencies(OVERALL_TOPIC).clone()

    if (bound > dictionary.getSize) {
      LOG.info("calculate dictionary entries")
      dictionary.getDictionary.foreach( entry => clone.remove(entry._1))

      val sortedWordvectors = clone.toList.sortBy(
        (entry) => - math.sqrt(entry._2._1)*
                     math.log(docSum / entry._2._2) )
      val dicSize = dictionary.getSize
      for (i <- 0 until math.min(bound, clone.size)-dictionary.getSize)
        dictionary.put(sortedWordvectors(i)._1, i+dicSize)

      topicInfo.getTopics.foreach( topic => {
        val clone = topicInfo.getWordFrequencies(topic).clone()
        topicInfo.getWordFrequencies(topic).foreach {
          case (word,_) => if (dictionary.getId(word).equals(-1)) clone.remove(word)
        }
        topicInfo.setWordFrequencies(topic, clone)
      })
    }
  }

  private def writeFileSeq(inputs: Array[File], test : Boolean) {
    val tempCategoriesFile: String = output.substring(0, output.lastIndexOf("/") + 1) + FILENAME_TOPICS

    var ctr = 0

    try {
      val pw: PrintWriter = new PrintWriter(new FileWriter(output+".writing"))
      val pw2: PrintWriter = new PrintWriter(new FileWriter(tempCategoriesFile+".writing"))

      for (file <- inputs) {
        val lines = scala.io.Source.fromFile(file.getAbsolutePath).getLines()
        lines.foreach((fileLine) => {
          ctr += 1
          writeLine(fileLine, pw, pw2, test)
          if (ctr%10000==0)
            LOG.info(ctr + " examples processed")
        })
      }
      pw.flush
      pw.close

      pw2.flush
      pw2.close

      rewriteTopics(tempCategoriesFile, tempCategoriesFile+".writing")
    }
    catch {
      case e: IOException => {
        e.printStackTrace()
      }
    }
  }

  private def writeLine(fileLine: String, datasetWriter: PrintWriter, topicWriter: PrintWriter, test: Boolean) {
    var text = fileLine
    var line: String = "|"

    val split = text.split("\t", 2)
    text = split(1)
    val topic = new Topic(split(0))

    topicWriter.println(topic.getName)

    if (!test) {
      if (!topicInfo.getTopics.contains(topic))
        topicInfo.newTopic(topic, 1)
      else
        topicInfo.incNumDocs(topic,1)

      topicInfo.incNumDocs(OVERALL_TOPIC,1)
    }


    val vector: Map[String, Double] = vectorizer.getWordCountVector(text)

    for ((word,count) <- vector) {
      if (!test) {
        val sum = math.sqrt(vector.foldLeft(0.0)((ctr, element)=>ctr + element._2*element._2))

        var stats = topicInfo.getWordFrequencies(topic).getOrElse(word, (0.0,0.0))
        topicInfo.putWordFrequency(topic, word, stats._1 + count / sum, stats._2 + 1 )

        stats = topicInfo.getWordFrequencies(OVERALL_TOPIC).getOrElse(word, (0.0,0.0))
        topicInfo.putWordFrequency(OVERALL_TOPIC, word, stats._1 + count / sum, stats._2 + 1 )
      }

      line += " " + word + ":" + count.toInt
    }
    datasetWriter.println(line)
  }

  private def rewriteTopics(topicsFile: String, tmpTopicsFile: String) {
    val pw3: PrintWriter = new PrintWriter(new FileWriter(topicsFile))
    pw3.println(topicInfo.getTopics.foldLeft("")((acc, topic) => {
      if (topic.equals(OVERALL_TOPIC))
        acc
      else
        acc + "," + topic.getName
    } ).substring(1) )
    val topicLines = scala.io.Source.fromFile(tmpTopicsFile).getLines()
    topicLines.foreach((topic) => pw3.println(topic))
    pw3.flush()
    pw3.close()

    new File(tmpTopicsFile).delete()
  }

  /**
   * @deprecated
   */
  private def applyEntropyScalingTransformation(output: String, input: String) {
    LOG.info("Data transformation")
    val lines = scala.io.Source.fromFile(input).getLines()

    var split : Array[String] = null
    var word : Array[String] = null
    var wordId = ""
    var wordCount : Double = 0
    var wordCountNew: Long = 0
    var entropy : Double = 0
    var entropies = Map[String, Double]()
    var ctr = 0

    val pw : PrintWriter = new PrintWriter(new FileWriter(output))
    var line = ""; var sum = 0
    lines.foreach((fileLine) => {
      pw.print("|")
      split = fileLine.split(" ")

      line = ""
      for(i <- 1 until split.length) {
        word = split(i).split(":")
        wordId = word(0)
        wordCount = word(1).toDouble

        entropy = entropies.getOrElseUpdate(wordId, getScaledCategoryEntropy(wordId))

        wordCount *=  (2-entropy)
        wordCountNew = math.round(wordCount)
        if (wordCountNew>0)  {
          line += " "+wordId+":"+wordCountNew
        }
      }
      pw.println(line)

      ctr += 1
      if( ctr%10000 == 0)
        LOG.info(ctr+ " examples transformed")
    })
    pw.flush()
    pw.close()

    new File(input).delete()
  }

  /**
   * @deprecated
   */
  private def applyEntropyCuttingTransformation(output: String, input: String) {
    val lines = scala.io.Source.fromFile(input).getLines()

    var split : Array[String] = null
    var word : Array[String] = null
    var wordId = ""
    var wordCount : Double = 0
    val pw : PrintWriter = new PrintWriter(new FileWriter(output))
    lines.foreach((fileLine) => {
      pw.print("|")
      split = fileLine.split(" ")

      for(i <- 1 until split.length) {
        word = split(i).split(":")
        wordId = word(0)
        wordCount = word(1).toDouble
        val entropy = getScaledCategoryEntropy(wordId)
        if (entropy<0.5)  {
          val newWC = math.round(wordCount)
          pw.print(" "+wordId+":"+newWC)
        }
      }
      pw.println()
    })
    pw.flush()
    pw.close()

    new File(input).delete()
  }

  private def getScaledCategoryEntropy(wordId: String) : Double = {
    var entropy = 0.0
    val topics = topicInfo.getTopics.filterNot(_.equals(OVERALL_TOPIC))
    val vector = new Array[Double](topics.size)
    var k = 0
    topics.foreach( topic => {
      val tf = topicInfo.getTF(topic,wordId) / topicInfo.getNumDocs(topic)
      vector(k) -= tf*math.log(tf)/math.log(2)
      k+=1
    })
    val sum = vector.sum
    vector.foreach( el => entropy -= el/sum * math.log(el/sum)/math.log(2))
    return entropy
  }

}
