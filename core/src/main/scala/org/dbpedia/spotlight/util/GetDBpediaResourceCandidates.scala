package org.dbpedia.spotlight.util

import java.io.PrintWriter
import io.Source
import org.dbpedia.spotlight.model.DBpediaResource._
import org.apache.commons.logging.LogFactory
import org.dbpedia.spotlight.model.{SurfaceForm, DBpediaResource, SpotlightFactory, SpotlightConfiguration}
import scala.collection.JavaConversions._
//import org.dbpedia.spotlight.candidate.NonCommonWordSelector
//import org.dbpedia.spotlight.io.WortschatzParser

/**
 * Takes in a list of Common Words and queries the index to find classes that are confusable with those.
 * After this, you can run Generate
 * @author pablomendes
 */
object GetDBpediaResourceCandidates {

    private val LOG = LogFactory.getLog(this.getClass)

    /**
     * This class obtains DBpediaResources that are candidates for a given surface form
     *
     */
    def main(args: Array[String]) {
        val indexingConfigFileName = args(0)
        val spotlightConfigFileName = args(1)
        val surfaceFormSetFile = args(2)
        val uriSetFile = surfaceFormSetFile+".uris"

        val configuration = new SpotlightConfiguration(spotlightConfigFileName); //TODO move this to command line parameter

        val factory = new SpotlightFactory(configuration)
        val searcher = factory.searcher

        val out = new PrintWriter(uriSetFile);

        //val surfaceForms = WortschatzParser.parse(surfaceFormSetFile, 50, 100);
        val surfaceForms = Source.fromFile(surfaceFormSetFile).getLines;
        var i = 0;
        surfaceForms.foreach( name => {
            i = i + 1
            LOG.info(String.format("Surface Form %s : %s", i.toString, name));
            val sf = new SurfaceForm(name)
            val uriList = searcher.getCandidates(sf).toList.map( r => r.uri).mkString("\n")
            if (uriList.size>0)
                out.print(uriList+"\n");
            if (i % 100 == 0) {
                out.flush
            }
        });

        LOG.info(String.format("Results saved to %s ", uriSetFile))

        out.close
    }

}