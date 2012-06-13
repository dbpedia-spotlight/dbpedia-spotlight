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
 * Create a graph storing from a IntegerListFile
 *
 * @author hector.liu
 */

class LabelledGraphUtils() {

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

  def storeGraph(graph: ArrayListMutableGraph, outputFile: File) = {
    LOG.info("Storing Graph to " + outputFile.getAbsolutePath)
    val g = graph.immutableView()
    BVGraph.store(g, outputFile.getAbsolutePath + ArcLabelledImmutableGraph.UNDERLYINGGRAPH_SUFFIX)
    LOG.info("Done.")
    g
  }
}

