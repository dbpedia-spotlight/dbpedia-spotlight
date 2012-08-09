package org.dbpedia.spotlight.topic.wikipedia.flattening

import io.Source
import java.io._
import scala.collection.mutable._
import org.dbpedia.spotlight.io.VowpalPredIterator
import org.dbpedia.spotlight.util.TextVectorizer
import actors._
import actors.Actor._
import org.dbpedia.spotlight.model.{Topic, DBpediaCategory}
import java.io.File
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.topic.wikipedia.util.TopicKeywordLoader

/**
 * This object takes the input corpora (normal and rest) produced by ExtractCategoryCorpus$ and clusters hierarchically
 * until labels can be applied with sufficient confidence or until maximal depth of flattening is reached. <br>
 * Note: this is maybe the most complicated way to flatten the hierarchy and this class is a monster!
 *
 * @deprecated rather use FlattenHierarchySemiSupervised$
 * @see org.dbpedia.spotlight.topic.wikipedia.flattening.ExtractCategoryCorpus$
 * @author Dirk Weissenborn
 */
object FlattenHierarchyByClusters {
  private val LOG = LogFactory.getLog(getClass)

  private var numberOfClusters = 1
  private var categoryInformation = Map[DBpediaCategory, Array[Double]]()

  /**
   *
   * @param args input.vowpal, rest.input.vowpal, categories of input.vowpal, categories of rest.input.vowpal,
   *             path to where flattened hierarchy should be written, temp working dir,
   *             maximal depth of flattening (maximum should be 3, exp growth!), path to installed vowpal wabbit
   */
  def main(args: Array[String]) {
    var arguments = args

    val trainingCorpus = arguments(0)
    val restCorpus = arguments(1)
    val pathToCats = arguments(2)
    val clusterOutput = arguments(4)
    val pathToRestCategories = arguments(3)
    val tmpPath= arguments(5)
    val clusteringDepth = arguments(6).toInt
    val pathToVW = arguments(7)

    flattenCategories(trainingCorpus, restCorpus, pathToCats, pathToRestCategories, clusterOutput, tmpPath, clusteringDepth, pathToVW)
  }

