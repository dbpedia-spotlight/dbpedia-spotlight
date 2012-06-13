package org.dbpedia.spotlight.graph

import org.apache.commons.logging.LogFactory
import scala.io.Source
import it.unimi.dsi.webgraph.labelling.{Label, ArcLabelledImmutableGraph, GammaCodedIntLabel}
import collection.mutable.Map
import scala.Predef._
import java.io.{File, PrintStream, FileOutputStream, OutputStream}
import java.util.NoSuchElementException
import java.lang.IllegalArgumentException
import it.unimi.dsi.webgraph.{BVGraph, ArrayListMutableGraph}
import org.dbpedia.spotlight.model.DBpediaResource


/**
 * Created with IntelliJ IDEA.
 * User: hector
 * Date: 6/11/12
 * Time: 5:00 PM
 *
 * Create a graph for all entities in Wikipedia from Occurrence file
 *
 * @author hector.liu
 */

class EntityOccsGraph() {

  val LOG = LogFactory.getLog(this.getClass)


  def buildGraph(integerListFile: File, numNodes: Int): ArrayListMutableGraph = {
    val graph = new ArrayListMutableGraph()
    graph.addNodes(numNodes)
    LOG.info(String.format("Creating a graph with %s number of nodes",numNodes.toString) )

    Source.fromFile(integerListFile).getLines().filterNot(line => line.trim() == "").foreach(
      line => {

        val fields = line.split("\t")

        if (fields.length == 3) {
          val src = fields(0).toInt
          val target = fields(1).toInt
          try {
            graph.addArc(src, target)
          } catch {
            case illArg: IllegalArgumentException => {
              //LOG.warn(String.format("Duplicate inlinks from %s to %s ignored", fields(0), fields(1)))
            }
          }
          //LOG.info(String.format("Added arc from %s to %s",fields(0),fields(1)))

        } else {
          LOG.error(String.format("Wrong line formatting at: \n -> \t%s Input file should be triple seperated by tabs", line))
        }
      }
    )

    LOG.info("Done.")
    graph
  }

  //read occs.tsv or occs.uriSorted.tsv and map to integers
  def parseToMap(srcFile: File, integerListFile: File, mapFile: File): Int = {
    LOG.info("Parsing the occs file to Map")
    //var uriMap = Map.empty[String, (Int, Array[String])]
    var uriMap = Map.empty[String,Int]

    val ilfo: OutputStream = new FileOutputStream(integerListFile)
    val ilfoStream = new PrintStream(ilfo, true)

    val mfo = new FileOutputStream(mapFile)
    val mfoStream = new PrintStream(mfo, true)

    //Go through the file and attach give a index to each URI encountered
    var indexCount = 0
    var srcIndex = 0
    var targetIndex = 0
    Source.fromFile(srcFile).getLines().foreach(
      line => {
        var lineNum = 0
        val fields = line.split("\\t")
        if (fields.length == 5) {
          val id = fields(0)
          val targetUri = fields(1)
          val srcUri = id.split("-")(0)

          if (uriMap.contains(srcUri)) {
            uriMap(srcUri) = 1
           // LOG.info(String.format("Creat link from %s to %s",srcUri,targetUri))
          } else {
            uriMap += (srcUri -> 1)
            srcIndex = indexCount
            indexCount += 1
           // LOG.info(String.format("Adding Souce uri: %s , given number %s, pointing to Target Uri: %s",srcUri,srcIndex.toString,targetUri))
            val mapString = srcIndex + "\t" + srcUri
            mfoStream.println(mapString)
          }

          if (!uriMap.contains(targetUri)) {
            uriMap += (targetUri -> 1)
            targetIndex = indexCount
            indexCount += 1
           // LOG.info(String.format("Adding Target uri: %s , given number %s, pointing to empty list",targetUri,targetIndex.toString))
            val mapString = targetIndex + "\t" + targetUri
            mfoStream.println(mapString)
          }

          val intString = srcIndex + "\t" + targetIndex + "\t" + getWeight(new DBpediaResource(srcUri), new DBpediaResource(targetUri))


          ilfoStream.println(intString)


        } else {
          LOG.error("Invailid line in file: " + line)
        }
        lineNum += 1
        if (lineNum % 1000000 == 0) LOG.info(String.format("Store %s valid URIs and %s Links", indexCount.toString, lineNum.toString))
      })



/*    // Output each link by one line
    uriMap.foreach {
      case (srcUri, tuple) => {
        val srcIndex = tuple._1
        val targetUriArr = tuple._2
        targetUriArr.foreach(targetUri => {
          try {
            val targetIndex = uriMap(targetUri)._1
            val intString = srcIndex + "\t" + targetIndex + "\t" + getWeight(new DBpediaResource(srcUri), new DBpediaResource(targetUri))
            ilfoStream.println(intString)
          } catch {
            case ioe: NoSuchElementException => LOG.error(String.format("Uri: %s not found", targetUri))
          }
        })


      }
    }*/

    ilfoStream.close()
    mfoStream.close()

    LOG.info(String.format("Succesfully process map with size %s", uriMap.size.toString))
    LOG.info(String.format("Map file saved at %s; Integer List File saved at %s", mapFile.getName, integerListFile.getName))

    uriMap.size
  }

  def getWeight(src: DBpediaResource, target: DBpediaResource): Double = {
    1.0
  }

  def storeGraph(graph: ArrayListMutableGraph, outputFile: File) = {
    LOG.info("Storing Graph to " + outputFile.getAbsolutePath)
    val g = graph.immutableView()
    BVGraph.store(g, outputFile.getAbsolutePath + ArcLabelledImmutableGraph.UNDERLYINGGRAPH_SUFFIX)
    LOG.info("Done.")
    g
  }
}

object EntityOccsGraph {
  def main(args: Array[String]) {
    val occSrcFile = new File("/home/hector/Researches/nlp/DBpedia_Spotlight/dbpedia-spotlight/index/output/occs.tsv")
    val interListFile = new File("output/occsIntgerList.tsv")
    val occsMapFile = new File("output/occsMap.tsv")
    val graphFile = new File("output/occsGraph")

    val entityGraph = new EntityOccsGraph()

    val numbeOfNodes = entityGraph.parseToMap(occSrcFile, interListFile, occsMapFile)

    val graph = entityGraph.buildGraph(interListFile, numbeOfNodes)

    entityGraph.storeGraph(graph, graphFile)
  }
}