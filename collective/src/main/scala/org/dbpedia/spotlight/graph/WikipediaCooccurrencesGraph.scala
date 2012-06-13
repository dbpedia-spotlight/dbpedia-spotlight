package org.dbpedia.spotlight.graph

import io.Source
import org.apache.commons.logging.LogFactory
import scala.collection._
import org.dbpedia.spotlight.model.DBpediaResource
import java.io.{PrintStream, FileOutputStream, OutputStream, File}

/**
 * Created with IntelliJ IDEA.
 * User: hector
 * Date: 6/11/12
 * Time: 5:49 PM
 *
 * Store a graph storing all co-occurrences in Wikipedia
 *
 * @author hector.liu
 */

/**
 * Create a Integer list to help build a co-occurrence graph (use LabelledGraphUtils to build)
 * This class will use the Ocurrence-Integer map created by WikipediaOccurrenceGraph for consistence
*/
class WikipediaCooccurrencesGraph {
  val LOG = LogFactory.getLog(this.getClass)

  /**
   *
   * @param mapFile the map file created by WikipediaOccurrenceGraph
   * @return a host map from uri to graph index
   */
  private def loadHostMap (mapFile:File): Map[String,Int] = {
    LOG.info("Reading the host map.")
    var hostMap =  Map.empty[String,Int]
    Source.fromFile(mapFile).getLines().filterNot(line => line.trim() == "").foreach(
      line =>{
         val fields = line.split("\\t")
         if (fields.length == 2){
           val index = fields(0).toInt
           val uri = fields(1)

           hostMap += (uri -> index)
         }else{
           LOG.error("Invalid string in mapFile, lines should be two fields seperated by tab: \n\t-->\t"+line)
         }
      }
    )
    LOG.info("Done")
    hostMap
  }

  private def hadoopTuplesToMap(bagString:String):Map[String,Int] = {
     var targetMap = Map.empty[String,Int]
     val pattern = """\((.*?),(.*?)\)""".r
     for(m <- pattern.findAllIn(bagString).matchData) {
      // LOG.info(m.group(1)+"\t"+m.group(2))
       targetMap += (m.group(1) -> m.group(2).toInt)
     }
    targetMap
  }

  def parseCooccsList(cooccsFile:File, integerListFile:File , occsMapFile:File) : Int = {
    LOG.info("Parsing Cooccurrences into Integer List");

    val ilfo: OutputStream = new FileOutputStream(integerListFile)
    val ilfoStream = new PrintStream(ilfo, true)

    var indexSet = Set.empty[Int]

    val hostMap = loadHostMap(occsMapFile)

    Source.fromFile(cooccsFile).getLines().filterNot(line => line.trim() == "").foreach(
      line => {
         val fields = line.split("\\t")
         val srcUri = fields(0)

         hostMap.get(srcUri) match {
           case None => LOG.error(String.format("Uri %s was not found in host map, if this happens a lot, something might be wrong",srcUri))
           case Some(srcIndex) => {
             if (!indexSet.contains(srcIndex)){
               indexSet += srcIndex
             }
             val targetMap = hadoopTuplesToMap(fields(1))
             targetMap.foreach{
               case(targetUri,occCount) => {
                 hostMap.get(targetUri) match {
                   case None => LOG.error(String.format("Uri %s was not found in host map, if this happens a lot, something might be wrong",targetUri))
                   case Some(targetIndex) => {
                     if (!indexSet.contains(targetIndex)){
                       indexSet += targetIndex
                     }
                     val intString = srcIndex + "\t" + targetIndex + "\t" + getWeight(srcUri, targetUri,occCount)
                     ilfoStream.println(intString)
                   }
                 }
               }
             }
           }
         }


/*         if (hostMap.contains(srcUri)){
           val srcIndex = hostMap.get(srcUri)

           val targetMap = hadoopTuplesToMap(fields(1))
           targetMap.foreach{
              case(targetUri,occCount) => {
                if (hostMap.contains(targetUri) ){
                  val  targetIndex = hostMap.get(targetIndex)
                  val intString = srcIndex + "\t" + targetIndex + "\t" + getWeight(srcUri, targetUri,occCount)
                  ilfoStream.println(intString)
                } else LOG.error(String.format("Uri %s was not found in host map, if this happens a lot, something might be wrong",targetUri))
              }
           }
         }else LOG.error(String.format("Uri %s was not found in host map, if this happens a lot, something might be wrong",srcUri))*/
      })
    indexSet.size
  }

  private def getWeight(srcUri:String, targetUri: String, cooccCount:Int): Double = {
    return cooccCount
  }
}

object WikipediaCooccurrencesGraph {
  def main(args: Array[String]){
    //inputs
    val cooccsSrcFile = new File("/home/hector/Researches/nlp/DBpedia_Spotlight/dbpedia-spotlight/index/output/co-occs-count.tsv")
    val occsMapFile = new File("output/occsMap.tsv")

    //outputs
    val interListFile = new File("output/co-occs/cooccsIntgerList.tsv")
    val graphFile = new File("output/co-occs/cooccsGraph")

    val wcg = new WikipediaCooccurrencesGraph
    val numberOfNodes = wcg.parseCooccsList(cooccsSrcFile,interListFile,occsMapFile)

    val cooccsGraphUtils = new LabelledGraphUtils()
    val cooccsGraph = cooccsGraphUtils.buildGraph(interListFile, numberOfNodes)
    cooccsGraphUtils.storeGraph(cooccsGraph, graphFile)
  }
}