  /**
   * @param trainingCorpus
   * @param restCorpus
   * @param categoriesFile
   * @param restCategoriesFile
   * @param output path of flattened hierarchy
   * @param tmpPath temp working dir
   * @param clusteringDepth maximal depth of hierarchical flattening
   */
  def flattenCategories(trainingCorpus:String, restCorpus:String,
                        categoriesFile: String, restCategoriesFile: String,
                        output: String, tmpPath:String, clusteringDepth:Int,pathToVW:String) {
    val outputFile = new File(output)
    outputFile.mkdirs()
    new File(tmpPath).mkdirs()

    cluster(pathToVW,trainingCorpus,100,tmpPath,"",200000)
    processes.foreach(process => process.waitFor() )
    writePredictions(pathToVW,restCorpus,tmpPath+"/model.",tmpPath+"/rest.predictions.",100)
    processes.foreach(process => process.waitFor() )

    //loading topic vectors
    val (topicVectors,idf) = TopicKeywordLoader.loadTopicVectors("/media/Data/Wikipedia/topics.vectors")

    val writers = Map[Topic,PrintWriter]()
    topicVectors.foreach( topic => {
      if (!topic._1.getName.contains("-related"))
        writers += (topic._1 -> new PrintWriter(new FileWriter(output+"/"+topic._1+".tsv")))
    } )

    var toCluster = evaluateClusters(Array(tmpPath+"/predictions.", tmpPath+"/rest.predictions."), Array(categoriesFile,restCategoriesFile), topicVectors, idf, writers,false,"")

    var depth=1
    while(toCluster.size>0) {
      val corpusWriters = Map[String, PrintWriter]()
      val categoriesWriters = Map[String, PrintWriter]()
      val restCorpusWriters = Map[String, PrintWriter]()
      val restCategoriesWriters = Map[String, PrintWriter]()

      //build new training corpora
      toCluster.foreach( clustering => {
        val newTrainingCorpus = tmpPath +"/corpus."+clustering._1
        val newCategories = tmpPath +"/categories."+clustering._1
        corpusWriters += (clustering._1 -> new PrintWriter(new FileWriter(newTrainingCorpus)))
        categoriesWriters += (clustering._1 -> new PrintWriter(new FileWriter(newCategories)))

        val newRestTrainingCorpus = tmpPath +"/rest.corpus."+clustering._1
        val newRestCategories = tmpPath +"/rest.categories."+clustering._1
        restCorpusWriters += (clustering._1 -> new PrintWriter(new FileWriter(newRestTrainingCorpus)))
        restCategoriesWriters += (clustering._1 -> new PrintWriter(new FileWriter(newRestCategories)))
      })

      LOG.info("Splitting training corpus for individual clusters")
      var categoryIterator= Source.fromFile(categoriesFile).getLines()
      val docCount = Map[String,Int]()
      toCluster.foreach( clustering => docCount += (clustering._1 -> 0))
      Source.fromFile(trainingCorpus).getLines().foreach( line => {
        val category = new DBpediaCategory(categoryIterator.next())
        toCluster.foreach( clustering => {
          if(clustering._2._2.contains(category)) {
            docCount(clustering._1) += 1
            corpusWriters(clustering._1).println(line)
            categoriesWriters(clustering._1).println(category.getCategory)
          }
        } )
      })
      corpusWriters.foreach(corpusWriter => corpusWriter._2.close())
      categoriesWriters.foreach(categoriesWriter => categoriesWriter._2.close())

      categoryIterator= Source.fromFile(restCategoriesFile).getLines()
      Source.fromFile(restCorpus).getLines().foreach( line => {
        val category = new DBpediaCategory(categoryIterator.next())
        toCluster.foreach( clustering => {
          if(clustering._2._2.contains(category)) {
            restCorpusWriters(clustering._1).println(line)
            restCategoriesWriters(clustering._1).println(category.getCategory)
          }
        } )
      })

      restCorpusWriters.foreach(corpusWriter => corpusWriter._2.close())
      restCategoriesWriters.foreach(categoriesWriter => categoriesWriter._2.close())

      LOG.info("flattening "+toCluster.size+" clusters again")
      var ctr = 0
      toCluster.foreach( clustering => {
        if (ctr%2==0) {
          processes.synchronized {
            processes.foreach(process => process.waitFor() )
            processes =  List()
            LOG.info(ctr+" clusters processed")
          }
        }
        cluster(pathToVW,tmpPath +"/corpus."+clustering._1,clustering._2._1, tmpPath, clustering._1, docCount(clustering._1))
        ctr+=1
      })

      processes.synchronized {
        processes.foreach(process => process.waitFor() )
        processes =  List()
      }
      Thread.sleep(1000)
      processes.synchronized {
        processes.foreach(process => process.waitFor() )
        processes =  List()
      }
      //calculate cluster assignments of rest categories
      LOG.info("Calculating predictions of rest-categories (categories, not used for flattening)")
      toCluster.foreach( clustering => {
        if (ctr%2==0) {
          processes.synchronized {
            processes.foreach(process => process.waitFor() )
            processes =  List()
          }
        }
        writePredictions(pathToVW,tmpPath +"/rest.corpus."+clustering._1, tmpPath+"/model."+clustering._1, tmpPath+"/rest.predictions."+clustering._1, clustering._2._1)
        ctr+=1
      })

      processes.synchronized {
        processes.foreach(process => process.waitFor() )
        processes =  List()
      }

      var tmpToCluster =  Map[String, (Int, Set[DBpediaCategory])]()

      toCluster.foreach( clustering => {
        LOG.info("=========Cluster analysis of clusters from old cluster "+clustering._1+"=========")
        tmpToCluster ++= evaluateClusters(Array(tmpPath+"/predictions."+clustering._1, tmpPath+"/rest.predictions."+clustering._1),
                                          Array(tmpPath+"/categories."+clustering._1,tmpPath +"/rest.categories."+clustering._1),
                                          topicVectors, idf, writers, depth==clusteringDepth,clustering._1)
      })
      depth+=1
      toCluster = tmpToCluster
  }

  writers.foreach( writer => writer._2.close())
}

