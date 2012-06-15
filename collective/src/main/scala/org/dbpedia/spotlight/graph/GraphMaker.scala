package org.dbpedia.spotlight.graph

import java.io.File

/**
 * Created with IntelliJ IDEA.
 * User: hector
 * Date: 6/13/12
 * Time: 6:14 PM
 */

/**
 * Just a Object to run the whole process of creating the graphs
 */
object GraphMaker {
  def main(args: Array[String]) {
    val occSrcFile = new File("/home/hector/Researches/nlp/DBpedia_Spotlight/dbpedia-spotlight/index/output/occs.tsv")
    val occsMapFile = new File("output/occsMap.tsv")


    val baseDir = "output/occs/"
    val interListFile = new File(baseDir+"occsIntgerList.tsv")
    val graphFile = new File(baseDir + "occsGraph")


    val mapper = new WikipediaOccurrenceGraph()
    val numberOfNodes = mapper.parseOccsList(occSrcFile, interListFile, occsMapFile)

    val occsGraphUtils = new LabelledGraphUtils()
    val occsGraph = occsGraphUtils.buildGraph(interListFile, numberOfNodes)

    occsGraphUtils.storeGraph(occsGraph, graphFile)
  }
}