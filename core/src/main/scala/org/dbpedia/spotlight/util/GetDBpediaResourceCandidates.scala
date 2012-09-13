package org.dbpedia.spotlight.util

import java.io.PrintWriter
import io.Source
import org.dbpedia.spotlight.model.DBpediaResource._
import org.apache.commons.logging.LogFactory
import scala.collection.JavaConversions._
import org.dbpedia.spotlight.model._

/**
 * Takes in a list of surface forms and queries the index to find classes that are confusable with those.
 *
 * @author pablomendes
 */
object GetDBpediaResourceCandidates {

    private val LOG = LogFactory.getLog(this.getClass)

    /**
     * This class obtains DBpediaResources that are candidates for a given surface form
     *
     */
    def main(args: Array[String]) {

        val spotlightConfigFileName = args(0)
        val surfaceFormSetFile = args(1)
        val uriSetFile = surfaceFormSetFile+".uris"

        val configuration = new SpotlightConfiguration(spotlightConfigFileName);

        val factory = new SpotlightFactory(configuration)
        val searcher = factory.contextSearcher

        val out = new PrintWriter(uriSetFile);

        var i = 0;
        Source.fromFile(surfaceFormSetFile).getLines.foreach( name => {
            i = i + 1
            LOG.info(String.format("Surface Form %s : %s", i.toString, name.toString));
            val sf = Factory.SurfaceForm.fromString(name);
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