  def evaluateClusters(vowpalPredictions: Array[String], categoriesFiles: Array[String],
                       topicVectors:Map[Topic, Map[String, Double]],
                       idf: Map[String, Double],
                       writers: Map[Topic, PrintWriter],
                       writeAll:Boolean, id:String) :  Map[String, (Int, Set[DBpediaCategory])] = {
    var toCluster: Map[String, (Int,Set[DBpediaCategory])] = Map()

    categoryInformation = Map[DBpediaCategory, Array[Double]]()
    collectClusterInformation(vowpalPredictions, categoriesFiles)

    val vectorizer = new TextVectorizer()

    var topicList: Map[Topic, Double] = null
    var bestTopics: List[(Topic, Double)] = null
    var relatedness = 0.0
    val threshold = 0.4
    var assignedCategoriesCtr = 0

    def assignCategoryToTopic(category: DBpediaCategory, threshold:Double){
      if (!alreadyProcessed.contains(category.getCategory)) {
        val catVector = vectorizer.getWordCountVector(category.getCategory.toLowerCase.replaceAll("[^a-z]", " "))
        val selectedTopics = Map[Topic, Double]()
        topicVectors.foreach(topic => {
          var sum = 0.0
          val topicName = vectorizer.getWordCountVector(topic._1.getName.replace("_"," "))
          topicName.remove("scienc")
          topicName.remove("relat")
          catVector.foreach {
            case (word, count) => sum += topicName.getOrElse(word, topic._2.getOrElse(word, 0.0) ) * count
          }

          if (sum > threshold) {
            val selectedTopic = new Topic(topic._1.getName.replace("-related", ""))
            if (sum > selectedTopics.getOrElse(selectedTopic,0.0))
              selectedTopics += (selectedTopic -> sum)
          }
        })
        if (selectedTopics.size > 0) {
          var topics = selectedTopics.toList.sortBy(x => -x._2)
          topics = topics.takeWhile { case (topic, value) => value > 0.8*topics.head._2}
          topics.foreach(topic => {
            writers(topic._1).println(category.getCategory + "\t" + 1/topic._2)
            //LOG.info("Defining category " + category.getCategory + " as member of " + topic._1.getName)
          })
          assignedCategoriesCtr += 1
          alreadyProcessed += (category.getCategory)
          if(assignedCategoriesCtr%1000==0) {
            LOG.info("Assigned "+assignedCategoriesCtr+" categories to topics")
            LOG.info("Latest assignment: " + category.getCategory + " -> " + topics.head._1.getName)
          }
        }
      }
    }
    categoryInformation.keys.foreach( category => {
      assignCategoryToTopic(category,0.4)
      })

    for (i <- 0 until numberOfClusters) {
      topicList = Map()
      var clusterString = ""
      LOG.info("============Cluster " + i + "============")

      val candidates = categoryInformation.toList.filter(x => (x._2(i)) > threshold)
      candidates.foreach(clusterString += " " + _._1.getCategory.toLowerCase.replaceAll("[^a-z]", " "))
      val vector = vectorizer.getWordCountVector(clusterString)
      val sum = math.sqrt(vector.foldLeft(0.0)((acc, element) => acc + element._2 * element._2))
      topicVectors.foreach {
        case (topic, wordVec) => {
          relatedness = 0.0
          wordVec.foreach {
            case (word, value) => relatedness += value * math.log(topicVectors.size.toDouble / idf.getOrElse(word, topicVectors.size.toDouble)) * vector.getOrElse(word, 0.0) / sum
          }
          val mainTopic = new Topic(topic.getName.replace("-related", ""))
          if (topicList.contains(mainTopic)) {
            if (topicList(mainTopic) < relatedness)
              topicList.update(mainTopic, relatedness)
          }
          else
            topicList += (mainTopic -> relatedness)
        }
      }
      bestTopics = topicList.toList.sortBy(x => -x._2).take(10)
      val topicsSum = bestTopics.foldLeft(0.0)(_ + _._2)
      bestTopics = bestTopics.map[(Topic, Double),List[(Topic, Double)]]( element => (element._1, element._2 / topicsSum))

      LOG.info(bestTopics.foldLeft("")((acc, element) => acc + ", " + element._1 + "(" + math.round(element._2 * 100) / 100.0 + ")").substring(2))
      var tempSum = 0.0
      if (writeAll){
        val threshold = 0.25
        bestTopics.takeWhile( topic => topic._2 > threshold).foreach( topic => {
          LOG.info("Defining cluster as " + topic._1)
          candidates.foreach(candidate => {
            if (!alreadyProcessed.contains(candidate._1.getCategory))
              writers(topic._1).println(candidate._1.getCategory + "\t" + ((1 - topic._2) * (1 - candidate._2(i)) * 100 ))
          })
        })
        //assign categories to topics that do not belong to any topic yet, if possible
        if(bestTopics(0)._2 <= threshold) {
          candidates.foreach(candidate => {
            assignCategoryToTopic(candidate._1,0.4)
          } )
        }
      }
      else {
        bestTopics = bestTopics.takeWhile(element => {
          tempSum += element._2
          tempSum-element._2 < 0.5 && element._2 > 0.1 &&  element._2 > (bestTopics(0)._2/3.0)
        })
        if (bestTopics.size > 1) {
          LOG.info("Has to be clustered again into " + bestTopics.size + " clusters!")
          val candidatesSet = Set[DBpediaCategory]()
          candidates.foreach( candidate =>
            //if (!alreadyProcessed.contains(candidate._1.getCategory))
            candidatesSet += (candidate._1)
          )
          toCluster += ({if (id!="") id+"." else ""}+i -> ( (bestTopics.size, candidatesSet) ))
        } else {
          if (bestTopics.size>0) {
            LOG.info("Defining cluster as " + bestTopics(0)._1)
            candidates.foreach( candidate =>
              if (!alreadyProcessed.contains(candidate._1.getCategory))
                writers(bestTopics(0)._1).println(candidate._1.getCategory + "\t" + ((1 - bestTopics(0)._2) * (1 - candidate._2(i))* 100)) )
          } else
            candidates.foreach(candidate => {
              assignCategoryToTopic(candidate._1,0.4)
            } )
        }
      }

    }
    //assign categories to topics that do not belong to any cluster, if possible
    val candidates = categoryInformation.toList.filter { case (category, predictions) => {
      predictions.takeWhile(pred => pred <= threshold).size.equals(predictions.size)
    }}

    if (!writeAll&&candidates.size>200) {
      val candidatesSet = Set[DBpediaCategory]()
      candidates.foreach( candidate => candidatesSet += (candidate._1))
      toCluster += ({if (id!="") id+"." else ""}+numberOfClusters -> ( (40, candidatesSet) ))
    }  else
      candidates.foreach(candidate => {
        assignCategoryToTopic(candidate._1,0.4)
      } )

    toCluster
  }

