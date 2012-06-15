package org.dbpedia.spotlight.graph

import java.io.{PrintStream, FileOutputStream, OutputStream, File}
import io.Source
import org.dbpedia.spotlight.model.DBpediaResource
import org.apache.commons.logging.LogFactory

/**
 * Created with IntelliJ IDEA.
 * User: hector
 * Date: 6/13/12
 * Time: 4:37 PM
 */

/**
 * Create a Integer list to help build a occurrence graph (use LabelledGraphUtils to build)
 * Alongside, this class also create a Occurrences-Integer map (a one-to-one mapping) to retrieve the origin ocurrences
 */
class WikipediaOccurrenceGraph {
  val LOG = LogFactory.getLog(this.getClass)

  /**
   * Read in data and map URIs to integers, provide a Integer list to build a ArrayListMutableGraph using LabelledGraphUtils
   * @param occsFile  the input file: occs.tsv or occs.uriSorted.tsv
   * @param integerListFile the output file to store the integer list representing the graph encoded in occs.tsv
   * @param mapFile the output file to store the uri to interger map
   * @return Integer indicating the number of nodes found
   */
  def parseOccsList(occsFile: File, integerListFile: File, mapFile: File): Int = {
    LOG.info("Parsing the occs file to Map")
    //var uriMap = Map.empty[String, (Int, Array[String])]
    //var uriMap = Map.empty[String,Int]
    var uriSet = Set.empty[String]       // use a set to control duplicat URIs and to count the URI number

    val ilfo: OutputStream = new FileOutputStream(integerListFile)
    val ilfoStream = new PrintStream(ilfo, true)

    val mfo = new FileOutputStream(mapFile)
    val mfoStream = new PrintStream(mfo, true)

    //Go through the file and attach give a index to each URI encountered
    var indexCount = 0
    var srcIndex = 0
    var targetIndex = 0
    Source.fromFile(occsFile).getLines().filterNot(line => line.trim() == "").foreach(
      line => {
        var lineNum = 0
        val fields = line.split("\\t")
        if (fields.length == 5) {
          val id = fields(0)
          val targetUri = fields(1)
          val srcUri = id.split("-")(0)

          if (uriSet.contains(srcUri)) {

          } else {
            uriSet += srcUri
            srcIndex = indexCount
            indexCount += 1
            // LOG.info(String.format("Adding Souce uri: %s , given number %s, pointing to Target Uri: %s",srcUri,srcIndex.toString,targetUri))
            val mapString = srcIndex + "\t" + srcUri
            mfoStream.println(mapString)
          }

          if (!uriSet.contains(targetUri)) {
            uriSet += targetUri
            targetIndex = indexCount
            indexCount += 1
            // LOG.info(String.format("Adding Target uri: %s , given number %s, pointing to empty list",targetUri,targetIndex.toString))
            val mapString = targetIndex + "\t" + targetUri
            mfoStream.println(mapString)
          }

          // LOG.info(String.format("Creat link from %s to %s",srcUri,targetUri))
          val intString = srcIndex + "\t" + targetIndex + "\t" + getWeight(new DBpediaResource(srcUri), new DBpediaResource(targetUri))

          ilfoStream.println(intString)


        } else {
          LOG.error("Invailid line in file at \n -> \t" + line)
        }
        lineNum += 1
        if (lineNum % 1000000 == 0) LOG.info(String.format("Store %s valid URIs and %s Links", indexCount.toString, lineNum.toString))
      })

    ilfoStream.close()
    mfoStream.close()

    LOG.info(String.format("Succesfully process map with size %s", uriSet.size.toString))
    LOG.info(String.format("Map file saved at %s; Integer List File saved at %s", mapFile.getName, integerListFile.getName))

    uriSet.size
  }

  private def getWeight(src: DBpediaResource, target: DBpediaResource): Double = {
    1.0
  }
}