  def cluster(pathToVW:String, trainingCorpus:String, nrOfClusters:Int, outputDir:String, id:String, nrOfDocuments:Int) {
    val modelPath = outputDir +"/model."+id
    val predictionsPath = outputDir +"/predictions."+id

    if(!new File(predictionsPath).exists()) {
      //if nr of documents is too small, flattening is not worth it
      if (nrOfDocuments<50) {
        LOG.info("Number of documents to low for flattening- making just one cluster containing all elements")
        val writer = new PrintWriter(new FileWriter(predictionsPath))
        Source.fromFile(trainingCorpus).getLines().foreach( _ => writer.println("1"))
        writer.close()
      }
      else {
        val passes = 20
        //run vowpal flattening
        var command = pathToVW
        command += " "+trainingCorpus
        command += " --lda "+nrOfClusters
        command += " -b 17 --minibatch "+(nrOfDocuments/50+10)
        command += " -f "+modelPath
        command += " --lda_D "+ (nrOfDocuments*passes)
        command += " --passes "+passes+" --cache_file /tmp/vw.cache."+id
        var secCommand = "/home/dirk/workspace/vowpal_wabbit/vowpalwabbit/vw"
        secCommand += " "+trainingCorpus
        secCommand += " --lda "+nrOfClusters
        secCommand += " -b 17 -i "+modelPath+" --testonly -p "+predictionsPath

        run(command,secCommand)
      }
    }
  }

  def writePredictions(pathToVW:String, corpus:String, modelPath:String,predictionsPath:String,nrOfClusters:Int) {
    if(!new File(predictionsPath).exists()) {
      if (new File(modelPath).exists()){
        //run vowpal
        var command = pathToVW
        command += " "+corpus
        command += " --lda "+nrOfClusters
        command += " -b 17 -i "+modelPath+" --testonly -p "+predictionsPath

        run(command,null)
      }
      else {
        val writer = new PrintWriter(new FileWriter(predictionsPath))
        Source.fromFile(corpus).getLines().foreach( _ => writer.println("1"))
        writer.close()
      }

    }
  }
  private val alreadyProcessed = Set[String]()

  def collectClusterInformation(vowpalPredictions: Array[String], categoriesFiles: Array[String]) {
    var predictions: Array[Double] = Array()

    for (i <- 0 until vowpalPredictions.size) {
      val vowpalPredictionFile= vowpalPredictions(i)
      val categoriesFile = categoriesFiles(i)
      var category = ""

      if (new File(vowpalPredictionFile).exists()) {
        val iterator = new VowpalPredIterator(vowpalPredictionFile)
        val catIterator = Source.fromFile(categoriesFile).getLines()

        while (iterator.hasNext) {
          category = catIterator.next()
          predictions = iterator.next()
          if (category!=null && !alreadyProcessed.contains(category))
            categoryInformation += (new DBpediaCategory(category) -> predictions)
        }
      }
    }

    numberOfClusters = predictions.size
  }

  private var processes: List[Process] =  List()

  private def run(firstCommand:String,secondCommand:String) : Process = {
    val args = firstCommand.split(" ")
    val processBuilder = new ProcessBuilder(args: _* )

    LOG.info("running command : \""+firstCommand+"\"")

    val newreader = (Actor.actor {
      val WAIT_TIME = 2000

      reactWithin(WAIT_TIME) {
        case TIMEOUT =>
        case proc:Process =>
          val streamReader = new java.io.InputStreamReader(proc.getErrorStream)
          val bufferedReader = new java.io.BufferedReader(streamReader)
          var line:String = null
          while({line = bufferedReader.readLine; line != null}){
            //LOG.info(Thread.currentThread+": "+line)
          }
          bufferedReader.close
          if(secondCommand!=null)
            processes.synchronized {
              processes = run(secondCommand,null) :: processes
            }
      }
    } )
    val proc:Process = processBuilder.start()
    //Send the proc to the actor, to extract the console output.
    newreader ! proc
    processes.synchronized {
      processes = (proc) :: processes
    }

    proc
  }
}
